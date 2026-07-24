package com.github.tartaricacid.touhoulittlemaid.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** One-time compatibility bridge for the old shared common config file. */
public final class ConfigFileMigration {
    private static final Logger LOGGER = LogManager.getLogger(ConfigFileMigration.class);
    public static final String GLOBAL_FILE_NAME = TouhouLittleMaid.MOD_ID + "-global.toml";
    public static final String SERVER_FILE_NAME = TouhouLittleMaid.MOD_ID + "-server.toml";
    private static final String PROVISIONAL_CLIENT_FILE_NAME = TouhouLittleMaid.MOD_ID + "-client.toml";
    private static final String LEGACY_FILE_NAME = TouhouLittleMaid.MOD_ID + "-common.toml";

    // 岩浆怪替换开关是本项目对上游共享 ReplaceSlimeModel 的拆分；旧文件缺新键时必须继承旧史莱姆值，
    // 而不是被 spec 默认 true 补掉。
    private static final List<String> REPLACE_SLIME_MODEL_PATH = List.of("vanilla", "ReplaceSlimeModel");
    private static final List<String> REPLACE_MAGMA_CUBE_MODEL_PATH = List.of("vanilla", "ReplaceMagmaCubeModel");

    private ConfigFileMigration() {
    }

    public static void migrateGlobalFileIfNeeded(List<ModConfigSpec.ConfigValue<?>> globalValues,
                                                 ModConfigSpec globalSpec) {
        migrateGlobalFileIfNeeded(FabricLoader.getInstance().getConfigDir(), globalValues, globalSpec);
    }

    static void migrateGlobalFileIfNeeded(Path configDir,
                                          List<ModConfigSpec.ConfigValue<?>> globalValues,
                                          ModConfigSpec globalSpec) {
        Path global = configDir.resolve(GLOBAL_FILE_NAME);
        if (Files.exists(global)) {
            return;
        }

        Path source = firstRegularFile(
                configDir.resolve(PROVISIONAL_CLIENT_FILE_NAME),
                configDir.resolve(LEGACY_FILE_NAME)
        );
        try {
            CommentedConfig target = emptyConfig();
            globalSpec.correct(target);
            int migrated = 0;
            if (source != null) {
                CommentedConfig sourceConfig = read(source);
                migrated = copyKnownValues(sourceConfig, target, globalValues);
                // 旧 client/common 文件只有共享的 ReplaceSlimeModel；新建 global 时按其旧值初始化岩浆怪开关。
                if (!sourceConfig.contains(REPLACE_MAGMA_CUBE_MODEL_PATH)
                        && sourceConfig.getRaw(REPLACE_SLIME_MODEL_PATH) instanceof Boolean slimeValue) {
                    target.set(REPLACE_MAGMA_CUBE_MODEL_PATH, slimeValue);
                }
            }
            writeAtomically(target, global);
            if (source == null) {
                LOGGER.info("Created global player config {}", global);
            } else {
                LOGGER.info("Migrated {} player config values from {} to {}",
                        migrated, source, global);
            }
        } catch (RuntimeException | IOException exception) {
            LOGGER.error("Failed to create global player config {}", global, exception);
        }
    }

    public static void inheritMagmaCubeFromSlime() {
        inheritMagmaCubeFromSlime(FabricLoader.getInstance().getConfigDir());
    }

    /**
     * Runs before the spec loads the existing global file: an old file that predates the
     * ReplaceMagmaCubeModel split keeps the player's ReplaceSlimeModel choice for magma cubes
     * instead of silently reverting to the spec default. Files that already carry the key,
     * unknown third-party keys and comments are left untouched.
     */
    static void inheritMagmaCubeFromSlime(Path configDir) {
        Path global = configDir.resolve(GLOBAL_FILE_NAME);
        if (!Files.isRegularFile(global)) {
            return;
        }
        try {
            CommentedConfig config = read(global);
            if (config.contains(REPLACE_MAGMA_CUBE_MODEL_PATH)) {
                return;
            }
            if (!(config.getRaw(REPLACE_SLIME_MODEL_PATH) instanceof Boolean slimeValue)) {
                return;
            }
            config.set(REPLACE_MAGMA_CUBE_MODEL_PATH, slimeValue);
            writeAtomically(config, global);
            LOGGER.info("Inherited ReplaceMagmaCubeModel={} from legacy ReplaceSlimeModel in {}",
                    slimeValue, global);
        } catch (RuntimeException | IOException exception) {
            LOGGER.error("Failed to migrate ReplaceMagmaCubeModel in {}", global, exception);
        }
    }

    /**
     * Creates the current world's authoritative config before it is first loaded. Existing
     * worlds inherit the old instance-wide values once; newly created worlds start from defaults.
     */
    public static Path prepareWorldFile(MinecraftServer server,
                                        List<ModConfigSpec.ConfigValue<?>> serverValues,
                                        ModConfigSpec serverSpec) throws IOException {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path serverConfigDir = worldRoot.resolve("serverconfig");
        Path worldConfig = serverConfigDir.resolve(SERVER_FILE_NAME);
        if (Files.isRegularFile(worldConfig)) {
            return worldConfig;
        }

        Files.createDirectories(serverConfigDir);
        CommentedConfig target = emptyConfig();
        serverSpec.correct(target);

        Path source = null;
        int migrated = 0;
        if (server.getWorldData().overworldData().isInitialized()) {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            source = server.isDedicatedServer()
                    ? firstRegularFile(configDir.resolve(SERVER_FILE_NAME),
                    configDir.resolve(LEGACY_FILE_NAME),
                    configDir.resolve(PROVISIONAL_CLIENT_FILE_NAME))
                    : firstRegularFile(configDir.resolve(PROVISIONAL_CLIENT_FILE_NAME),
                    configDir.resolve(LEGACY_FILE_NAME),
                    configDir.resolve(SERVER_FILE_NAME));
            if (source != null) {
                migrated = copyKnownValues(read(source), target, serverValues);
            }
        }

        writeAtomically(target, worldConfig);
        if (source == null) {
            LOGGER.info("Created default world config {}", worldConfig);
        } else {
            LOGGER.info("Migrated {} server config values from {} to {}",
                    migrated, source, worldConfig);
        }
        return worldConfig;
    }

    private static int copyKnownValues(CommentedConfig source, CommentedConfig target,
                                       List<ModConfigSpec.ConfigValue<?>> values) {
        int migrated = 0;
        for (ModConfigSpec.ConfigValue<?> value : values) {
            List<String> path = value.getPath();
            if (source.contains(path)) {
                target.set(path, copyValue(source.getRaw(path)));
                source.getOptionalComment(path).ifPresent(comment -> target.setComment(path, comment));
                migrated++;
            }
        }
        return migrated;
    }

    @SafeVarargs
    private static Path firstRegularFile(Path... candidates) {
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    static CommentedConfig read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return new TomlParser().parse(reader);
        }
    }

    static CommentedConfig emptyConfig() {
        return new TomlParser().parse(new java.io.StringReader(""));
    }

    static void writeAtomically(CommentedConfig config, Path target) throws IOException {
        java.io.StringWriter output = new java.io.StringWriter();
        new TomlWriter().write(config, output);
        byte[] content = output.toString().getBytes(StandardCharsets.UTF_8);
        AtomicConfigFileWriter.write(target, content, ConfigFileMigration::read);
    }

    private static Object copyValue(Object value) {
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>(list.size());
            for (Object element : list) {
                copy.add(copyValue(element));
            }
            return copy;
        }
        return value;
    }
}
