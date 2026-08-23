package com.github.tartaricacid.touhoulittlemaid.compat.cloth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
    private static final Path MAIN_JAVA = Path.of("..", "..", "src", "main", "java");
    private static final Path SOURCE = MAIN_JAVA.resolve(Path.of("com", "github",
            "tartaricacid", "touhoulittlemaid", "compat", "cloth", "MenuIntegration.java"));
    private static final Path EN_US = Path.of("..", "..", "src", "main", "resources", "assets",
            "touhou_little_maid", "lang", "en_us.json");
    /**
     * Java 字符串字面量抽取，用于反向扫描的「代码能构造出哪些键」。
     *
     * <p>⚠️ <b>手写扫描而不是正则</b>：{@code "((?:[^"\\]|\\.)*)"} 这种写法在本仓库
     * 1700+ 个源文件上会把 Java 的 {@code Pattern} 递归爆栈（实测 {@code StackOverflowError}），
     * 而同一个模式在 python 的 re 上跑得好好的——所以不能拿原型脚本跑通当作它在 Java 侧也行。</p>
     */
    private static Set<String> stringLiteralsOf(String source) {
        Set<String> out = new LinkedHashSet<>();
        int i = 0;
        while (i < source.length()) {
            if (source.charAt(i) != '"') {
                i++;
                continue;
            }
            StringBuilder sb = new StringBuilder();
            int j = i + 1;
            boolean closed = false;
            while (j < source.length()) {
                char c = source.charAt(j);
                if (c == '\\') {
                    j += 2;          // 转义序列整体跳过：键里不会出现转义，跳过即可
                    sb.setLength(0); // 含转义的字面量一律不参与拼接，免得拼出假键
                    continue;
                }
                if (c == '"') {
                    closed = true;
                    break;
                }
                if (c == '\n') {
                    break;           // 未闭合（多半是注释残渣），放弃这一段
                }
                sb.append(c);
                j++;
            }
            if (closed) {
                out.add(sb.toString());
                i = j + 1;
            } else {
                i++;
            }
        }
        return out;
    }

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

    /**
     * 反向：lang 里每一个 {@code config.touhou_little_maid.*} 键，都必须有代码能构造出来。
     *
     * <p>上一条查的是「菜单要的键 lang 里有没有」，这一条查反面：**孤儿键**。
     * 它是「资源先行搬了、代码侧没搬完」的现成信号——菜单分组重构那一轮，
     * {@code menu.personal.*} 四条分组名早就躺在 lang 里没人用，那本身就是重构没做的证据。
     * 也照得出反向的一种：宿主把配置键换成了别的机制（物品标签），lang 没人清。</p>
     *
     * <p><b>判据按成因写</b>：成因是「有没有代码能构造出这个<b>完整的</b>键」，
     * 不是「有没有哪个字面量恰好是它的最后一段」。后者是本轮实测过的假阴性——
     * 用「全仓任意字面量 + {@code .} 边界」判，{@code TagItem.createTagKey("maid_tamed_item")}
     * 这个毫不相干的字面量会把死键 {@code ...maid.maid_tamed_item} 判成活的。
     * 所以拼接的两半必须**同源**：前缀常量与被拼的字面量取自同一个文件。</p>
     *
     * <p>实查全仓只有三种构造通道，下面逐条实现：① 整键字面量（AI 设置屏那批
     * {@code Component.translatable("config.touhou_little_maid.global_ai.llm_enable")}），
     * 且 {@code addRuleToggle} 会自己接 {@code .tooltip}；② 前缀常量 + 同文件字面量
     * （各 subconfig 的 {@code TRANSLATE_KEY + "." + key}、菜单的 {@code CATEGORY + "x"}）；
     * ③ 前缀常量本身（{@code builder.translation(TRANSLATE_KEY)} 的分节标题）。</p>
     */
    @Test
    void noOrphanConfigLangKeys() throws IOException {
        Map<Path, Set<String>> literalsByFile = new LinkedHashMap<>();
        try (Stream<Path> walk = Files.walk(MAIN_JAVA)) {
            for (Path java : walk.filter(p -> p.toString().endsWith(".java")).toList()) {
                String src = stripComments(Files.readString(java, StandardCharsets.UTF_8));
                literalsByFile.put(java, stringLiteralsOf(src));
            }
        }

        Set<String> constructible = new HashSet<>();
        int prefixConstants = 0;
        for (Set<String> literals : literalsByFile.values()) {
            List<String> prefixes = literals.stream().filter(l -> l.startsWith(PREFIX)).toList();
            for (String pre : prefixes) {
                prefixConstants++;
                constructible.add(pre);                 // ③ 分节标题 / 整键
                constructible.add(pre + ".tooltip");    // ① addRuleToggle 自接的悬停键
                for (String lit : literals) {           // ② 只与**同文件**的字面量拼
                    if (lit.isEmpty()) {
                        continue;
                    }
                    constructible.add(pre + "." + lit);
                    constructible.add(pre + "." + lit + ".tooltip");
                    constructible.add(pre + lit);
                    constructible.add(pre + lit + ".tooltip");
                }
            }
        }

        // 活性断言：与结论正交——「没有孤儿键」与「扫描根本没跑」在结果上完全一样，
        // 所以活性必须用独立于结论的量（走过几个文件 / 认出几个前缀常量）来证。
        assertTrue(literalsByFile.size() >= 1500,
                "只扫到 " + literalsByFile.size() + " 个 java 文件，扫描面可能已失效");
        assertTrue(prefixConstants >= 15,
                "只认出 " + prefixConstants + " 个 config 前缀常量，识别依据可能已失效");

        JsonObject lang = JsonParser.parseString(Files.readString(EN_US, StandardCharsets.UTF_8)).getAsJsonObject();
        List<String> orphans = new ArrayList<>();
        int checked = 0;
        for (String key : lang.keySet()) {
            if (!key.startsWith(PREFIX)) {
                continue;
            }
            checked++;
            if (!constructible.contains(key)) {
                orphans.add(key);
            }
        }
        assertTrue(checked >= 150, "只查了 " + checked + " 个 config 键，lang 命名空间可能已改");
        assertTrue(orphans.isEmpty(),
                "这些 config 翻译键全仓没有任何代码能构造出来（死键，多半是代码侧没搬完或已被换掉）：" + orphans);
    }

    /** 注释里可能写着示例调用，剥掉免得变成假阳性。 */
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
    }
}
