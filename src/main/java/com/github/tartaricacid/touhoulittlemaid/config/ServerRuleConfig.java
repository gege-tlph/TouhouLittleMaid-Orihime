package com.github.tartaricacid.touhoulittlemaid.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ChairConfig;
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

/**
 * 服务器权威的**存档级**玩法规则。
 *
 * <p>值保存在本存档的 {@code serverconfig/touhou_little_maid-server.toml}；客户端只持有快照，
 * 保存时由服务器重新解析、校验并写自己的文件。</p>
 *
 * <p><b>唯一读口是 {@link #get}</b>。{@link ServerConfig} 的 spec 有意不注册，规则值上的
 * {@code XXX.get()} 会当场抛异常——这是有意的机制性保证，见该类的注释。</p>
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
            root.add(key(value), GSON.toJsonTree(active ? getUnchecked(value) : value.getDefault()));
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

    /**
     * Applies the most recently loaded file values as the gameplay snapshot.
     *
     * <p>本刀内**尚无调用点**：它的调用者是客户端断开连接时的复位（离开服务器后，运行期快照要退回本端文件值），
     * 而运行期快照本身要等网络层那一刀。单人档不受影响——退出世界走 {@code SERVER_STOPPED → unloadWorld()}。
     * 恢复锚点与 {@link #jsonKeys()} 同：审计 §3.A 的网络层那一刀。</p>
     */
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
     * 唯一读口。
     *
     * <p>⚠️ 行为基准 {@code port/1.21.11-fabric} 在这里还有一条按键归属路由到
     * {@code AiServerRuleConfig}（实例级 AI 规则）的分支。**那一店随 AI 聊天那一刀一起搬**，
     * 本刀不预留空壳：装了空壳而没有实现，就是本仓库反复栽过的「纸面接口」。
     * 恢复锚点：审计 §3.C 与 §9 的 {@code AiServerRuleMigrationTest} / {@code AiServerRuleAttackTest}
     * 两条用例——它们搬进来的那一轮，必须同时在此加回路由分支。</p>
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(ModConfigSpec.ConfigValue<T> value) {
        Object active = activeValues.get(value);
        return active == null ? (T) value.getDefault() : (T) active;
    }

    public static String key(ModConfigSpec.ConfigValue<?> value) {
        return String.join(".", value.getPath());
    }

    /**
     * 保存包按键分拣用：本店认领的键名全集。
     * 本刀内尚无调用点——保存包属网络层那一刀（审计 §3.A），届时按键归属分拣要用它。
     */
    public static Set<String> jsonKeys() {
        Set<String> keys = new java.util.HashSet<>();
        for (ModConfigSpec.ConfigValue<?> value : values()) {
            keys.add(key(value));
        }
        return Set.copyOf(keys);
    }

    /**
     * 世界规则全集。与行为基准 {@code port/1.21.11-fabric} 的同名方法逐条对齐，**三处有意的差异**：
     *
     * <ul>
     *   <li>{@code MaidConfig.MAID_TAMED_ITEM} / {@code MAID_TEMPTATION_ITEM}：**代码宿主已删**，
     *       26.1 改用物品标签 {@code TagItem.MAID_TAMED_ITEM} / {@code MAID_TEMPTATION_ITEM}
     *       （消费点见 {@code MaidMiscManager} 与 {@code MaidBegTask}）。标签由数据包控制，
     *       不再是配置项，**不存在等价的世界规则**，故不补。</li>
     *   <li>{@code ExperimentalConfig.SMOOTH_FOLLOW}：那个类是我们独有的（上游两个版本都没有），
     *       而它唯一的消费者是跟随手感调优——属审计 §3.E，本刀未搬。**配置项要和它的消费者同批落地**，
     *       否则就是一个改了没反应的开关。恢复锚点：§3.E 那一刀落地时，连同 {@code ExperimentalConfig}
     *       一起加进本表（本表缺键由 {@code completeMissingValues} 自动补默认值，老存档不会因此报错）。</li>
     *   <li>{@code MaidConfig.MAID_GUN_LONG/MEDIUM/NEAR_DISTANCE} 三键：**已随 TACZ 兼容刀入表**
     *       （2026-08-15）。三键必须在此认领——1.21.11 分支实证两次：读口对未认领的键回落到
     *       裸 spec，集成服务端 tick 期一读就崩服（{@code TaskGunAttack.searchRadius} 正在寻路路径上）。
     *       读点：{@code TaskGunAttack.searchRadius} 与 {@code TacInnerCompat.canSee}，
     *       均经 {@code GunRecognitionRange.configFor} 拿配置对象、经本类唯一读口解析。</li>
     * </ul>
     */
    public static List<ModConfigSpec.ConfigValue<?>> values() {
        return List.of(
                MaidConfig.MAID_WORK_RANGE,
                MaidConfig.MAID_IDLE_RANGE,
                MaidConfig.MAID_SLEEP_RANGE,
                MaidConfig.MAID_NON_HOME_RANGE,
                MaidConfig.BOW_RANGE,
                MaidConfig.CROSS_BOW_RANGE,
                MaidConfig.DANMAKU_RANGE,
                MaidConfig.TRIDENT_RANGE,
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
                MaidConfig.MAID_GUN_LONG_DISTANCE,
                MaidConfig.MAID_GUN_MEDIUM_DISTANCE,
                MaidConfig.MAID_GUN_NEAR_DISTANCE,
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
     * 伪造包防线：字符串一律封顶。GUI 端的输入上限只是君子锁——改装客户端可以直塞超长字符串，
     * 它会被写进 TOML、进公开快照、再广播给**每个进服玩家**。
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
