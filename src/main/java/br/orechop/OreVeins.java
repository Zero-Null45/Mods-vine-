package br.orechop;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Set;

/** Identificacao de minerios e busca em largura por blocos que dividem uma face. */
final class OreVeins {
    private OreVeins() { }

    static String family(BlockState state, boolean allowModded) {
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        String namespace = id.getNamespace();
        String path = id.getPath();
        if (!namespace.equals("minecraft") && !allowModded) return null;
        // Ancient debris nao tem sufixo _ore.
        if (namespace.equals("minecraft") && path.equals("ancient_debris")) return "minecraft:ancient_debris";
        // Camadas de profundidade sao o mesmo tipo de minerio.
        if (path.startsWith("deepslate_")) path = path.substring("deepslate_".length());
        if (!path.endsWith("_ore")) return null;
        return namespace + ":" + path;
    }

    static Set<BlockPos> find(ServerWorld world, BlockPos origin, String family, OreChopConfig config) {
        Set<BlockPos> found = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        if (!loaded(world, origin)) return found;

        found.add(origin.toImmutable());
        queue.add(origin.toImmutable());
        while (!queue.isEmpty() && found.size() < config.maxVeinBlocks) {
            BlockPos current = queue.removeFirst();
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.offset(direction);
                if (found.contains(neighbor)) continue;
                if (Math.abs(neighbor.getX() - origin.getX()) > config.maxAxisDistance
                        || Math.abs(neighbor.getY() - origin.getY()) > config.maxAxisDistance
                        || Math.abs(neighbor.getZ() - origin.getZ()) > config.maxAxisDistance) continue;
                if (!loaded(world, neighbor)) continue;
                if (!family.equals(family(world.getBlockState(neighbor), config.allowModdedOres))) continue;
                found.add(neighbor.toImmutable());
                queue.addLast(neighbor.toImmutable());
                if (found.size() >= config.maxVeinBlocks) break;
            }
        }
        return found;
    }

    static boolean loaded(ServerWorld world, BlockPos pos) {
        return world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4);
    }

    /** Escala moderada: veios maiores precisam de mais blocos minerados manualmente. */
    static int requiredBreaks(int veinSize) {
        return Math.min(veinSize, (int) Math.ceil(Math.sqrt(veinSize)));
    }
}
