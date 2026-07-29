package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 逐按钮反射审计的自动化部分：AI 界面源码里**字面引用**的每一个 lang 键，
 * 必须同时存在于 zh_cn 与 en_us——按钮/标签显示成裸键（`ai.touhou_little_maid.…`）
 * 就是这类缺陷，肉眼扫屏很容易漏掉冷分支里的那几个（错误红字、极端态文案）。
 *
 * <p>射程外（源码扫描的固有边界，别指望本测试抓到）：
 * ① 动态拼接的键——`%s` 模板、`translateKey(…)`、`CATEGORY + "…"`，以及**以点结尾的前缀
 * 字面量**（`"….summary.state." + state` 这类）。首轮扫描把三个前缀误报成缺键，
 * 人工枚举其成品键后全数存在（state 3、site_check 5、secret 三态 3，2026-07-28 核）；
 * 新增拼接族时要么改成完整字面量，要么自己保证成品键齐全。
 * ② 键存在但文案本身写错——那只有实机验收能看出来。</p>
 */
class GuiLangKeyCoverageTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path SRC = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid"));
    private static final Path LANG = ROOT.resolve(Path.of("src", "main", "resources", "assets",
            "touhou_little_maid", "lang"));

    /** 完整字面键：含 % 的模板串天然不匹配；结尾必须是键段字符，以点收尾的拼接前缀不算 */
    private static final Pattern KEY_LITERAL = Pattern.compile(
            "\"((?:ai|config|gui)\\.touhou_little_maid\\.[A-Za-z0-9_.]*[A-Za-z0-9_])\"");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//.*");

    /** AI 界面与其直接后端的文案面：屏、控件、试听客户端、AI 网络包（校验回绝键住在那里） */
    private static final List<Path> SCOPE = List.of(
            Path.of("client", "gui", "entity", "maid", "ai"),
            Path.of("client", "gui", "widget", "ai"),
            Path.of("client", "sound"),
            Path.of("network", "message", "ai")
    );

    @Test
    void everyLiteralKeyInTheAiGuiExistsInBothLocales() throws IOException {
        Set<String> zh = keysOf(LANG.resolve("zh_cn.json"));
        Set<String> en = keysOf(LANG.resolve("en_us.json"));

        List<String> missing = new ArrayList<>();
        for (Path scope : SCOPE) {
            Path dir = SRC.resolve(scope);
            assertTrue(Files.isDirectory(dir), "audit scope vanished: " + dir);
            try (Stream<Path> files = Files.walk(dir)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String source = stripComments(Files.readString(file));
                    Matcher matcher = KEY_LITERAL.matcher(source);
                    while (matcher.find()) {
                        String key = matcher.group(1);
                        if (!zh.contains(key)) {
                            missing.add(key + " (zh_cn) <- " + file.getFileName());
                        }
                        if (!en.contains(key)) {
                            missing.add(key + " (en_us) <- " + file.getFileName());
                        }
                    }
                }
            }
        }
        assertTrue(missing.isEmpty(),
                "GUI 引用了不存在的 lang 键（会显示成裸键）:\n  " + String.join("\n  ", new TreeSet<>(missing)));
    }

    private static Set<String> keysOf(Path langFile) throws IOException {
        JsonObject json = JsonParser.parseString(Files.readString(langFile)).getAsJsonObject();
        return json.keySet();
    }

    private static String stripComments(String source) {
        return LINE_COMMENT.matcher(BLOCK_COMMENT.matcher(source).replaceAll("")).replaceAll("");
    }
}
