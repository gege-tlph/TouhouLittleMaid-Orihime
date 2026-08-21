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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 背部展示格里的枪械：<b>只在模型作者给了挂点骨骼时才画，没有骨骼就什么都不画</b>
 * （用户 2026-08-21 裁决）。
 *
 * <p><b>这是接线判据（🟡），不是行为判据</b>：GameTest 是纯服务端，客户端渲染在那里不加载，
 * 「背上那把枪看起来对不对」只有实机能验。明写这一点是为了不让「测试通过」被读成「渲染正确」。</p>
 *
 * <p>⚠️ 本文件 2026-08-21 整体重写过一次，原因值得留着：上一版断言「填充 backItem 的条件
 * 必须显式排除枪」，而那条断言建立在一个<b>假事实</b>上——「TACZ 的枪带 TOOL 组件」。
 * 运行期实测（{@code tacz:modern_kinetic_gun} 共 12 个组件，{@code minecraft:tool} 不在其中；
 * 对照组 {@code minecraft:diamond_pickaxe} 有）证伪了它：枪从来就走不到通用物品渲染，
 * 那条排除是<b>纯空操作</b>，而红测只能证明「判据抓得住它断言的那件事」，
 * 证明不了「它断言的是对的那件事」。</p>
 *
 * <p>现在的契约有<b>三个动词</b>，各配一条用例：</p>
 * <ol>
 *   <li><b>通用渲染只收带 TOOL 的东西</b>——抽取期填 {@code state.backItem} 的那个条件
 *       必须仍然只由 {@code DataComponents.TOOL} 把关（放宽了枪就会掉进通用渲染），
 *       且不许再加那条空操作的 {@code isGun} 排除。</li>
 *   <li><b>只有 gecko 那一层接枪</b>——画通用背部物品的两层里，只有 gecko 层调
 *       {@code renderBackGun}；bedrock 层不许再有兜底调用。</li>
 *   <li><b>枪械渲染器只按挂点骨骼画</b>——{@code GunMaidRender} 必须仍按
 *       {@code TAC_PISTOL} / {@code TAC_RIFLE} 定位组渲染，且不许再出现那条
 *       固定变换兜底的特征（{@code scale(0.6f)} / {@code ZP -35} / 背包位移）。</li>
 * </ol>
 *
 * <p>⚠️ 全部判定都在<b>剥掉注释之后</b>做：这几处的注释里恰好写着 {@code isGun}、
 * {@code renderBackGun}、{@code scale(0.6f)}，不剥的话把接线删干净了测试照样绿。</p>
 */
class BackDisplayGunRenderContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path PROJECT_ROOT = Path.of("..", "..");
    private static final Path MAIN_JAVA = PROJECT_ROOT.resolve("src/main/java");
    private static final Path RENDER_STATE = MAIN_JAVA.resolve(
            "com/github/tartaricacid/touhoulittlemaid/client/renderer/entity/state/EntityMaidRenderState.java");
    private static final Path CLIENT_RENDERER = MAIN_JAVA.resolve(
            "com/github/tartaricacid/touhoulittlemaid/client/renderer");
    private static final Path GUN_MAID_RENDER = MAIN_JAVA.resolve(
            "com/github/tartaricacid/touhoulittlemaid/compat/gun/tacz/client/GunMaidRender.java");

    /** 通用背部物品渲染的落笔处——谁写了这一句，谁就是一个「背部物品层」。 */
    private static final String GENERIC_BACK_ITEM_DRAW = "state.backItem.submit(";
    /** 抽取期填充 backItem 的唯一入口。 */
    private static final String BACK_ITEM_FILL = "updateForLiving(state.backItem";

    /**
     * ① 通用背部物品渲染的准入判据只能是 TOOL，且不许再加空操作的枪械排除。
     *
     * <p>判据缩到<b>被判定的那个表达式</b>（守着填充语句的那个 {@code if} 的条件），
     * 不是整份文件——否则文件里别处出现一次 {@code isGun} 就能让它假绿。</p>
     */
    @Test
    void backItemFillIsGatedOnToolAlone() throws IOException {
        String source = stripComments(Files.readString(RENDER_STATE, StandardCharsets.UTF_8));

        // 活性：填充点必须恰好一处，多了少了都说明这条判据已经不在看它该看的东西
        assertEquals(1, countOccurrences(source, BACK_ITEM_FILL),
                "backItem 的填充点不是恰好一处，判据的锚点已失效：" + RENDER_STATE);

        String condition = enclosingIfCondition(source, source.indexOf(BACK_ITEM_FILL));
        assertTrue(condition.contains("showItem"),
                "认错了 if：抽出来的条件里没有 showItem，实际是 " + condition);
        assertTrue(condition.contains("DataComponents.TOOL"),
                "背部展示的 TOOL 判据不见了——放宽了它，枪就会掉进通用物品渲染。实际条件是 " + condition);
        assertFalse(condition.contains("isGun"),
                "又加回了 isGun 排除。TACZ 的枪不带 TOOL（2026-08-21 运行期实测），"
                        + "这条排除永远不会成立，是纯空操作。实际条件是 " + condition);
    }

    /**
     * ② 画通用背部物品的两层里，只有 gecko 那层把枪交给枪械渲染器。
     *
     * <p>bedrock 模型没有枪械挂点，上游在那里的兜底是固定变换，实机表现为枪甩到身侧、
     * 穿进模型里；用户 2026-08-21 裁决砍掉。枚举面按「谁画了通用背部物品」定，不按包定。</p>
     */
    @Test
    void onlyTheGeckoLayerRoutesGunsToTheDedicatedRenderer() throws IOException {
        List<Path> layers = new ArrayList<>();
        List<String> routing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(CLIENT_RENDERER)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                if (!source.contains(GENERIC_BACK_ITEM_DRAW)) {
                    continue;
                }
                layers.add(file);
                if (source.contains("renderBackGun(")) {
                    routing.add(file.toString());
                }
            }
        }
        // 活性下限：识别依据一变就静默零覆盖，而零覆盖的测试永远是绿的
        assertEquals(2, layers.size(),
                "画通用背部物品的层不是两个（bedrock + gecko），判据的枚举面已失效：" + layers);
        assertEquals(1, routing.size(),
                "接枪械渲染的背部物品层不是恰好一个。多了说明 bedrock 那条固定变换兜底被加了回来，"
                        + "少了说明 gecko 模型的枪彻底不画了。实际：" + routing);
        assertTrue(routing.getFirst().contains("gecko"),
                "接枪械渲染的不是 gecko 那一层——只有它拿得到 TAC_PISTOL / TAC_RIFLE 定位组。实际：" + routing);
    }

    /**
     * ③ 枪械渲染器只按挂点骨骼画，不许有固定变换的兜底。
     *
     * <p>「没有骨骼就什么都不画」这条，用户 2026-08-20 已对 gecko 分支裁决过一次，
     * 2026-08-21 扩到全部路径：挂点归模型作者定，没给挂点就是不想让枪挂在那儿。</p>
     */
    @Test
    void backGunRendererDrawsOnlyAtLocatorBones() throws IOException {
        String source = stripComments(Files.readString(GUN_MAID_RENDER, StandardCharsets.UTF_8));

        // 活性：被看管的那两条挂点分支必须都在，否则下面的「不许出现」全是空转
        assertTrue(source.contains("GeoLocatorType.TAC_PISTOL"),
                "手枪挂点分支不见了：" + GUN_MAID_RENDER);
        assertTrue(source.contains("GeoLocatorType.TAC_RIFLE"),
                "长枪挂点分支不见了：" + GUN_MAID_RENDER);

        assertFalse(source.contains("scale(0.6f"),
                "固定变换兜底的缩放又出现了——那条路不看任何挂点骨骼，实机表现是枪穿进女仆身体。");
        assertFalse(source.contains("rotationDegrees(-35"),
                "固定变换兜底的 ZP -35 又出现了——见本文件类注释与 GunMaidRender 类注释。");
        assertFalse(source.contains("offsetBackpackItem"),
                "固定变换兜底的背包位移又出现了：枪的落点不该由背包型决定，只该由挂点骨骼决定。");
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
