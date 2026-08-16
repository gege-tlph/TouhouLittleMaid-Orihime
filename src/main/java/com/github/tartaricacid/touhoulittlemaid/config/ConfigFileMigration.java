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
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 配置文件的建立与迁移，四个目标各一条。源头都是代码宿主 {@code origin/26.1} 的那一个
 * {@code config/touhou_little_maid-common.toml}——它把世界规则、AI 两半与玩家个人偏好混在一起：
 *
 * <ul>
 *   <li>**存档级**世界规则 {@code <world>/serverconfig/touhou_little_maid-server.toml}
 *       —— {@link #migrateServerFileIfNeeded} / {@link #prepareWorldFile}</li>
 *   <li>**实例级** AI 规则 {@code config/touhou_little_maid-ai-server.toml}
 *       —— {@link #migrateAiServerFileIfNeeded}</li>
 *   <li>**个人** AI 配置 {@code config/touhou_little_maid-ai.toml}
 *       —— {@link #migrateAiFileIfNeeded}</li>
 *   <li>**个人**偏好 {@code config/touhou_little_maid-global.toml}
 *       —— {@link #migrateGlobalFileIfNeeded}</li>
 * </ul>
 *
 * <p>四条都是<b>一次性</b>的：目标文件已存在就绝不再动。四条也都<b>必须在对应 spec 注册之前</b>
 * 调用——注册加载那一刻 {@code correct()} 会把「已不在 spec 里」的键整批剥掉，旧值当场消失。</p>
 */
public final class ConfigFileMigration {
    private static final Logger LOGGER = LogManager.getLogger(ConfigFileMigration.class);

    /** 世界规则文件名。与 Forge Config API Port 给 {@code Type.SERVER} 用的名字一致，见 {@link #migrateServerFileIfNeeded}。 */
    public static final String SERVER_FILE_NAME = TouhouLittleMaid.MOD_ID + "-server.toml";
    /** 实例级 AI 规则文件名，由 {@link AiServerRuleConfig} 独占读写。 */
    public static final String AI_SERVER_FILE_NAME = TouhouLittleMaid.MOD_ID + "-ai-server.toml";
    /** 个人 AI 配置文件名，由 Forge Config API Port 正常管理（{@link AiClientConfig}）。 */
    public static final String AI_FILE_NAME = TouhouLittleMaid.MOD_ID + "-ai.toml";
    /** 玩家个人偏好文件名，由 Forge Config API Port 正常管理（{@link GeneralConfig}），**只在客户端建立**。 */
    public static final String GLOBAL_FILE_NAME = TouhouLittleMaid.MOD_ID + "-global.toml";
    /** 这些规则值在代码宿主 origin/26.1 上原属 COMMON spec，即实例级的这个文件。 */
    private static final String LEGACY_FILE_NAME = TouhouLittleMaid.MOD_ID + "-common.toml";

    private static final java.util.List<String> REPLACE_SLIME_MODEL_PATH = java.util.List.of("vanilla", "ReplaceSlimeModel");
    private static final java.util.List<String> REPLACE_MAGMA_CUBE_MODEL_PATH = java.util.List.of("vanilla", "ReplaceMagmaCubeModel");

    private ConfigFileMigration() {
    }

    /**
     * 把规则值的旧值从实例级 common 文件搬进实例级 server 文件，**必须在 COMMON spec 注册之前调用**。
     *
     * <p>本方法在行为基准 {@code port/1.21.11-fabric} 上不存在，是本分支的宿主结构逼出来的：
     * 代码宿主 {@code origin/26.1} 把这 43 项规则值放在 COMMON spec 里，也就是玩家现有的
     * {@code config/touhou_little_maid-common.toml}。我们把它们移进 SERVER spec 之后，
     * 注册 COMMON spec 的那一刻 {@code correct()} 会把「已不在 spec 里」的这些键**整批剥掉**，
     * 旧值当场消失——等服务器启动、{@link #prepareWorldFile} 想去继承时，源头已经空了。
     * 所以先落一份实例级快照，让它充当迁移源。一次性：目标已存在就绝不再动。</p>
     *
     * <p>（1.21.11 上的同型事故记在 {@code migrateAiFileIfNeeded} 的注释里：
     * 「注册加载那一刻会被 correct 剥掉已不在 spec 里的节，旧值就没了」。）</p>
     */
    public static void migrateServerFileIfNeeded(List<ModConfigSpec.ConfigValue<?>> serverValues,
                                                 ModConfigSpec serverSpec) {
        migrateServerFileIfNeeded(FabricLoader.getInstance().getConfigDir(), serverValues, serverSpec);
    }

    static void migrateServerFileIfNeeded(Path configDir,
                                          List<ModConfigSpec.ConfigValue<?>> serverValues,
                                          ModConfigSpec serverSpec) {
        Path serverFile = configDir.resolve(SERVER_FILE_NAME);
        if (Files.exists(serverFile)) {
            return;
        }
        Path legacy = configDir.resolve(LEGACY_FILE_NAME);
        if (!Files.isRegularFile(legacy)) {
            // 全新安装：没有旧值要救，世界文件按默认值现建即可，不必留下一个纯默认的实例级文件。
            return;
        }
        try {
            CommentedConfig target = emptyConfig();
            serverSpec.correct(target);
            int migrated = copyKnownValues(read(legacy), target, serverValues);
            writeAtomically(target, serverFile);
            LOGGER.info("Migrated {} server rule values from {} to {}", migrated, legacy, serverFile);
        } catch (RuntimeException | IOException exception) {
            LOGGER.error("Failed to snapshot server rule values from {}", legacy, exception);
        }
    }

    /**
     * 实例级 AI 规则从 {@code -common.toml} 的 {@code [ai]} 节迁往专属的 {@code -ai-server.toml}。
     *
     * <p><b>必须在 COMMON spec 注册之前调用</b>，与 {@link #migrateServerFileIfNeeded} 同一个陷阱。</p>
     *
     * <p>源链逐键取第一处命中，而不是整文件二选一——「某个源是被上一轮迁移新建的、AI 值还留在
     * 更老的文件里」这条升级路径会栽在整文件选择上。链序：<b>本次启动的存档</b>的世界文件 →
     * 实例模板 {@code -server.toml} → {@code -common.toml}。多存档时以升级后第一个进入的存档为准
     * （AI 规则本就是实例级的，不同存档配不同值的情形近零）。</p>
     */
    public static void migrateAiServerFileIfNeeded(Path configDir, @Nullable Path worldServerFile,
                                                   List<ModConfigSpec.ConfigValue<?>> aiValues,
                                                   ModConfigSpec aiSpec) {
        List<Path> candidates = new ArrayList<>();
        if (worldServerFile != null) {
            candidates.add(worldServerFile);
        }
        candidates.add(configDir.resolve(SERVER_FILE_NAME));
        candidates.add(configDir.resolve(LEGACY_FILE_NAME));
        seedFromFirstHit(configDir.resolve(AI_SERVER_FILE_NAME), candidates, aiValues, aiSpec, "AI rule config");
    }

    /**
     * 个人 AI 配置从 {@code -common.toml} 的 {@code [ai]} 节迁往专属的 {@code -ai.toml}。
     *
     * <p><b>必须在两个 spec 注册之前调用</b>：注册加载那一刻 {@code -common.toml} 会被
     * {@code correct()} 剥掉已不在其 spec 里的 {@code [ai]} 节，旧值就没了。</p>
     *
     * <p>⚠️ 行为基准 {@code port/1.21.11-fabric} 的源链是 {@code -global.toml → -client.toml →
     * -common.toml} 三级；<b>本分支没有前两层</b>（无 global 层，个人配置一律在 common），
     * 故源链只有 {@code -common.toml} 一处。逐键取第一处命中的形状保留，将来加源不必改结构。</p>
     */
    public static void migrateAiFileIfNeeded(List<ModConfigSpec.ConfigValue<?>> aiValues,
                                             ModConfigSpec aiSpec) {
        migrateAiFileIfNeeded(FabricLoader.getInstance().getConfigDir(), aiValues, aiSpec);
    }

    static void migrateAiFileIfNeeded(Path configDir,
                                      List<ModConfigSpec.ConfigValue<?>> aiValues,
                                      ModConfigSpec aiSpec) {
        seedFromFirstHit(configDir.resolve(AI_FILE_NAME), List.of(configDir.resolve(LEGACY_FILE_NAME)),
                aiValues, aiSpec, "AI player config");
    }

    /**
     * 玩家个人偏好从 {@code -common.toml} 迁往专属的 {@code -global.toml}。
     *
     * <p><b>必须在 COMMON spec 注册之前调用</b>，与其余三条同一个陷阱：注册加载那一刻
     * {@code correct()} 会把「已不在 COMMON spec 里」的 {@code [misc]}/{@code [vanilla]}/
     * {@code [render]} 三节与 {@code [maid]} 的两个键**整批剥掉**，玩家调过的值当场消失。</p>
     *
     * <p>⚠️ 行为基准 {@code port/1.21.11-fabric} 的源链是 {@code -client.toml → -common.toml}
     * 两级——那边宿主曾以 {@code Type.CLIENT} 的默认名建过 {@code -client.toml}。
     * <b>本分支没有那一层</b>（代码宿主 {@code origin/26.1} 一直用 {@code Type.COMMON}），
     * 故源链只有 {@code -common.toml} 一处。<b>别照抄成两级</b>：一个从不存在的源会让
     * 「逐键取第一处命中」多一次无谓的读盘，也会让下一个人以为本树有过 client 层。</p>
     */
    public static void migrateGlobalFileIfNeeded(List<ModConfigSpec.ConfigValue<?>> globalValues,
                                                 ModConfigSpec globalSpec) {
        migrateGlobalFileIfNeeded(FabricLoader.getInstance().getConfigDir(), globalValues, globalSpec);
    }

    static void migrateGlobalFileIfNeeded(Path configDir,
                                          List<ModConfigSpec.ConfigValue<?>> globalValues,
                                          ModConfigSpec globalSpec) {
        seedFromFirstHit(configDir.resolve(GLOBAL_FILE_NAME), List.of(configDir.resolve(LEGACY_FILE_NAME)),
                globalValues, globalSpec, "player config");
    }

    /**
     * 专服上把「从旧版本升上来、如今无人读」的两份客户端配置文件点名说清楚。
     *
     * <p>不删——那是管理员的文件。但也不能让它们默不作声地躺着：
     * {@code -global.toml} 里全是渲染与界面偏好，{@code -ai.toml} 里是麦克风与语音识别，
     * 服主看见它们只会得到一个错误结论「这些能在服务端配」。<b>载体的存在会被读成语义。</b>
     * 与 {@code AvailableSites} 对残留 {@code stt.json} 的处理同款。</p>
     */
    public static void logUnusedClientFilesOnServer() {
        logUnusedClientFilesOnServer(FabricLoader.getInstance().getConfigDir());
    }

    static void logUnusedClientFilesOnServer(Path configDir) {
        for (String name : List.of(GLOBAL_FILE_NAME, AI_FILE_NAME)) {
            Path file = configDir.resolve(name);
            if (Files.isRegularFile(file)) {
                LOGGER.info("{} is not used on a dedicated server and can be deleted: "
                        + "it holds each player's own client-side preferences", file);
            }
        }
    }

    /**
     * 一次性播种：目标不存在时按 spec 建默认值，再逐键从源链第一处命中处取旧值覆盖。
     * 源链里读不出来的文件跳过而不是整批放弃——一个坏文件不该让其余的旧值全丢。
     */
    private static void seedFromFirstHit(Path target, List<Path> candidates,
                                         List<ModConfigSpec.ConfigValue<?>> values,
                                         ModConfigSpec spec, String label) {
        if (Files.exists(target)) {
            return;
        }
        try {
            CommentedConfig seeded = emptyConfig();
            spec.correct(seeded);

            List<CommentedConfig> sources = new ArrayList<>();
            for (Path candidate : candidates) {
                if (!Files.isRegularFile(candidate)) {
                    continue;
                }
                try {
                    sources.add(read(candidate));
                } catch (RuntimeException | IOException exception) {
                    LOGGER.error("Skipped unreadable migration source {}", candidate, exception);
                }
            }

            int migrated = 0;
            for (ModConfigSpec.ConfigValue<?> value : values) {
                List<String> path = value.getPath();
                for (CommentedConfig source : sources) {
                    if (source.contains(path)) {
                        seeded.set(path, copyValue(source.getRaw(path)));
                        migrated++;
                        break;
                    }
                }
            }
            writeAtomically(seeded, target);
            LOGGER.info("Created {} {} ({} values migrated)", label, target, migrated);
        } catch (RuntimeException | IOException exception) {
            LOGGER.error("Failed to create {} {}", label, target, exception);
        }
    }

    public static void inheritMagmaCubeFromSlime() {
        inheritMagmaCubeFromSlime(FabricLoader.getInstance().getConfigDir());
    }

    /**
     * 在 COMMON spec 加载既有文件之前跑：早于「岩浆怪独立开关」拆分的旧文件，
     * 让玩家的 ReplaceSlimeModel 选择顺延到岩浆怪，而不是被 spec 默认值静默盖掉。
     * 已带新键的文件、第三方未知键与注释一律不动。
     *
     * <p>⚠️ <b>本方法作用于迁移源 {@code -common.toml}，不是目标 {@code -global.toml}</b>，
     * 因此它<b>必须排在 {@link #migrateGlobalFileIfNeeded} 之前</b>：先把旧值补进源文件，
     * 再由 global 迁移逐键搬走。次序反了就没救了——global 迁移会先按 spec 铺一份默认值
     * （{@code ReplaceMagmaCubeModel=true}），源里没有这个键便不覆盖，随后本方法看到目标里
     * <b>已经有</b>该键就直接返回，玩家的旧选择被默认值静默吃掉。
     * {@code ConfigBootstrapOrderContractTest} 钉着这条次序。</p>
     *
     * <p>行为基准 {@code port/1.21.11-fabric} 的同名方法作用于目标 global 文件，另在
     * {@code migrateGlobalFileIfNeeded} 里对源文件重做一次同样的判断。本分支把两处合成一处：
     * {@code -global.toml} 是本次（2026-08-17）才引入的，不可能存在「早于拆分的旧 global 文件」，
     * 那一半没有适用对象（{@code MagmaCubeConfigInheritanceTest} 钉着源侧语义）。</p>
     */
    static void inheritMagmaCubeFromSlime(Path configDir) {
        Path common = configDir.resolve(LEGACY_FILE_NAME);
        if (!Files.isRegularFile(common)) {
            return;
        }
        try {
            CommentedConfig config = read(common);
            if (config.contains(REPLACE_MAGMA_CUBE_MODEL_PATH)) {
                return;
            }
            if (!(config.getRaw(REPLACE_SLIME_MODEL_PATH) instanceof Boolean slimeValue)) {
                return;
            }
            config.set(REPLACE_MAGMA_CUBE_MODEL_PATH, slimeValue);
            writeAtomically(config, common);
            LOGGER.info("Inherited ReplaceMagmaCubeModel={} from legacy ReplaceSlimeModel in {}",
                    slimeValue, common);
        } catch (RuntimeException | IOException exception) {
            LOGGER.error("Failed to migrate ReplaceMagmaCubeModel in {}", common, exception);
        }
    }

    /**
     * 建立/补全当前存档的权威世界规则文件。已有存档一次性继承实例级旧值；新建存档从默认值开始。
     *
     * <p>与 1.21.11 的同名方法有一处**必要的差异**：那边这个文件只可能是我们自己建的，
     * 存在即完整，可以直接返回；本分支上它可能是 Forge Config API Port 以
     * {@code ModConfig.Type.SERVER} 建的——里面只有原 SERVER spec 那四项，其余 39 项规则值缺席。
     * 照抄「存在即返回」会让那 39 项静默落回默认值，玩家在 common 文件里调过的值全部丢失。
     * 故改为：文件已存在时仍逐键补齐缺失项，补到了才回写。</p>
     */
    public static Path prepareWorldFile(MinecraftServer server,
                                        List<ModConfigSpec.ConfigValue<?>> serverValues,
                                        ModConfigSpec serverSpec) throws IOException {
        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path serverConfigDir = worldRoot.resolve("serverconfig");
        Path worldConfig = serverConfigDir.resolve(SERVER_FILE_NAME);
        Files.createDirectories(serverConfigDir);

        if (Files.isRegularFile(worldConfig)) {
            CommentedConfig existing = read(worldConfig);
            int inherited = inheritMissingValues(existing, serverValues);
            if (inherited > 0) {
                writeAtomically(existing, worldConfig);
                LOGGER.info("Completed {} missing server rule values in {}", inherited, worldConfig);
            }
            return worldConfig;
        }

        CommentedConfig target = emptyConfig();
        serverSpec.correct(target);

        Path source = null;
        int migrated = 0;
        if (server.getWorldData().overworldData().isInitialized()) {
            source = firstRegularFile(
                    FabricLoader.getInstance().getConfigDir().resolve(SERVER_FILE_NAME),
                    FabricLoader.getInstance().getConfigDir().resolve(LEGACY_FILE_NAME));
            if (source != null) {
                migrated = copyKnownValues(read(source), target, serverValues);
            }
        }

        writeAtomically(target, worldConfig);
        if (source == null) {
            LOGGER.info("Created default world config {}", worldConfig);
        } else {
            LOGGER.info("Migrated {} server config values from {} to {}", migrated, source, worldConfig);
        }
        return worldConfig;
    }

    /** 从实例级迁移源里补齐这份世界文件尚未携带的规则键；源不存在或键也缺就留空，由调用方补默认值。 */
    private static int inheritMissingValues(CommentedConfig target,
                                            List<ModConfigSpec.ConfigValue<?>> serverValues) {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path source = firstRegularFile(configDir.resolve(SERVER_FILE_NAME), configDir.resolve(LEGACY_FILE_NAME));
        if (source == null) {
            return 0;
        }
        final CommentedConfig sourceConfig;
        try {
            sourceConfig = read(source);
        } catch (RuntimeException | IOException exception) {
            LOGGER.error("Failed to read server rule migration source {}", source, exception);
            return 0;
        }
        int inherited = 0;
        for (ModConfigSpec.ConfigValue<?> value : serverValues) {
            List<String> path = value.getPath();
            if (!target.contains(path) && sourceConfig.contains(path)) {
                target.set(path, copyValue(sourceConfig.getRaw(path)));
                sourceConfig.getOptionalComment(path).ifPresent(comment -> target.setComment(path, comment));
                inherited++;
            }
        }
        return inherited;
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

    private static @Nullable Path firstRegularFile(Path... candidates) {
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
        return new TomlParser().parse(new StringReader(""));
    }

    static void writeAtomically(CommentedConfig config, Path target) throws IOException {
        StringWriter output = new StringWriter();
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
