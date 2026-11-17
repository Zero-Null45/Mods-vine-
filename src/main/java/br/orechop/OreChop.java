package br.orechop;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * OreChop: minerar um veio por etapas. O jogador minera N blocos normalmente e,
 * ao alcancar o limiar, os blocos restantes sao quebrados via rotina vanilla.
 *
 * Nao altera o HT's TreeChop, nao copia suas classes e nao substitui VeinMiner.
 */
public final class OreChop implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("orechop");
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, PendingBreak> PENDING = new HashMap<>();
    private static final ThreadLocal<Boolean> BREAKING_BATCH = ThreadLocal.withInitial(() -> false);
    private static OreChopConfig config;

    @Override
    public void onInitialize() {
        config = OreChopConfig.load(LOGGER);
        PlayerBlockBreakEvents.BEFORE.register(OreChop::beforeBreak);
        PlayerBlockBreakEvents.AFTER.register(OreChop::afterBreak);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            UUID uuid = handler.getPlayer().getUuid();
            SESSIONS.remove(uuid);
            PENDING.remove(uuid);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            SESSIONS.clear();
            PENDING.clear();
        });
        LOGGER.info("OreChop 0.1.0 iniciado - Minecraft 1.20.1, Java 17+");
    }

    private static boolean beforeBreak(World world, PlayerEntity player, BlockPos pos,
                                       BlockState state, BlockEntity blockEntity) {
        // Permite callbacks vanilla/de protecao ao executar a etapa automatica.
        if (BREAKING_BATCH.get() || !(world instanceof ServerWorld serverWorld)
                || !(player instanceof ServerPlayerEntity serverPlayer)) return true;
        UUID uuid = player.getUuid();
        PENDING.remove(uuid);
        if (!config.enabled || player.isCreative() || player.isSpectator()) return true;
        if (config.sneakToMineNormally && player.isSneaking()) return true;
        if (!validTool(player, state)) return true;

        String family = OreVeins.family(state, config.allowModdedOres);
        if (family == null) return true;

        Session session = SESSIONS.get(uuid);
        long tick = world.getTime();
        if (session == null || !session.worldKey.equals(world.getRegistryKey())
                || !session.family.equals(family) || !session.positions.contains(pos)
                || tick - session.lastBreakTick > config.sessionTimeoutSeconds * 20L) {
            Set<BlockPos> vein = OreVeins.find(serverWorld, pos, family, config);
            if (vein.size() < 2) {
                SESSIONS.remove(uuid);
                return true; // Um bloco isolado usa mecanica vanilla.
            }
            session = new Session(world.getRegistryKey(), family, vein, tick);
            SESSIONS.put(uuid, session);
        }
        PENDING.put(uuid, new PendingBreak(pos.toImmutable(), state.getBlock(), session));
        return true; // Nunca cancela a mineracao vanilla.
    }

    private static void afterBreak(World world, PlayerEntity player, BlockPos pos,
                                   BlockState state, BlockEntity blockEntity) {
        if (BREAKING_BATCH.get() || !(world instanceof ServerWorld serverWorld)
                || !(player instanceof ServerPlayerEntity serverPlayer)) return;

        UUID uuid = player.getUuid();
        PendingBreak pending = PENDING.remove(uuid);
        if (pending == null || !pending.pos.equals(pos) || pending.block != state.getBlock()) return;
        Session session = pending.session;
        if (SESSIONS.get(uuid) != session) return;
        session.completedBreaks++;
        session.lastBreakTick = world.getTime();
        int required = OreVeins.requiredBreaks(session.positions.size());

        if (session.completedBreaks < required) {
            if (config.showActionBarProgress) {
                serverPlayer.sendMessage(Text.literal("OreChop | Veio: " + session.completedBreaks
                        + "/" + required + " blocos para liberar o restante"), true);
            }
            return;
        }

        SESSIONS.remove(uuid);
        int brokenAutomatically = breakRemaining(serverWorld, serverPlayer, session);
        if (config.showActionBarProgress) {
            serverPlayer.sendMessage(Text.literal("OreChop | Veio concluido: +"
                    + brokenAutomatically + " blocos minerados"), true);
        }
    }

    private static int breakRemaining(ServerWorld world, ServerPlayerEntity player, Session session) {
        int count = 0;
        BREAKING_BATCH.set(true);
        try {
            for (BlockPos pos : session.positions) {
                if (!OreVeins.loaded(world, pos)) continue;
                BlockState state = world.getBlockState(pos);
                if (!session.family.equals(OreVeins.family(state, config.allowModdedOres))) continue;
                if (!validTool(player, state)) break; // Picareta quebrou, ou ferramenta foi trocada.
                // tryBreakBlock reproduz drops, Fortune, Silk Touch, XP, desgaste e eventos
                // normais de quebra em vez de simplesmente substituir por ar.
                if (player.interactionManager.tryBreakBlock(pos)) {
                    count++;
                }
            }
        } catch (RuntimeException e) {
            LOGGER.error("Falha ao minerar o restante do veio", e);
        } finally {
            BREAKING_BATCH.remove();
        }
        return count;
    }

    private static boolean validTool(PlayerEntity player, BlockState state) {
        return player.getMainHandStack().isSuitableFor(state) && player.canHarvest(state);
    }

    private static final class Session {
        final RegistryKey<World> worldKey;
        final String family;
        final Set<BlockPos> positions;
        long lastBreakTick;
        int completedBreaks;

        Session(RegistryKey<World> worldKey, String family, Set<BlockPos> positions, long tick) {
            this.worldKey = worldKey;
            this.family = family;
            this.positions = positions;
            this.lastBreakTick = tick;
        }
    }

    private record PendingBreak(BlockPos pos, net.minecraft.block.Block block, Session session) { }
}
