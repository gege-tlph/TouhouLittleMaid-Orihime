package com.github.tartaricacid.touhoulittlemaid.ai.manager.site;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.SettingReader;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.DefaultLLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.google.common.collect.Maps;
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


    public static final Map<String, LLMSite> LLM_SITES = Maps.newLinkedHashMap();
    public static final Map<String, TTSSite> TTS_SITES = Maps.newLinkedHashMap();
    public static final Map<String, STTSite> STT_SITES = Maps.newLinkedHashMap();

    // 部分默认站点需要进行修正，此处为修正列表
    public static final Map<String, Consumer<LLMSite>> FIXED_LLM_SITES = Maps.newHashMap();

    public static boolean init() {
        return init(createFolder(), true);
    }

    static boolean init(Path root, boolean reloadSettings) {
        Map<String, LLMSite> nextLlm = Maps.newLinkedHashMap();
        Map<String, TTSSite> nextTts = Maps.newLinkedHashMap();
        Map<String, STTSite> nextStt = Maps.newLinkedHashMap();
        Map<String, Consumer<LLMSite>> nextFixes = Maps.newHashMap();
        LLM_SERIALIZER.forEach((key, value) -> nextLlm.put(key, value.defaultSite()));
        TTS_SERIALIZER.forEach((key, value) -> nextTts.put(key, value.defaultSite()));
        STT_SERIALIZER.forEach((key, value) -> nextStt.put(key, value.defaultSite()));
        DefaultLLMSite.addDefaultSites(nextLlm, nextFixes);

        Path llmConfig = root.resolve("llm.json");
        Path ttsConfig = root.resolve("tts.json");
        Path sttConfig = root.resolve("stt.json");
        try {
            if (Files.exists(llmConfig)) {
                Map<String, LLMSite> loaded = LLMSite.readSitesStrict(llmConfig);
                loaded.forEach((siteId, site) -> {
                    Consumer<LLMSite> fix = nextFixes.get(siteId);
                    if (fix != null) {
                        fix.accept(site);
                    }
                });
                nextLlm.putAll(loaded);
            } else if (!LLMSite.writeSites(llmConfig, nextLlm)) {
                return false;
            }
            if (Files.exists(ttsConfig)) {
                nextTts.putAll(TTSSite.readSitesStrict(ttsConfig));
            } else if (!TTSSite.writeSites(ttsConfig, nextTts)) {
                return false;
            }
            if (Files.exists(sttConfig)) {
                nextStt.putAll(STTSite.readSitesStrict(sttConfig));
            } else if (!STTSite.writeSites(sttConfig, nextStt)) {
                return false;
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to reload AI sites; keeping the previous runtime snapshot", exception);
            restore(root.resolve("llm.json"));
            restore(root.resolve("tts.json"));
            restore(root.resolve("stt.json"));
            return false;
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
        return true;
    }

    private static void restore(Path file) {
        com.github.tartaricacid.touhoulittlemaid.config.AtomicConfigFileWriter.restoreLastGood(file, candidate -> {
            try (java.io.Reader reader = Files.newBufferedReader(candidate, java.nio.charset.StandardCharsets.UTF_8)) {
                net.minecraft.util.GsonHelper.parse(reader);
            }
        });
    }

    private static void clearSites() {
        LLM_SITES.clear();
        TTS_SITES.clear();
        STT_SITES.clear();
    }

    private static void addDefaultSites() {
        LLM_SERIALIZER.forEach((key, value) -> AvailableSites.LLM_SITES.put(key, value.defaultSite()));
        TTS_SERIALIZER.forEach((key, value) -> AvailableSites.TTS_SITES.put(key, value.defaultSite()));
        STT_SERIALIZER.forEach((key, value) -> AvailableSites.STT_SITES.put(key, value.defaultSite()));

        // 其他额外的默认站点
        DefaultLLMSite.addDefaultSites();
    }

    private static void readSites() {
        Path root = createFolder();
        Path llmConfig = root.resolve("llm.json");
        Path ttsConfig = root.resolve("tts.json");
        Path sttConfig = root.resolve("stt.json");

        if (Files.exists(llmConfig)) {
            try {
                Map<String, LLMSite> allLLMSiteMap = LLMSite.readSites(llmConfig);
                for (String siteId : allLLMSiteMap.keySet()) {
                    if (FIXED_LLM_SITES.containsKey(siteId)) {
                        LLMSite llmSite = allLLMSiteMap.get(siteId);
                        FIXED_LLM_SITES.get(siteId).accept(llmSite);
                    }
                }
                LLM_SITES.putAll(allLLMSiteMap);
            } catch (Exception e) {
                TouhouLittleMaid.LOGGER.error("Failed to read LLM sites", e);
            }
        }

        if (Files.exists(ttsConfig)) {
            try {
                TTS_SITES.putAll(TTSSite.readSites(ttsConfig));
            } catch (Exception e) {
                TouhouLittleMaid.LOGGER.error("Failed to read TTS sites", e);
            }
        }

        if (Files.exists(sttConfig)) {
            try {
                STT_SITES.putAll(STTSite.readSites(sttConfig));
            } catch (Exception e) {
                TouhouLittleMaid.LOGGER.error("Failed to read STT sites", e);
            }
        }
    }

    public static boolean saveSites() {
        Path root = createFolder();
        boolean saved = saveLLMSites(root) & saveTTSSites(root) & saveSTTSites(root);
        if (!saved) {
            return false;
        }
        SettingReader.reloadSettings();
        return true;
    }

    public static boolean saveSTTSitesOnly() {
        Path root = createFolder();
        return saveSTTSites(root);
    }

    private static boolean saveLLMSites(Path root) {
        Path llmConfig = root.resolve("llm.json");

        try {
            return LLMSite.writeSites(llmConfig, LLM_SITES);
        } catch (Exception e) {
            TouhouLittleMaid.LOGGER.error("Failed to save LLM sites", e);
            return false;
        }
    }

    private static boolean saveTTSSites(Path root) {
        Path ttsConfig = root.resolve("tts.json");

        try {
            return TTSSite.writeSites(ttsConfig, TTS_SITES);
        } catch (Exception e) {
            TouhouLittleMaid.LOGGER.error("Failed to save TTS sites", e);
            return false;
        }
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
