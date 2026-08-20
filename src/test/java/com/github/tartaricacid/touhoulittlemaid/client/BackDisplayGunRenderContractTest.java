package com.github.tartaricacid.touhoulittlemaid.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 背部展示格里的枪械必须走 TACZ 自己的渲染器，不能走通用物品渲染。
 *
 * <p><b>这是接线判据（🟡），不是行为判据</b>：GameTest 是纯服务端，客户端渲染在那里不加载，
 * 「背上那把枪看起来对不对」只有实机能验。明写这一点是为了不让「测试通过」被读成「渲染正确」。</p>
 *
 * <p>契约有<b>两个动词</b>，各配一条用例：</p>
 * <ol>
 *   <li><b>不走通用渲染</b>——抽取期填 {@code state.backItem} 的那个条件必须显式排除枪。
 *       ⚠️ <b>枪是带 TOOL 组件的</b>，只写 {@code has(DataComponents.TOOL)} 会把它放进来。</li>
 *   <li><b>专用分支还在</b>——两个背部渲染层在「{@code backItem} 为空」那条路上
 *       都得真的调 {@code renderBackGun}。前者排除掉枪只是让控制流<b>落得到</b>这里，
 *       这里要是没人接，症状会从「穿模」变成「什么都不画」。</li>
 * </ol>
 *
 * <p>⚠️ 全部判定都在<b>剥掉注释之后</b>做：这两处的注释里恰好写着 {@code isGun} 与
 * {@code renderBackGun}，不剥的话把接线删干净了测试照样绿。</p>
 */
class BackDisplayGunRenderContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path PROJECT_ROOT = Path.of("..", "..");
    private static final Path MAIN_JAVA = PROJECT_ROOT.resolve("src/main/java");
    private static final Path RENDER_STATE = MAIN_JAVA.resolve(
            "com/github/tartaricacid/touhoulittlemaid/client/renderer/entity/state/EntityMaidRenderState.java");
    private static final Path CLIENT_RENDERER = MAIN_JAVA.resolve(
            "com/github/tartaricacid/touhoulittlemaid/client/renderer");

    /** 通用背部物品渲染的落笔处——谁写了这一句，谁就得对枪负责。 */
    private static final String GENERIC_BACK_ITEM_DRAW = "state.backItem.submit(";
    /** 抽取期填充 backItem 的唯一入口。 */
    private static final String BACK_ITEM_FILL = "updateForLiving(state.backItem";

    /**
     * ① 抽取期必须把枪排除在通用物品渲染之外。
     *
     * <p>判据缩到<b>被判定的那个表达式</b>（守着填充语句的那个 {@code if} 的条件），
     * 不是整份文件——否则文件里别处出现一次 {@code isGun} 就能让它假绿。</p>
     */
    @Test
    void backItemFillExcludesGuns() throws IOException {
        String source = stripComments(Files.readString(RENDER_STATE, StandardCharsets.UTF_8));

        // 活性：填充点必须恰好一处，多了少了都说明这条判据已经不在看它该看的东西
        assertEquals(1, countOccurrences(source, BACK_ITEM_FILL),
                "backItem 的填充点不是恰好一处，判据的锚点已失效：" + RENDER_STATE);

        String condition = enclosingIfCondition(source, source.indexOf(BACK_ITEM_FILL));
        assertTrue(condition.contains("showItem"),
                "认错了 if：抽出来的条件里没有 showItem，实际是 " + condition);
        assertTrue(condition.contains("DataComponents.TOOL"),
                "背部展示的 TOOL 判据不见了，实际条件是 " + condition);
        assertTrue(condition.contains("isGun"),
                "背部展示没有排除枪械——枪带 TOOL 组件，不排除就会走通用物品渲染并穿模。实际条件是 " + condition);
    }

    /**
     * ② 排除之后得有人接：画通用背部物品的每一层，都要在「backItem 为空」那条路上调枪械渲染。
     *
     * <p>枚举面按「谁画了通用背部物品」定，不按「哪个包」定——普通与 gecko 两套各一个。</p>
     */
    @Test
    void everyBackItemLayerAlsoRoutesGunsToTheDedicatedRenderer() throws IOException {
        List<Path> layers = new ArrayList<>();
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(CLIENT_RENDERER)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                if (!source.contains(GENERIC_BACK_ITEM_DRAW)) {
                    continue;
                }
                layers.add(file);
                if (!source.contains("renderBackGun(")) {
                    offenders.add(file.toString());
                }
            }
        }
        // 活性下限：识别依据一变就静默零覆盖，而零覆盖的测试永远是绿的
        assertEquals(2, layers.size(),
                "画通用背部物品的层不是两个（普通 + gecko），判据的枚举面已失效：" + layers);
        assertTrue(offenders.isEmpty(),
                "这些层画了通用背部物品却没给枪械留渲染路径，枪会变成什么都不画：" + offenders);
    }

    /**
     * 取包住 {@code fromIndex} 的那个 {@code if} 的条件表达式。
     *
     * <p>先往回找最近的 {@code if (}，再按括号配对往前截到与之匹配的右括号。
     * 手写扫描而不用正则：正则的失败模式（回溯爆炸、递归深度）是引擎相关的，
     * 本仓库栽过一次（python 原型跑得通，Java 的 Pattern 同一模式爆栈）。</p>
     */
    private static String enclosingIfCondition(String source, int fromIndex) {
        int ifIndex = source.lastIndexOf("if (", fromIndex);
        if (ifIndex < 0) {
            return "<找不到包住它的 if>";
        }
        int open = source.indexOf('(', ifIndex);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return source.substring(open + 1, i);
                }
            }
        }
        return "<括号不配对>";
    }

    private static int countOccurrences(String source, String needle) {
        int count = 0;
        for (int i = source.indexOf(needle); i >= 0; i = source.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
    }

    /** 剥掉块注释与行注释（含 javadoc）。字符串字面量里的 {@code //} 在本仓库这几个文件里不存在。 */
    private static String stripComments(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        while (i < source.length()) {
            if (source.startsWith("/*", i)) {
                int end = source.indexOf("*/", i + 2);
                i = end < 0 ? source.length() : end + 2;
            } else if (source.startsWith("//", i)) {
                int end = source.indexOf('\n', i);
                i = end < 0 ? source.length() : end;
            } else {
                out.append(source.charAt(i));
                i++;
            }
        }
        return out.toString();
    }
}
