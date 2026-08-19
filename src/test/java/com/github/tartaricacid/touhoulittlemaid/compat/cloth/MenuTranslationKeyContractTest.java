package com.github.tartaricacid.touhoulittlemaid.compat.cloth;

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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置菜单里每一个条目都必须有翻译键，否则界面上显示的是裸键名。
 *
 * <p>本仓库已经栽过一次同型：恢复 TACZ 时代码、注册、配置全接好了，唯独漏了 lang，
 * 玩家在「工作安排」里看到的是 {@code task.touhou_little_maid.gun_attack} 这样的裸键。
 * 那次是靠用户实机发现的——菜单条目多、加一条时人眼盯不住，判据必须机械化。</p>
 *
 * <p>只查 {@code en_us}：MC 的 lang 回退保证缺其它语言时显示英文，不会露出裸键。</p>
 */
class MenuTranslationKeyContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path SOURCE = Path.of("..", "..", "src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "compat", "cloth", "MenuIntegration.java");
    private static final Path EN_US = Path.of("..", "..", "src", "main", "resources", "assets",
            "touhou_little_maid", "lang", "en_us.json");

    /**
     * 条目助手：{@code serverSlider(entries, "maid.bow_range", ...)}。它们内部统一走
     * {@code tr(key)} 做标题、{@code tip(key)} 做悬停，故一个条目要两个键。
     *
     * <p><b>不能只扫 {@code tr("字面量")}</b>——那样只认得出直接调用的少数几处，绝大多数条目的
     * 键名是**参数**。下面的下限断言就是防这个的：识别依据一变就静默零覆盖，而零覆盖恒绿。</p>
     */
    private static final Pattern ENTRY_HELPER = Pattern.compile(
            "\\b(?:localBoolean|serverBoolean|serverInt|serverSlider|serverDouble|serverString|serverList)"
                    + "\\(\\s*entries\\s*,\\s*\"([^\"]+)\"");
    /** 子分类标题：只有标题没有悬停，且前缀是 menu. */
    private static final Pattern SUB_CATEGORY =
            Pattern.compile("\\bsub\\(\\s*entries\\s*,\\s*\"([^\"]+)\"");
    /** 少数条目直接调 tr/tip */
    private static final Pattern DIRECT_CALL = Pattern.compile("\\b(tr|tip)\\(\"([^\"]+)\"\\)");
    /** 栏目标题与按钮文案：{@code CATEGORY + "personal"} 这种前缀常量拼接。 */
    private static final Pattern CATEGORY_CONCAT = Pattern.compile("\\bCATEGORY\\s*\\+\\s*\"([^\"]+)\"");

    private static final String PREFIX = "config.touhou_little_maid.";
    private static final String CATEGORY_PREFIX = "config.touhou_little_maid.menu.";

    @Test
    void everyMenuEntryHasAnEnglishTranslation() throws IOException {
        String source = stripComments(Files.readString(SOURCE, StandardCharsets.UTF_8));
        JsonObject lang = JsonParser.parseString(Files.readString(EN_US, StandardCharsets.UTF_8)).getAsJsonObject();

        Set<String> required = new LinkedHashSet<>();
        Matcher entry = ENTRY_HELPER.matcher(source);
        while (entry.find()) {
            required.add(PREFIX + entry.group(1));
            required.add(PREFIX + entry.group(1) + ".tooltip");
        }
        Matcher category = SUB_CATEGORY.matcher(source);
        while (category.find()) {
            required.add(CATEGORY_PREFIX + category.group(1));
        }
        Matcher direct = DIRECT_CALL.matcher(source);
        while (direct.find()) {
            String key = PREFIX + direct.group(2);
            required.add("tip".equals(direct.group(1)) ? key + ".tooltip" : key);
        }
        Matcher concat = CATEGORY_CONCAT.matcher(source);
        while (concat.find()) {
            required.add(CATEGORY_PREFIX + concat.group(1));
        }

        // 下限断言：识别依据（助手名）一变就静默零覆盖，而零覆盖的测试永远是绿的
        assertTrue(required.size() >= 60,
                "只认出 " + required.size() + " 个菜单翻译键，识别依据可能已失效");

        List<String> missing = new ArrayList<>();
        for (String key : required) {
            if (!lang.has(key)) {
                missing.add(key);
            }
        }
        assertTrue(missing.isEmpty(),
                "配置菜单里这些条目没有英文翻译，界面上会显示裸键名：" + missing);
    }

    /** 注释里可能写着示例调用，剥掉免得变成假阳性。 */
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
    }
}
