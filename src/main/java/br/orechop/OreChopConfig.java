package br.orechop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Configuracoes lidas no inicio de cada sessao de jogo/servidor. */
public final class OreChopConfig {
    public boolean enabled = true;
    public boolean allowModdedOres = true;
    public boolean sneakToMineNormally = true;
    public boolean showActionBarProgress = true;
    public int maxVeinBlocks = 64;
    public int maxAxisDistance = 16;
    public int sessionTimeoutSeconds = 45;

    public static OreChopConfig load(Logger logger) {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("orechop.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        OreChopConfig config = new OreChopConfig();
        try {
            Files.createDirectories(file.getParent());
            if (Files.exists(file)) {
                OreChopConfig loaded = gson.fromJson(Files.readString(file, StandardCharsets.UTF_8), OreChopConfig.class);
                if (loaded != null) config = loaded;
            }
            config.maxVeinBlocks = Math.max(2, Math.min(config.maxVeinBlocks, 256));
            config.maxAxisDistance = Math.max(2, Math.min(config.maxAxisDistance, 32));
            config.sessionTimeoutSeconds = Math.max(5, Math.min(config.sessionTimeoutSeconds, 600));
            Files.writeString(file, gson.toJson(config), StandardCharsets.UTF_8);
        } catch (IOException | RuntimeException e) {
            logger.warn("Nao foi possivel ler/gravar config/orechop.json; usando config em memoria", e);
        }
        return config;
    }
}
