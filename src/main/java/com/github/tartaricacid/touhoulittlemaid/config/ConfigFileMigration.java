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
import org.jetbrains.annotations.Nullable;

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
    public static final String AI_FILE_NAME = TouhouLittleMaid.MOD_ID + "-ai.toml";
    public static final String AI_SERVER_FILE_NAME = TouhouLittleMaid.MOD_ID + "-ai-server.toml";
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

    /**
     * 2026-07-28 拆分：个人 AI 配置（ai 节五项）从 -global.toml 迁往专属的 -ai.toml。
     *
     * <p><b>必须在两个 spec 注册之前调用</b>：注册加载那一刻 -global.toml 会被 correct 剥掉
     * 已不在 spec 里的 ai 节，旧值就没了。逐键在候选链（global → 更老的 client → common）里
     * 取第一处命中，而不是整文件二选一——「global 是新建的、AI 值还留在更老文件里」的升级路径
     * 会栽在整文件选择上。</p>
     */
    public static void migrateAiFileIfNeeded(List<ModConfigSpec.ConfigValue<?>> aiValues,
                                             ModConfigSpec aiSpec) {
        migrateAiFileIfNeeded(FabricLoader.getInstance().getConfigDir(), aiValues, aiSpec);
    }

    static void migrateAiFileIfNeeded(Path configDir,
                                      List<ModConfigSpec.ConfigValue<?>> aiValues,
                                      ModConfigSpec aiSpec) {
        Path aiFile = configDir.resolve(AI_FILE_NAME);
        if (Files.exists(aiFile)) {
            return;
        }
        try {
            CommentedConfig target = emptyConfig();
            aiSpec.correct(target);

            List<CommentedConfig> sources = new ArrayList<>();
            for (Path candidate : new Path[]{
                    configDir.resolve(GLOBAL_FILE_NAME),
                    configDir.resolve(PROVISIONAL_CLIENT_FILE_NAME),
                    configDir.resolve(LEGACY_FILE_NAME)}) {
                if (Files.isRegularFile(candidate)) {
                    sources.add(read(candidate));
                }
            }
            int migrated = 0;
            for (ModConfigSpec.ConfigValue<?> value : aiValues) {
                List<String> path = value.getPath();
                for (CommentedConfig source : sources) {
                    if (source.contains(path)) {
                        target.set(path, copyValue(source.getRaw(path)));
                        migrated++;
                        break;
                    }
                }
            }
            writeAtomically(target, aiFile);
            LOGGER.info("Created AI player config {} ({} values migrated)", aiFile, migrated);
        } catch (RuntimeException | IOException exception) {
            LOGGER.error("Failed to create AI player config {}", aiFile, exception);
        }
    }

    /**
     * §17 v2 拆分：AI 规则（11 条）从每存档的世界规则文件迁往**实例级**的 -ai-server.toml。
     *
     * <p>逐键取第一处命中，源链 = <b>本次启动的存档</b>的世界文件 → 实例模板 -server.toml →
     * 更老的 client/common。多存档时以升级后第一个进入的存档为准（AI 规则太新，
     * 不同存档配不同值的情形近零）；其余存档里的旧 AI 键随各自下次被写盘时剥除。
     * 一次性：实例文件已存在就绝不再动。</p>
     */
    public static void migrateAiServerFileIfNeeded(Path configDir, @Nullable Path worldServerFile,
                                                   List<ModConfigSpec.ConfigValue<?>> aiValues,
                                                   ModConfigSpec aiSpec) {
        Path aiFile = configDir.resolve(AI_SERVER_FILE_NAME);
        if (Files.exists(aiFile)) {
            return;
        }
        try {
            CommentedConfig target = emptyConfig();
            aiSpec.correct(target);

            List<CommentedConfig> sources = new ArrayList<>();
            List<Path> candidates = new ArrayList<>();
            if (worldServerFile != null) {
                candidates.add(worldServerFile);
            }
            candidates.add(configDir.resolve(SERVER_FILE_NAME));
            candidates.add(configDir.resolve(PROVISIONAL_CLIENT_FILE_NAME));
            candidates.add(configDir.resolve(LEGACY_FILE_NAME));
            for (Path candidate : candidates) {
                if (Files.isRegularFile(candidate)) {
                    sources.add(read(candidate));
                }
            }
            int migrated = 0;
            for (ModConfigSpec.ConfigValue<?> value : aiValues) {
                List<String> path = value.getPath();
                for (CommentedConfig source : sources) {
                    if (source.contains(path)) {
                        target.set(path, copyValue(source.getRaw(path)));
                        migrated++;
                        break;
                    }
                }
            }
            writeAtomically(target, aiFile);
            LOGGER.info("Created AI rule config {} ({} values migrated)", aiFile, migrated);
        } catch (RuntimeException | IOException exception) {
            LOGGER.error("Failed to create AI rule config {}", aiFile, exception);
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
