package com.github.tartaricacid.touhoulittlemaid.ai.manager.site;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.SettingReader;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.DefaultLLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister.*;

@SuppressWarnings("all")
public class AvailableSites {
    private static final String FOLDER_NAME = "sites";
    private static final Logger LOGGER = LogManager.getLogger(AvailableSites.class);

    // 服务端缓存的站点信息，包含秘钥等敏感信息
    // B8: 类型已从移植期阉割的 Map<String,Object> 复原为 TTSSite/STTSite（服务端 AI/TTS 已恢复）。
    public static final Map<String, LLMSite> LLM_SITES = Maps.newLinkedHashMap();
    public static final Map<String, TTSSite> TTS_SITES = Maps.newLinkedHashMap();
    public static final Map<String, STTSite> STT_SITES = Maps.newLinkedHashMap();

    // 部分默认站点需要进行修正，此处为修正列表
    public static final Map<String, Consumer<LLMSite>> FIXED_LLM_SITES = Maps.newHashMap();

    public static boolean init() {
        return init(createFolder(), managesSttSites(), true);
    }

    /**
     * 测试入口：始终连 STT 一起加载，不受物理端影响。
     */
    static boolean init(Path root, boolean reloadSettings) {
        return init(root, true, reloadSettings);
    }

    /**
     * 专用服务器**不管理 STT 站点**，因而也不生成 {@code stt.json}。
     *
     * <p>「服务器提供 STT」于 2026-07-27 撤除后，语音识别的站点、凭据与调用全在玩家客户端，
     * 专服上这份文件**没有任何消费者**。而它躺在服务端配置目录里本身就在暗示「STT 该在这里配」——
     * 管理员会照着它去找一个并不存在的开关。<b>载体的存在会被读成语义</b>，所以不能只是"留着不用"。</p>
     *
     * <p>判据用物理端而非 {@code MinecraftServer#isDedicatedServer}：本方法在
     * {@code SerializerRegister.init()} 里跑，那时还没有服务器实例。物理端也正是我们要的语义——
     * <b>开了局域网的客户端仍是 CLIENT</b>，它自己就要用这份文件。</p>
     */
    private static boolean managesSttSites() {
        return FabricLoader.getInstance().getEnvironmentType() != EnvType.SERVER;
    }

    static boolean init(Path root, boolean includeStt, boolean reloadSettings) {
        Map<String, LLMSite> nextLlm = Maps.newLinkedHashMap();
        Map<String, TTSSite> nextTts = Maps.newLinkedHashMap();
        Map<String, STTSite> nextStt = Maps.newLinkedHashMap();
        Map<String, Consumer<LLMSite>> nextFixes = Maps.newHashMap();
        LLM_SERIALIZER.forEach((key, value) -> nextLlm.put(key, value.defaultSite()));
        TTS_SERIALIZER.forEach((key, value) -> nextTts.put(key, value.defaultSite()));
        if (includeStt) {
            STT_SERIALIZER.forEach((key, value) -> nextStt.put(key, value.defaultSite()));
        }
        DefaultLLMSite.addDefaultSites(nextLlm, nextFixes);

        Path llmConfig = root.resolve("llm.json");
        Path ttsConfig = root.resolve("tts.json");
        Path sttConfig = root.resolve("stt.json");

        // 每个文件各自成败，互不牵连：一份 stt.json 损坏不得让 llm/tts 一起失效。
        boolean complete = loadService(llmConfig, nextLlm, LLM_SITES,
                () -> readLlmWithFixes(llmConfig, nextFixes),
                () -> LLMSite.writeSites(llmConfig, nextLlm));
        complete &= loadService(ttsConfig, nextTts, TTS_SITES,
                () -> TTSSite.readSitesStrict(ttsConfig),
                () -> TTSSite.writeSites(ttsConfig, nextTts));
        if (includeStt) {
            complete &= loadService(sttConfig, nextStt, STT_SITES,
                    () -> STTSite.readSitesStrict(sttConfig),
                    () -> STTSite.writeSites(sttConfig, nextStt));
        } else if (Files.exists(sttConfig)) {
            // 不删管理员的文件，但要让残留物可解释：否则下一个人看到它又会以为 STT 能在服务端配
            LOGGER.info("{} is no longer used on a dedicated server and can be deleted: "
                    + "speech recognition is configured entirely on each player's client", sttConfig);
        }

        LLM_SITES.clear();
        LLM_SITES.putAll(nextLlm);
        TTS_SITES.clear();
        TTS_SITES.putAll(nextTts);
        STT_SITES.clear();
        STT_SITES.putAll(nextStt);
        FIXED_LLM_SITES.clear();
        FIXED_LLM_SITES.putAll(nextFixes);
        if (reloadSettings) {
            SettingReader.reloadSettings();
        }
        return complete;
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private static Map<String, LLMSite> readLlmWithFixes(Path file, Map<String, Consumer<LLMSite>> fixes)
            throws Exception {
        Map<String, LLMSite> loaded = LLMSite.readSitesStrict(file);
        loaded.forEach((siteId, site) -> {
            Consumer<LLMSite> fix = fixes.get(siteId);
            if (fix != null) {
                fix.accept(site);
            }
        });
        return loaded;
    }

    /**
     * 加载单个站点文件，失败时只降级它自己。
     *
     * <p>降级分两种情形，混同会造成新的破坏：<b>reload</b> 时 {@code runtime} 里是正在使用的站点，
     * 必须原样保住，否则一次读取失败就会把管理员填好的凭据换成空的内置默认值；<b>启动</b> 时
     * {@code runtime} 为空，此时保留 {@code next} 里已经装好的内置默认站点。</p>
     *
     * <p>后者正是 P0 的根因：原实现在任一文件读取失败时整批放弃，启动场景下三个站点表因此保持为空，
     * {@code getSTTSite} 恒返回 null，玩家看到「服务器不提供语音识别」，而真相是服务器提供了、
     * 只是它的站点表是空的——提示把管理员引向了完全错误的方向。</p>
     *
     * @return 是否完整加载；已降级时返回 false，调用方据此把 reload 报告为失败
     */
    private static <T> boolean loadService(Path file, Map<String, T> next, Map<String, T> runtime,
                                           ThrowingSupplier<Map<String, T>> read,
                                           java.util.function.BooleanSupplier writeDefaults) {
        if (!Files.exists(file)) {
            return writeDefaults.getAsBoolean();
        }
        try {
            next.putAll(read.get());
            return true;
        } catch (Exception failure) {
            LOGGER.error("Failed to load AI sites from {}", file, failure);
        }

        // 先把盘上的文件修好；这一步与内存里采用什么互不影响。
        boolean restored = restore(file);

        // 已经在跑：运行时才是最新的，last-good 可能比它旧，重读会造成降级。原样保住。
        if (!runtime.isEmpty()) {
            next.clear();
            next.putAll(runtime);
            LOGGER.error("Keeping the running sites for {}; the file on disk was {}",
                    file, restored ? "restored from its last-good snapshot" : "left damaged");
            return false;
        }

        // 启动阶段没有运行时可保：恢复成功就读恢复后的文件，否则留内置默认站点。
        if (restored) {
            try {
                next.putAll(read.get());
                LOGGER.warn("Recovered AI sites in {} from its last-good snapshot", file);
                return false;
            } catch (Exception stillBroken) {
                LOGGER.error("The last-good snapshot of {} is unusable as well", file, stillBroken);
            }
        }
        LOGGER.error("No runtime snapshot for {} yet; falling back to built-in default sites "
                + "so the service degrades instead of disappearing", file);
        return false;
    }

    private static boolean restore(Path file) {
        return com.github.tartaricacid.touhoulittlemaid.config.AtomicConfigFileWriter.restoreLastGood(file, candidate -> {
            try (java.io.Reader reader = Files.newBufferedReader(candidate, java.nio.charset.StandardCharsets.UTF_8)) {
                net.minecraft.util.GsonHelper.parse(reader);
            }
        });
    }

    public static boolean saveSTTSitesOnly() {
        Path root = createFolder();
        return saveSTTSites(root);
    }



    private static boolean saveSTTSites(Path root) {
        Path sttConfig = root.resolve("stt.json");

        try {
            return STTSite.writeSites(sttConfig, STT_SITES);
        } catch (Exception e) {
            TouhouLittleMaid.LOGGER.error("Failed to save STT sites", e);
            return false;
        }
    }

    public static LLMSite getLLMSite(String siteName) {
        return LLM_SITES.get(siteName);
    }

    public static TTSSite getTTSSite(String siteName) {
        return TTS_SITES.get(siteName);
    }

    public static STTSite getSTTSite(String siteName) {
        return STT_SITES.get(siteName);
    }

    private static Path createFolder() {
        Path root = FabricLoader.getInstance().getConfigDir().resolve(TouhouLittleMaid.MOD_ID).resolve(FOLDER_NAME);
        if (!root.toFile().isDirectory()) {
            try {
                Files.createDirectories(root);
            } catch (Exception e) {
                TouhouLittleMaid.LOGGER.error("Failed to create sites folder", e);
            }
        }
        return root;
    }
}
