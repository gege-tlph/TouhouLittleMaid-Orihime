package com.github.tartaricacid.touhoulittlemaid.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「哪个名字显示给玩家」这条契约，在<b>两个</b>取名点上必须一致。
 *
 * <p>照片 / 手办上渲染出来的是 YSM 模型，标题就该是那个模型的名字；拿本体模型名当标题会与
 * 玩家看到的图对不上。这条规则有两个独立实现——提示框（{@code ClientMaidTooltip}）与手办
 * 物品名（{@code ItemGarageKitProxy}）——**改一个漏一个不会有任何报错**，只会让两处显示不一致。
 * 本仓库栽过多次的正是这种「全称契约只写在注释里」，故按符号枚举成断言。</p>
 *
 * <p>第三条断言钉的是一处<b>有意的跨分支差异</b>：{@code port/1.21.11-fabric} 把 YSM 名字存成
 * JSON 序列化的 {@code Component}，读的时候要解析；<b>本树存的是纯字符串</b>
 * （{@code YsmMaidModelPackage.displayName}）。照抄那边的解析会把好好的名字解析失败、
 * 回落成模型 id。这条断言防的就是将来有人「照基准修一下」。</p>
 */
class YsmTooltipNamePriorityContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path SRC = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid"));
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /** 两个取名点：文件 → 方法签名 → 该方法里「回落到本体模型名」的标志。 */
    private static final List<String[]> NAME_SITES = List.of(
            new String[]{"client/tooltip/ClientMaidTooltip.java",
                    "private MutableComponent getName(", "ParseI18n.getI18nKey"},
            new String[]{"client/proxy/ItemGarageKitProxy.java",
                    "public static Component getName(", "ParseI18n.parse"});

    @Test
    void bothNameSitesPreferTheYsmModelName() throws IOException {
        int checked = 0;
        for (String[] site : NAME_SITES) {
            Path file = SRC.resolve(Path.of(site[0].replace('/', java.io.File.separatorChar)));
            String body = methodBody(activeSource(file), site[1], file);

            assertTrue(body.contains("isYsmModel()"),
                    site[0] + " 的取名点没有先问 YSM，标题会与图里渲染的模型对不上");
            int ysm = body.indexOf("isYsmModel()");
            int fallback = body.indexOf(site[2]);
            assertTrue(fallback >= 0, site[0] + " 里找不到回落分支标志 " + site[2] + "（改了就同步更新本测试）");
            assertTrue(ysm < fallback,
                    site[0] + " 的 YSM 分支排在本体模型名之后，等于永远轮不到它");
            checked++;
        }
        // 下限断言：识别依据（文件名 / 方法签名）一旦失效，上面的循环会一条都不跑而依然"通过"。
        assertEquals(NAME_SITES.size(), checked, "取名点没有全部被检查到，判据已失效");
    }

    @Test
    void theYsmNameIsReadAsPlainTextNotJson() throws IOException {
        for (String[] site : NAME_SITES) {
            Path file = SRC.resolve(Path.of(site[0].replace('/', java.io.File.separatorChar)));
            String body = methodBody(activeSource(file), site[1], file);

            assertFalse(body.contains("JsonParser") || body.contains("ComponentSerialization"),
                    site[0] + " 在解析 YSM 名字的 JSON——本树存的是纯字符串，"
                            + "解析必然失败并回落成模型 id。那是 1.21.11 的形态，别照抄");
        }
    }

    private static String activeSource(Path path) throws IOException {
        StringBuilder active = new StringBuilder();
        for (String line : Files.readAllLines(path)) {
            if (line.trim().startsWith("//")) {
                continue;
            }
            active.append(line).append('\n');
        }
        return BLOCK_COMMENT.matcher(active.toString()).replaceAll(" ");
    }

    private static String methodBody(String source, String signature, Path path) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, path.getFileName() + " 里找不到方法：" + signature + "（改名了就同步更新本测试）");
        int brace = source.indexOf('{', start);
        int depth = 0;
        for (int i = brace; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(brace, i + 1);
                }
            }
        }
        throw new IllegalStateException("方法体大括号不配对：" + signature);
    }
}
