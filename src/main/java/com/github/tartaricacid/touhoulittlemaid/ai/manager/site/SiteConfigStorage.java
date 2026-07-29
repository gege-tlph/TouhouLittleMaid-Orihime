package com.github.tartaricacid.touhoulittlemaid.ai.manager.site;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.config.AtomicConfigFileWriter;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SiteJsonConfigWriter;
import com.google.common.collect.Maps;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.io.Reader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import net.minecraft.util.GsonHelper;

/** Reads and writes the editable site files without mutating the active runtime site maps. */
public final class SiteConfigStorage {
    private static final Path ROOT = FabricLoader.getInstance().getConfigDir()
            .resolve(TouhouLittleMaid.MOD_ID).resolve("sites");
    private static final Path LLM_FILE = ROOT.resolve("llm.json");
    private static final Path TTS_FILE = ROOT.resolve("tts.json");
    private static final Path STT_FILE = ROOT.resolve("stt.json");

    private SiteConfigStorage() {
    }

    public static Map<String, LLMSite> readLLM() {
        return Files.exists(LLM_FILE) ? Maps.newLinkedHashMap(readLLMStrict())
                : Maps.newLinkedHashMap(AvailableSites.LLM_SITES);
    }

    public static Map<String, TTSSite> readTTS() {
        return Files.exists(TTS_FILE) ? Maps.newLinkedHashMap(readTTSStrict())
                : Maps.newLinkedHashMap(AvailableSites.TTS_SITES);
    }

    public static Map<String, STTSite> readSTT() {
        return Files.exists(STT_FILE) ? Maps.newLinkedHashMap(readSTTStrict())
                : Maps.newLinkedHashMap(AvailableSites.STT_SITES);
    }

    /**
     * 磁盘上那些**本安装不认识**的站点 id（未知 {@code api_type}，多半来自没装的扩展）。
     *
     * <p>它们被严格读取跳过，因而不在运行时站点表里——于是「这个 id 有没有被占用」的判断
     * 如果只看运行时表，就会把它们判成空位，让管理员新建一个同 id 的内置站点把人家顶掉。
     * 保存包据此拒绝这类创建。读不出来时返回空集：拿不准就别拦，写入层还有最后一道拒绝。</p>
     */
    public static Set<String> foreignLLMIds() {
        return foreignIds(LLM_FILE, apiType -> SerializerRegister.getLLMSerializer(apiType) != null);
    }

    public static Set<String> foreignTTSIds() {
        return foreignIds(TTS_FILE, apiType -> SerializerRegister.getTTSSerializer(apiType) != null);
    }

    private static Set<String> foreignIds(Path file, java.util.function.Predicate<String> knownApiType) {
        try {
            return SiteJsonConfigWriter.foreignIds(file, knownApiType);
        } catch (IOException | RuntimeException exception) {
            return Set.of();
        }
    }

    public static boolean writeLLM(Map<String, LLMSite> sites) {
        ensureRoot();
        return LLMSite.writeSites(LLM_FILE, sites);
    }

    public static boolean writeTTS(Map<String, TTSSite> sites) {
        ensureRoot();
        return TTSSite.writeSites(TTS_FILE, sites);
    }

    public static boolean writeSTT(Map<String, STTSite> sites) {
        ensureRoot();
        return STTSite.writeSites(STT_FILE, sites);
    }

    private static void ensureRoot() {
        try {
            Files.createDirectories(ROOT);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to create AI site config directory", exception);
        }
    }

    private static Map<String, LLMSite> readLLMStrict() {
        try {
            return LLMSite.readSitesStrict(LLM_FILE);
        } catch (Exception exception) {
            if (restore(LLM_FILE)) {
                try {
                    return LLMSite.readSitesStrict(LLM_FILE);
                } catch (Exception ignored) {
                }
            }
            throw new IllegalStateException("Invalid LLM site config", exception);
        }
    }

    private static Map<String, TTSSite> readTTSStrict() {
        try {
            return TTSSite.readSitesStrict(TTS_FILE);
        } catch (Exception exception) {
            if (restore(TTS_FILE)) {
                try {
                    return TTSSite.readSitesStrict(TTS_FILE);
                } catch (Exception ignored) {
                }
            }
            throw new IllegalStateException("Invalid TTS site config", exception);
        }
    }

    private static Map<String, STTSite> readSTTStrict() {
        try {
            return STTSite.readSitesStrict(STT_FILE);
        } catch (Exception exception) {
            if (restore(STT_FILE)) {
                try {
                    return STTSite.readSitesStrict(STT_FILE);
                } catch (Exception ignored) {
                }
            }
            throw new IllegalStateException("Invalid STT site config", exception);
        }
    }

    private static boolean restore(Path file) {
        return AtomicConfigFileWriter.restoreLastGood(file, candidate -> {
            try (Reader reader = Files.newBufferedReader(candidate, StandardCharsets.UTF_8)) {
                GsonHelper.parse(reader);
            }
        });
    }
}
