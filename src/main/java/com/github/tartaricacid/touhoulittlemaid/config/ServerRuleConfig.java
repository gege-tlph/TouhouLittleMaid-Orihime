package com.github.tartaricacid.touhoulittlemaid.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ChairConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ExperimentalConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig;
import com.github.tartaricacid.touhoulittlemaid.event.MaidMealRegConfigEvent;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.jetbrains.annotations.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 专服全局菜单可编辑的服务器权威配置白名单（**存档级**玩法规则）。
 *
 * <p>这些值仍保存在原有 common/server TOML 中，避免迁移配置键；客户端只持有菜单快照，
 * 保存时由服务器重新解析、校验并写入自己的配置文件。</p>
 *
 * <p>⚠️ <b>镜像契约</b>：{@link AiServerRuleConfig} 是本类状态机的镜像（AI 规则于 §17 v2 拆去
 * 实例级）。凡改动本类的快照簿记 / 加载恢复 / applyJson 语义，**必须同步检查那边**；
 * 编解码与 IO 已共享（{@code decode}/{@code copyValue}/{@code readUnchecked}/
 * {@code ConfigFileMigration}/{@code AtomicConfigFileWriter}），不许各写各的。
 * 两店的有意差异只有三条：文件作用域（存档 vs 实例）、激活策略（专服延迟 vs 恒立即）、
 * 派生状态（本类有 meal regex 刷新，AI 店无）。若将来需要第三家店，先抽公共引擎再添。</p>
 */
public final class ServerRuleConfig {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogManager.getLogger(ServerRuleConfig.class);
    private static volatile Map<ModConfigSpec.ConfigValue<?>, Object> activeValues = Map.of();
    private static volatile Map<ModConfigSpec.ConfigValue<?>, Object> fileValues = Map.of();
    private static volatile @Nullable Path currentWorldFile;

    private ServerRuleConfig() {
    }

    public static String snapshotJson() {
        JsonObject root = new JsonObject();
        Map<ModConfigSpec.ConfigValue<?>, Object> snapshot = fileValues;
        for (ModConfigSpec.ConfigValue<?> value : values()) {
            root.add(key(value), GSON.toJsonTree(snapshot.getOrDefault(value, value.getDefault())));
        }
        return GSON.toJson(root);
    }

    public static String runtimeSnapshotJson() {
        return snapshotJson(publicRuntimeValues(), true);
    }

    private static String snapshotJson(List<ModConfigSpec.ConfigValue<?>> source, boolean active) {
        JsonObject root = new JsonObject();
        for (ModConfigSpec.ConfigValue<?> value : source) {
            root.add(key(value), GSON.toJsonTree(active ? getUnchecked(value) : value.get()));
        }
        return GSON.toJson(root);
    }

    /**
     * 事务式应用客户端提交。任何已知字段无法解析或未通过原配置校验时，整批拒绝。
     */
    public static boolean applyJson(String json, boolean activate) {
        final JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException exception) {
            return false;
        }

        Map<ModConfigSpec.ConfigValue<?>, Object> pending = new LinkedHashMap<>();
        for (ModConfigSpec.ConfigValue<?> value : values()) {
            JsonElement element = root.get(key(value));
            if (element == null) {
                continue;
            }
            Object decoded = decode(value, element);
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
            writeWorldValues(next);
            fileValues = Map.copyOf(next);
            if (activate) {
                activeValues = Map.copyOf(next);
                refreshDerivedState();
            }
            return true;
        } catch (RuntimeException | IOException exception) {
            return false;
        }
    }

    /** Initializes client-side placeholders before a server runtime snapshot is received. */
    public static synchronized void initializeDefaults() {
        Map<ModConfigSpec.ConfigValue<?>, Object> defaults = defaultValues();
        fileValues = defaults;
        activeValues = defaults;
        refreshDerivedState();
    }

    /** Creates/migrates and loads the current save's authoritative server rules. */
    public static synchronized boolean loadForServer(MinecraftServer server) {
        try {
            Path path = ConfigFileMigration.prepareWorldFile(server, values(), ServerConfig.CONFIG);
            boolean loaded = loadFromPath(path);
            if (!loaded) {
                currentWorldFile = null;
                LOGGER.error("Failed to load current world config {}", path);
            }
            return loaded;
        } catch (IOException | RuntimeException exception) {
            currentWorldFile = null;
            LOGGER.error("Failed to load current world config", exception);
            return false;
        }
    }

    static synchronized boolean loadFromPath(Path path) {
        try {
            Map<ModConfigSpec.ConfigValue<?>, Object> loaded = readValidatedValues(path, true);
            currentWorldFile = path;
            fileValues = loaded;
            activeValues = loaded;
            refreshDerivedState();
            return true;
        } catch (IOException | RuntimeException exception) {
            if (AtomicConfigFileWriter.restoreLastGood(path, candidate -> {
                readValidatedValues(candidate, false);
            })) {
                try {
                    Map<ModConfigSpec.ConfigValue<?>, Object> loaded = readValidatedValues(path, true);
                    currentWorldFile = path;
                    fileValues = loaded;
                    activeValues = loaded;
                    refreshDerivedState();
                    LOGGER.warn("Recovered invalid world config {} from its last-good snapshot", path);
                    return true;
                } catch (IOException | RuntimeException recoveryFailure) {
                    exception.addSuppressed(recoveryFailure);
                }
            }
            return false;
        }
    }

    public static synchronized void unloadWorld() {
        currentWorldFile = null;
        initializeDefaults();
    }

    /** Applies the most recently loaded file values as the gameplay snapshot. */
    public static synchronized void activatePendingValues() {
        activeValues = Map.copyOf(fileValues);
        refreshDerivedState();
    }

    /** Reads the environment-owned TOML directly and atomically replaces both snapshots. */
    public static synchronized boolean reloadFromDisk() {
        Path path = currentWorldFile;
        if (path == null) {
            return false;
        }
        if (!Files.isRegularFile(path)) {
            return false;
        }

        try {
            Map<ModConfigSpec.ConfigValue<?>, Object> loaded = readValidatedValues(path, true);
            fileValues = loaded;
            activeValues = loaded;
            refreshDerivedState();
            return true;
        } catch (IOException | RuntimeException exception) {
            LOGGER.error("Failed to reload current world config {}", path, exception);
            AtomicConfigFileWriter.restoreLastGood(path, candidate -> {
                readValidatedValues(candidate, false);
            });
            return false;
        }
    }

    /** Applies a server runtime snapshot without modifying the client's config file. */
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
            JsonElement element = root.get(key(value));
            if (element == null) {
                continue;
            }
            if (!allowed.contains(value)) {
                return false;
            }
            Object decoded = decode(value, element);
            if (decoded == null || !value.getSpec().test(decoded)) {
                return false;
            }
            next.put(value, copyValue(decoded));
        }
        activeValues = Map.copyOf(next);
        refreshDerivedState();
        return true;
    }

    /**
     * 唯一读口。AI 规则已迁到 {@link AiServerRuleConfig}，但全仓几十处读点仍从这里进——
     * 按键归属路由，不让「配置搬了家、读的人不知道」成为新的所有权混放事故面。
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        if (AiServerRuleConfig.owns(value)) {
            return AiServerRuleConfig.get(value);
        }
        Object active = activeValues.get(value);
        return active == null ? value.get() : (T) active;
    }

    public static String key(ModConfigSpec.ConfigValue<?> value) {
        return String.join(".", value.getPath());
    }

    /** 保存包按键分拣用：本店认领的键名全集 */
    public static Set<String> jsonKeys() {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (ModConfigSpec.ConfigValue<?> value : values()) {
            keys.add(key(value));
        }
        return Set.copyOf(keys);
    }

    public static List<ModConfigSpec.ConfigValue<?>> values() {
        return List.of(
                MaidConfig.MAID_TAMED_ITEM,
                MaidConfig.MAID_TEMPTATION_ITEM,
                MaidConfig.MAID_WORK_RANGE,
                MaidConfig.MAID_IDLE_RANGE,
                MaidConfig.MAID_SLEEP_RANGE,
                MaidConfig.MAID_NON_HOME_RANGE,
                MaidConfig.BOW_RANGE,
                MaidConfig.CROSS_BOW_RANGE,
                MaidConfig.DANMAKU_RANGE,
                MaidConfig.TRIDENT_RANGE,
                // 枪械（TaCZ）三档识别距离。**必须在本清单里**：get() 对未认领的键会回落到裸 spec，
                // 而裸 spec 在集成服务端的 tick 期没加载 → IllegalStateException 崩服（2026-08-14 实测两次）
                MaidConfig.MAID_GUN_LONG_DISTANCE,
                MaidConfig.MAID_GUN_MEDIUM_DISTANCE,
                MaidConfig.MAID_GUN_NEAR_DISTANCE,
                // 枪械（TaCZ）三档识别距离。**必须在本清单里**：get() 对未认领的键会回落到裸 spec，
                // 而裸 spec 在集成服务端的 tick 期没加载 → IllegalStateException 崩服（2026-08-14 实测两次）
                MaidConfig.FEED_ANIMAL_MAX_NUMBER,
                MaidConfig.MAID_CHANGE_MODEL,
                MaidConfig.MAID_GOMOKU_OWNER_LIMIT,
                MaidConfig.OWNER_MAX_MAID_NUM,
                MaidConfig.REPLACE_ALLAY_PERCENT,
                MaidConfig.ENABLE_EMOJI,
                MaidConfig.EMOJI_CHECK_RATE,
                MaidConfig.IMAGE_EMOJI_WEIGHT,
                MaidConfig.KAOMOJI_EMOJI_WEIGHT,
                MaidConfig.MAID_BACKPACK_BLACKLIST,
                MaidConfig.MAID_ATTACK_IGNORE,
                MaidConfig.MAID_RANGED_ATTACK_IGNORE,
                MaidConfig.MAID_WORK_MEALS_BLOCK_LIST,
                MaidConfig.MAID_HOME_MEALS_BLOCK_LIST,
                MaidConfig.MAID_HEAL_MEALS_BLOCK_LIST,
                MaidConfig.MAID_WORK_MEALS_BLOCK_LIST_REGEX,
                MaidConfig.MAID_HOME_MEALS_BLOCK_LIST_REGEX,
                MaidConfig.MAID_HEAL_MEALS_BLOCK_LIST_REGEX,
                MaidConfig.MAID_EATEN_RETURN_CONTAINER_LIST,
                ChairConfig.CHAIR_CHANGE_MODEL,
                ChairConfig.CHAIR_CAN_DESTROYED_BY_ANYONE,
                MiscConfig.MAID_FAIRY_POWER_POINT,
                MiscConfig.MAID_FAIRY_SPAWN_PROBABILITY,
                MiscConfig.MAID_FAIRY_BLACKLIST_DIMENSION,
                MiscConfig.PLAYER_DEATH_LOSS_POWER_POINT,
                MiscConfig.GIVE_SMART_SLAB,
                MiscConfig.GIVE_PATCHOULI_BOOK,
                MiscConfig.SHRINE_LAMP_EFFECT_COST,
                MiscConfig.SHRINE_LAMP_MAX_STORAGE,
                MiscConfig.SHRINE_LAMP_MAX_RANGE,
                MiscConfig.SCARECROW_RANGE,
                ExperimentalConfig.SMOOTH_FOLLOW,
                // AI 规则已整体迁往 AiServerRuleConfig（实例级），本表不再含任何 AI 条目
                ServerConfig.CLIENT_PACK_DOWNLOAD_URLS,
                ServerConfig.MAID_AI_TIME_DEBUG,
                ServerConfig.MAID_BACKUP_INTERVAL_SECONDS,
                ServerConfig.MAID_BACKUP_MAX_COUNT
        );
    }

    private static List<ModConfigSpec.ConfigValue<?>> publicRuntimeValues() {
        List<ModConfigSpec.ConfigValue<?>> result = new ArrayList<>(values());
        result.remove(ServerConfig.MAID_AI_TIME_DEBUG);
        result.remove(ServerConfig.MAID_BACKUP_INTERVAL_SECONDS);
        result.remove(ServerConfig.MAID_BACKUP_MAX_COUNT);
        return result;
    }

    /** 编解码三件套开放给 {@link AiServerRuleConfig} 复用（同包）：两店一套语义，别各写各的 */
    static Object copyValue(Object value) {
        if (value instanceof List<?> list) {
            List<Object> copy = new ArrayList<>();
            for (Object element : list) {
                copy.add(copyValue(element));
            }
            return List.copyOf(copy);
        }
        return value;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object getUnchecked(ModConfigSpec.ConfigValue<?> value) {
        return get((ModConfigSpec.ConfigValue) value);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static Object readUnchecked(ModConfigSpec.ConfigValue value, Config config) {
        return value.getRaw(config, value.getPath(), value::getDefault);
    }

    /**
     * 伪造包防线：字符串一律封顶。GUI 端 512 的输入上限只是君子锁——改装客户端可以直塞 1MB
     * 字符串（载荷解码上限才拦 1MB），它会被写进 TOML、进公开快照、再广播给**每个进服玩家**。
     * 4096 覆盖最长的合法值（下载 URL、代理地址）仍有富余。
     */
    private static final int MAX_STRING_VALUE_LENGTH = 4096;

    static @Nullable Object decode(ModConfigSpec.ConfigValue<?> value, JsonElement element) {
        try {
            Object defaultValue = value.getDefault();
            if (defaultValue instanceof Boolean) {
                return element.getAsBoolean();
            }
            if (defaultValue instanceof Integer) {
                return element.getAsInt();
            }
            if (defaultValue instanceof Long) {
                return element.getAsLong();
            }
            if (defaultValue instanceof Float) {
                return element.getAsFloat();
            }
            if (defaultValue instanceof Double) {
                return element.getAsDouble();
            }
            if (defaultValue instanceof String) {
                return boundedString(element);
            }
            if (defaultValue instanceof Enum<?> enumValue) {
                return decodeEnum(enumValue.getDeclaringClass(), element.getAsString());
            }
            if (defaultValue instanceof List<?>) {
                return decodeList(value, element.getAsJsonArray());
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static String boundedString(JsonElement element) {
        String decoded = element.getAsString();
        if (decoded.length() > MAX_STRING_VALUE_LENGTH) {
            throw new IllegalArgumentException("String value exceeds " + MAX_STRING_VALUE_LENGTH + " chars");
        }
        return decoded;
    }

    private static List<?> decodeList(ModConfigSpec.ConfigValue<?> value, JsonArray array) {
        if (value == MaidConfig.MAID_EATEN_RETURN_CONTAINER_LIST) {
            List<List<String>> result = new ArrayList<>();
            for (JsonElement element : array) {
                JsonArray pair = element.getAsJsonArray();
                if (pair.size() != 2) {
                    throw new IllegalArgumentException("Container return entry must contain exactly two item ids");
                }
                result.add(List.of(boundedString(pair.get(0)), boundedString(pair.get(1))));
            }
            return result;
        }

        List<String> result = new ArrayList<>();
        for (JsonElement element : array) {
            result.add(boundedString(element));
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static @Nullable Enum<?> decodeEnum(Class<?> enumClass, String name) {
        for (Object constant : enumClass.getEnumConstants()) {
            Enum<?> enumValue = (Enum<?>) constant;
            if (enumValue.name().equalsIgnoreCase(name)) {
                return enumValue;
            }
        }
        return null;
    }

    private static Map<ModConfigSpec.ConfigValue<?>, Object> defaultValues() {
        Map<ModConfigSpec.ConfigValue<?>, Object> defaults = new LinkedHashMap<>();
        for (ModConfigSpec.ConfigValue<?> value : values()) {
            defaults.put(value, copyValue(value.getDefault()));
        }
        return Map.copyOf(defaults);
    }

    private static Map<ModConfigSpec.ConfigValue<?>, Object> readValidatedValues(Path path,
                                                                                 boolean completeFile)
            throws IOException {
        CommentedConfig config = ConfigFileMigration.read(path);
        Map<ModConfigSpec.ConfigValue<?>, Object> loaded = new LinkedHashMap<>();
        for (ModConfigSpec.ConfigValue<?> value : values()) {
            Object decoded = readUnchecked(value, config);
            if (decoded == null || !value.getSpec().test(decoded)) {
                throw new IllegalArgumentException("Invalid world config value: " + key(value));
            }
            loaded.put(value, copyValue(decoded));
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

    private static void writeWorldValues(Map<ModConfigSpec.ConfigValue<?>, Object> values)
            throws IOException {
        Path path = currentWorldFile;
        if (path == null) {
            throw new IllegalStateException("No world config is currently loaded");
        }
        CommentedConfig config = Files.isRegularFile(path)
                ? ConfigFileMigration.read(path)
                : ConfigFileMigration.emptyConfig();
        completeMissingValues(config);
        values.forEach((value, decoded) -> config.set(value.getPath(), copyValue(decoded)));
        ConfigFileMigration.writeAtomically(config, path);
    }

    private static void completeMissingValues(CommentedConfig config) {
        for (ModConfigSpec.ConfigValue<?> value : values()) {
            if (!config.contains(value.getPath())) {
                config.set(value.getPath(), copyValue(value.getDefault()));
            }
        }
    }

    private static void refreshDerivedState() {
        MaidMealRegConfigEvent.handleConfig(
                get(MaidConfig.MAID_WORK_MEALS_BLOCK_LIST_REGEX),
                MaidMealRegConfigEvent.WORK_MEAL_REGEX
        );
        MaidMealRegConfigEvent.handleConfig(
                get(MaidConfig.MAID_HOME_MEALS_BLOCK_LIST_REGEX),
                MaidMealRegConfigEvent.HOME_MEAL_REGEX
        );
        MaidMealRegConfigEvent.handleConfig(
                get(MaidConfig.MAID_HEAL_MEALS_BLOCK_LIST_REGEX),
                MaidMealRegConfigEvent.HEAL_MEAL_REGEX
        );
    }
}
