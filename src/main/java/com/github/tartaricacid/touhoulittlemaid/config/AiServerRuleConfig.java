package com.github.tartaricacid.touhoulittlemaid.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 规则的实例级权威存储（{@code config/touhou_little_maid-ai-server.toml}）。
 *
 * <p>§17 v2 定案（用户 2026-07-28）：AI 规则从每存档的世界规则文件整体搬出——站点/技能本就
 * 存在实例级（{@code config/touhou_little_maid/}），规则管的就是它们，作用域应当对齐。
 * 从此 <b>{@code /tlm ai_chat} = 实例级 AI 的一切，{@code /tlm config} = 存档级玩法规则</b>。</p>
 *
 * <p>⚠️ <b>镜像契约</b>：本类是 {@link ServerRuleConfig} 状态机的镜像，凡改动那边的
 * 快照簿记 / 加载恢复 / applyJson 语义，**必须同步检查这里**（反向亦然）。编解码与 IO
 * 共享同一套包内助手，不许各写各的。两店的有意差异只有三条：</p>
 * <ul>
 *   <li>文件作用域：实例级（本类）vs 存档级（世界规则）。</li>
 *   <li><b>保存即激活，专服也一样</b>——与站点 {@code 48fe6db66} 同权逻辑：保存已过权限校验，
 *       漏跑命令玩家侧毫无线索。{@code /tlm ai_chat reload} 只服务手改文件的管理员。</li>
 *   <li>无派生状态刷新（AI 规则没有 regex 缓存这类东西）。</li>
 * </ul>
 * <p>若将来需要第三家店，先抽公共引擎再添，别再复制第三份。</p>
 */
public final class AiServerRuleConfig {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger(AiServerRuleConfig.class);

    public static ModConfigSpec SPEC;
    private static volatile Map<ModConfigSpec.ConfigValue<?>, Object> activeValues = Map.of();
    private static volatile Map<ModConfigSpec.ConfigValue<?>, Object> fileValues = Map.of();
    private static volatile @Nullable Path currentFile;
    private static Set<ModConfigSpec.ConfigValue<?>> owned = Set.of();

    private AiServerRuleConfig() {
    }

    /** 建 spec 并登记归属表；必须先于任何 {@link ServerRuleConfig#get} 路由调用 */
    public static ModConfigSpec init() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        AIConfig.initServer(builder);
        SPEC = builder.build();
        owned = Set.copyOf(values());
        return SPEC;
    }

    public static List<ModConfigSpec.ConfigValue<?>> values() {
        return List.of(
                AIConfig.LLM_ENABLED,
                AIConfig.AUTO_GEN_SETTING_ENABLED,
                AIConfig.LLM_PROXY_ADDRESS,
                AIConfig.MAID_HISTORY_COMPRESS_TOKEN_LIMIT,
                AIConfig.MAX_TOKENS_PER_PLAYER,
                AIConfig.TTS_ENABLED,
                AIConfig.TTS_PROXY_ADDRESS,
                // 默认值必须进公开快照：玩家端要靠它渲染「跟随默认（X）」和底部概要
                AIConfig.DEFAULT_LLM_SITE,
                AIConfig.DEFAULT_LLM_MODEL,
                AIConfig.DEFAULT_TTS_SITE,
                AIConfig.DEFAULT_TTS_MODEL
        );
    }

    private static List<ModConfigSpec.ConfigValue<?>> publicRuntimeValues() {
        List<ModConfigSpec.ConfigValue<?>> result = new ArrayList<>(values());
        result.remove(AIConfig.LLM_PROXY_ADDRESS);
        result.remove(AIConfig.TTS_PROXY_ADDRESS);
        return result;
    }

    /** {@link ServerRuleConfig#get} 的路由判据 */
    public static boolean owns(ModConfigSpec.ConfigValue<?> value) {
        return owned.contains(value);
    }

    /** 保存包按键分拣用：本店认领的键名全集 */
    public static Set<String> jsonKeys() {
        return values().stream().map(ServerRuleConfig::key).collect(Collectors.toUnmodifiableSet());
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        Object active = activeValues.get(value);
        return active == null ? value.get() : (T) active;
    }

    public static String snapshotJson() {
        JsonObject root = new JsonObject();
        Map<ModConfigSpec.ConfigValue<?>, Object> snapshot = fileValues;
        for (ModConfigSpec.ConfigValue<?> value : values()) {
            root.add(ServerRuleConfig.key(value), GSON.toJsonTree(snapshot.getOrDefault(value, value.getDefault())));
        }
        return GSON.toJson(root);
    }

    public static String runtimeSnapshotJson() {
        JsonObject root = new JsonObject();
        for (ModConfigSpec.ConfigValue<?> value : publicRuntimeValues()) {
            root.add(ServerRuleConfig.key(value), GSON.toJsonTree(getUnchecked(value)));
        }
        return GSON.toJson(root);
    }

    /**
     * 事务式应用客户端提交并**立即激活**（专服也不例外，见类注释）。
     * 任何已知字段无法解析或未通过校验时，整批拒绝。
     */
    public static synchronized boolean applyJson(String json) {
        final JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException exception) {
            return false;
        }

        Map<ModConfigSpec.ConfigValue<?>, Object> pending = new LinkedHashMap<>();
        for (ModConfigSpec.ConfigValue<?> value : values()) {
            JsonElement element = root.get(ServerRuleConfig.key(value));
            if (element == null) {
                continue;
            }
            Object decoded = ServerRuleConfig.decode(value, element);
            if (decoded == null || !value.getSpec().test(decoded)) {
                return false;
            }
            pending.put(value, decoded);
        }
        if (pending.isEmpty()) {
            return false;
        }

        Map<ModConfigSpec.ConfigValue<?>, Object> next = new LinkedHashMap<>(fileValues);
        next.putAll(pending);
        try {
            writeValues(next);
            fileValues = Map.copyOf(next);
            activeValues = Map.copyOf(next);
            return true;
        } catch (RuntimeException | IOException exception) {
            return false;
        }
    }

    /** 客户端在收到服务器快照前的占位默认值 */
    public static synchronized void initializeDefaults() {
        Map<ModConfigSpec.ConfigValue<?>, Object> defaults = new LinkedHashMap<>();
        for (ModConfigSpec.ConfigValue<?> value : values()) {
            defaults.put(value, ServerRuleConfig.copyValue(value.getDefault()));
        }
        Map<ModConfigSpec.ConfigValue<?>, Object> frozen = Map.copyOf(defaults);
        fileValues = frozen;
        activeValues = frozen;
    }

    /** 服务器启动：先把老值从旧世界文件播种进实例文件（一次性），再加载 */
    public static synchronized boolean loadForServer(MinecraftServer server) {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            Path worldFile = server.getWorldPath(LevelResource.ROOT)
                    .resolve("serverconfig").resolve(ConfigFileMigration.SERVER_FILE_NAME);
            ConfigFileMigration.migrateAiServerFileIfNeeded(configDir, worldFile, values(), SPEC);
            boolean loaded = loadFromPath(configDir.resolve(ConfigFileMigration.AI_SERVER_FILE_NAME));
            if (!loaded) {
                LOGGER.error("Failed to load AI rule config");
            }
            return loaded;
        } catch (RuntimeException exception) {
            currentFile = null;
            LOGGER.error("Failed to load AI rule config", exception);
            return false;
        }
    }

    static synchronized boolean loadFromPath(Path path) {
        try {
            Map<ModConfigSpec.ConfigValue<?>, Object> loaded = readValidatedValues(path, true);
            currentFile = path;
            fileValues = loaded;
            activeValues = loaded;
            return true;
        } catch (IOException | RuntimeException exception) {
            if (AtomicConfigFileWriter.restoreLastGood(path, candidate -> readValidatedValues(candidate, false))) {
                try {
                    Map<ModConfigSpec.ConfigValue<?>, Object> loaded = readValidatedValues(path, true);
                    currentFile = path;
                    fileValues = loaded;
                    activeValues = loaded;
                    LOGGER.warn("Recovered invalid AI rule config {} from its last-good snapshot", path);
                    return true;
                } catch (IOException | RuntimeException recoveryFailure) {
                    exception.addSuppressed(recoveryFailure);
                }
            }
            LOGGER.error("Failed to load AI rule config {}", path, exception);
            return false;
        }
    }

    /** 手改文件的入口（/tlm ai_chat reload）；失败保留旧快照 */
    public static synchronized boolean reloadFromDisk() {
        Path path = currentFile;
        if (path == null || !Files.isRegularFile(path)) {
            return false;
        }
        try {
            Map<ModConfigSpec.ConfigValue<?>, Object> loaded = readValidatedValues(path, true);
            fileValues = loaded;
            activeValues = loaded;
            return true;
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to reload AI rule config {}", path, exception);
            AtomicConfigFileWriter.restoreLastGood(path, candidate -> readValidatedValues(candidate, false));
            return false;
        }
    }

    public static synchronized void unload() {
        currentFile = null;
        initializeDefaults();
    }

    /** 客户端应用服务器运行时快照，不写本地文件 */
    public static synchronized boolean applyRuntimeJson(String json) {
        final JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException exception) {
            return false;
        }
        Map<ModConfigSpec.ConfigValue<?>, Object> next = new LinkedHashMap<>(activeValues);
        Set<ModConfigSpec.ConfigValue<?>> allowed = Set.copyOf(publicRuntimeValues());
        for (ModConfigSpec.ConfigValue<?> value : values()) {
            JsonElement element = root.get(ServerRuleConfig.key(value));
            if (element == null) {
                continue;
            }
            if (!allowed.contains(value)) {
                return false;
            }
            Object decoded = ServerRuleConfig.decode(value, element);
            if (decoded == null || !value.getSpec().test(decoded)) {
                return false;
            }
            next.put(value, ServerRuleConfig.copyValue(decoded));
        }
        activeValues = Map.copyOf(next);
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object getUnchecked(ModConfigSpec.ConfigValue<?> value) {
        return get((ModConfigSpec.ConfigValue) value);
    }

    private static Map<ModConfigSpec.ConfigValue<?>, Object> readValidatedValues(Path path, boolean completeFile)
            throws IOException {
        CommentedConfig config = ConfigFileMigration.read(path);
        Map<ModConfigSpec.ConfigValue<?>, Object> loaded = new LinkedHashMap<>();
        for (ModConfigSpec.ConfigValue<?> value : values()) {
            Object decoded = ServerRuleConfig.readUnchecked(value, config);
            if (decoded == null || !value.getSpec().test(decoded)) {
                throw new IllegalArgumentException("Invalid AI rule value: " + ServerRuleConfig.key(value));
            }
            loaded.put(value, ServerRuleConfig.copyValue(decoded));
        }
        if (completeFile) {
            boolean needsCompletion = values().stream().anyMatch(value -> !config.contains(value.getPath()));
            if (needsCompletion) {
                completeMissingValues(config);
                ConfigFileMigration.writeAtomically(config, path);
            }
        }
        return Map.copyOf(loaded);
    }

    private static void writeValues(Map<ModConfigSpec.ConfigValue<?>, Object> values) throws IOException {
        Path path = currentFile;
        if (path == null) {
            throw new IllegalStateException("No AI rule config is currently loaded");
        }
        CommentedConfig config = Files.isRegularFile(path)
                ? ConfigFileMigration.read(path)
                : ConfigFileMigration.emptyConfig();
        completeMissingValues(config);
        values.forEach((value, decoded) -> config.set(value.getPath(), ServerRuleConfig.copyValue(decoded)));
        ConfigFileMigration.writeAtomically(config, path);
    }

    private static void completeMissingValues(CommentedConfig config) {
        for (ModConfigSpec.ConfigValue<?> value : values()) {
            if (!config.contains(value.getPath())) {
                config.set(value.getPath(), ServerRuleConfig.copyValue(value.getDefault()));
            }
        }
    }
}
