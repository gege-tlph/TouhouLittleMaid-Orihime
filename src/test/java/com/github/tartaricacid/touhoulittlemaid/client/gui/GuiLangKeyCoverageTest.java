package com.github.tartaricacid.touhoulittlemaid.client.gui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 设置屏里引用的每一个 lang 键都必须真实存在于 en_us 与 zh_cn。
 *
 * <p>缺键在界面上表现为**原样印出那串键名**——不崩、不报错、日志干净，只有玩家看得到，
 * 而这一层没有任何自动化能替代实机。这条测试把「键写错 / 只加了一半语言」挡在门外，
 * 剩下的（文案是否通顺、是否说谎）才交给实机。</p>
 *
 * <p>只收**完整字面量**：带 {@code %s} 的模板键（如站点名 {@code ...chat.site.%s.name}）
 * 与用 {@code +} 拼出来的键（如 {@code labelKey + ".tooltip"}）在编译期不可知，跳过。
 * 这意味着本测试给的是**下界**，不是全集——它证明「写下的完整键都在」，
 * 不证明「屏上不会出现裸键」。</p>
 */
class GuiLangKeyCoverageTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path AI_GUI = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "client", "gui", "entity", "maid", "ai"));
    private static final Path LANG = ROOT.resolve(Path.of("src", "main", "resources", "assets",
            "touhou_little_maid", "lang"));

    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");
    /** 本模组自己的两个命名空间；原版键（selectWorld.edit.save 等）不归我们管 */
    private static final Pattern KEY_LITERAL = Pattern.compile(
            "\"((?:ai|config|gui)\\.touhou_little_maid\\.[A-Za-z0-9_.]+)\"");

    /** 这批屏引用的完整键远多于此；低于这个数说明扫描已经失效 */
    private static final int MIN_KEYS = 30;

    private static Set<String> collectKeys() throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        try (Stream<Path> files = Files.walk(AI_GUI)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                source = BLOCK_COMMENT.matcher(source).replaceAll(" ");
                source = LINE_COMMENT.matcher(source).replaceAll(" ");
                Matcher matcher = KEY_LITERAL.matcher(source);
                while (matcher.find()) {
                    String key = matcher.group(1);
                    // 模板键与拼接前缀都是运行期才成形的，编译期不可知：
                    //   "...chat.site.%s.name"        → 模板
                    //   "...chat.summary.state." + s  → 前缀片段（以 . 结尾）
                    // 收进来会得到一个永远查不到的假键——这正是本测试首跑报的两条假阳性。
                    if (key.contains("%") || key.endsWith(".")) {
                        continue;
                    }
                    keys.add(key);
                }
            }
        }
        return keys;
    }

    private static JsonObject readLang(String file) throws IOException {
        return JsonParser.parseString(Files.readString(LANG.resolve(file), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void everyLiteralKeyExistsInBothShippedLanguages() throws IOException {
        Set<String> keys = collectKeys();
        // 活性判据与结论正交：数的是「收集到几个键」，不是「缺了几个」
        assertTrue(keys.size() >= MIN_KEYS,
                "只收集到 %d 个 lang 键（期望 ≥ %d）——正则或扫描路径多半已经失效，这条测试正在零覆盖空转"
                        .formatted(keys.size(), MIN_KEYS));

        JsonObject en = readLang("en_us.json");
        JsonObject zh = readLang("zh_cn.json");

        List<String> missingEn = new ArrayList<>();
        List<String> missingZh = new ArrayList<>();
        for (String key : keys) {
            if (!en.has(key)) {
                missingEn.add(key);
            }
            if (!zh.has(key)) {
                missingZh.add(key);
            }
        }
        assertTrue(missingEn.isEmpty(), "en_us.json 缺少这些键（界面上会原样印出键名）：" + missingEn);
        assertTrue(missingZh.isEmpty(), "zh_cn.json 缺少这些键（中文界面上会退回英文或原样印出键名）：" + missingZh);
    }
}
