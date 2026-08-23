package com.github.tartaricacid.touhoulittlemaid.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 模型图标缓存的接线契约。链路两端都要有人：
 *
 * <p><b>生产者半边</b>（队列填充）：{@code MaidModels.addPack} / {@code ChairModels.addPack} 必须登记
 * 缓存队列，{@code CustomPackLoader.reloadPacks} 必须清队列。这正是 1.21.11 分支在重构出
 * {@code AbstractClientModels} 时静默丢掉的那半——丢了之后队列恒空、缓存屏永不弹出，
 * 三个 GUI 静默退回活体渲染，实机上看不出任何异常，属最难发现的纸面接口形态。</p>
 *
 * <p><b>消费者半边</b>（入口路由）：三个模型 GUI 的构造必须全部走 {@code CacheIconManager.openXxx}，
 * 任何 {@code new XxxModelGui(...)} 直连都会绕过缓存屏。</p>
 *
 * <p>按证伪表教训：方法内断言先用大括号配对把范围缩到方法体；扫描前剥 {@code //} 与
 * {@code /* *}{@code /}（含 javadoc）两类注释；每条枚举扫描自带「找到了几个看守对象」的下限断言。</p>
 */
class CacheIconWiringContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path ROOT = Path.of("..", "..");
    private static final Path MAIN_JAVA = ROOT.resolve("src/main/java");
    private static final String PKG = "src/main/java/com/github/tartaricacid/touhoulittlemaid";

    private static final List<String> GUI_CLASSES =
            List.of("MaidModelGui", "ChairModelGui", "ModelSwitcherModelGui");

    /** 判据按成因维度（「拿到了构造」）定：简单名与全限定名两种书写形态都算直连。 */
    private static Pattern constructorPattern(String simpleName) {
        return Pattern.compile("new\\s+(?:[\\w.]+\\.)?" + simpleName + "\\s*\\(");
    }

    @Test
    void guiConstructionIsRoutedThroughCacheIconManagerOnly() throws IOException {
        Path manager = ROOT.resolve(PKG + "/client/gui/entity/cache/CacheIconManager.java");
        String managerSource = stripComments(Files.readString(manager, StandardCharsets.UTF_8));
        for (String clazz : GUI_CLASSES) {
            assertTrue(constructorPattern(clazz).matcher(managerSource).find(),
                    "CacheIconManager 里找不到 new " + clazz + "(——路由中枢自己不建屏，说明扫描对象或实现挪了位置");
        }

        List<String> offenders = new ArrayList<>();
        int scanned = 0;
        try (Stream<Path> walk = Files.walk(MAIN_JAVA)) {
            for (Path file : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                scanned++;
                if (file.getFileName().toString().equals("CacheIconManager.java")) {
                    continue;
                }
                String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                for (String clazz : GUI_CLASSES) {
                    if (constructorPattern(clazz).matcher(source).find()) {
                        offenders.add(file.getFileName() + " -> new " + clazz + "(");
                    }
                }
            }
        }
        assertTrue(scanned > 1000, "只扫到 " + scanned + " 个源文件，扫描多半是坏的");
        assertEquals(List.of(), offenders,
                "这些直连构造绕过了 CacheIconManager：缓存屏被跳过，图标缓存对该入口静默失效");
    }

    @Test
    void queueFillingIsWiredIntoPackLoading() throws IOException {
        // 1.21.11 分支正是把这三处接线在重构时丢掉的——队列恒空则整个功能是死代码
        assertBodyContains(PKG + "/client/resource/models/MaidModels.java",
                "void addPack", "CacheIconManager.addMaidPack");
        assertBodyContains(PKG + "/client/resource/models/ChairModels.java",
                "void addPack", "CacheIconManager.addChairPack");
        assertBodyContains(PKG + "/client/resource/loader/CustomPackLoader.java",
                "void reloadPacks", "CacheIconManager.clearCache");
    }

    @Test
    void guiIconBranchesConsultTheCache() throws IOException {
        List<String> guis = List.of(
                PKG + "/client/gui/entity/model/MaidModelGui.java",
                PKG + "/client/gui/entity/model/ChairModelGui.java",
                PKG + "/client/gui/block/ModelSwitcherModelGui.java");
        for (String gui : guis) {
            assertBodyContains(gui, "void drawRightEntity", "getCacheIconId");
            assertBodyContains(gui, "void drawRightEntity", "CacheIconManager.isIconCached");
        }
    }

    @Test
    void modelIconCacheIsPersonalConfigNotWorldRule() throws IOException {
        // 个人配置进只在客户端注册的 CLIENT spec（initClient → GeneralConfig → -global.toml），
        // 绝不能进 SERVER spec：声明在哪一侧决定读法（裸 get 还是 ServerRuleConfig 读口），
        // 放错侧会被 ArchUnit 闸拦或运行期炸。
        // 2026-08-17：本键随「个人配置整层搬回 -global.toml」从 initCommon 改到 initClient；
        // 图标缓存是纯客户端行为，专服连这个键都不该有。
        String misc = PKG + "/config/subconfig/MiscConfig.java";
        assertBodyContains(misc, "void initClient", "MODEL_ICON_CACHE = builder.define(\"EnableModelIconCache\", false)");
        String serverRuleBody = methodBody(misc, "void initServerRule");
        assertTrue(!serverRuleBody.contains("MODEL_ICON_CACHE"),
                "MODEL_ICON_CACHE 出现在 initServerRule——个人配置被declared成了世界规则");
    }

    @Test
    void langKeysExistInBothLanguages() throws IOException {
        List<String> keys = List.of(
                "\"config.touhou_little_maid.misc.model_icon_cache\"",
                "\"config.touhou_little_maid.misc.model_icon_cache.tooltip\"",
                "\"gui.touhou_little_maid.cache_screen.progress\"",
                "\"gui.touhou_little_maid.cache_screen.desc\"");
        for (String lang : List.of("en_us", "zh_cn")) {
            Path file = ROOT.resolve("src/main/resources/assets/touhou_little_maid/lang/" + lang + ".json");
            String source = Files.readString(file, StandardCharsets.UTF_8);
            for (String key : keys) {
                assertTrue(source.contains(key), lang + ".json 缺少 " + key);
            }
        }
    }

    private static void assertBodyContains(String relativeFile, String methodMarker, String expected) throws IOException {
        String body = methodBody(relativeFile, methodMarker);
        assertTrue(body.contains(expected),
                relativeFile + " 的 " + methodMarker + " 方法体里找不到 " + expected + "——接线丢失");
    }

    /** 取 methodMarker 所在方法的花括号配对方法体；范围缩到方法体，避免「文件里恰好别处也有」的假阳性。 */
    private static String methodBody(String relativeFile, String methodMarker) throws IOException {
        String source = stripComments(Files.readString(ROOT.resolve(relativeFile), StandardCharsets.UTF_8));
        int markerAt = source.indexOf(methodMarker);
        assertTrue(markerAt >= 0, relativeFile + " 里找不到方法 " + methodMarker);
        assertEquals(-1, source.indexOf(methodMarker, markerAt + 1),
                relativeFile + " 里 " + methodMarker + " 出现多次，方法体定位不再唯一，本测试需要更精确的标记");
        int open = source.indexOf('{', markerAt);
        assertTrue(open >= 0, relativeFile + " 的 " + methodMarker + " 后找不到方法体");
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        throw new AssertionError(relativeFile + " 的 " + methodMarker + " 方法体花括号不配对");
    }

    /** 剥掉块注释（含 javadoc）与行注释；被扫描的文件里没有含 // 或 /* 的字符串字面量，故不做字符串态解析。 */
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
    }
}
