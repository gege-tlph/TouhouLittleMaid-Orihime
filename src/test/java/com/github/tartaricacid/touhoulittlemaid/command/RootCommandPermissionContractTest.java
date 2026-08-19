package com.github.tartaricacid.touhoulittlemaid.command;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 命令树的每一支都必须自带权限闸。
 *
 * <p><b>为什么这条必须机械化</b>：上游只在根上写一次权限判定，子命令全部裸挂——
 * 在上游没问题，因为它的根闸永远是管理员。可我们给根开了一条口子
 * （单人档房主不开作弊也能重载 AI 与世界规则，与界面权限保持一致，见 H1），
 * 于是「根放行 = 全部放行」会把发包、给点数、调试命令一并交给房主。
 * <b>逐支挂闸是那条口子能够成立的前提</b>，两者是一件事的两半，
 * 而它们写在同一个方法里、相隔几行——最容易被后来的人当成重复代码「顺手简化」掉。</p>
 *
 * <p>四树对照实证过这一维就是本节差异化的指纹：上游与代码宿主的 {@code .requires} 各 <b>1</b> 处
 * （只有根），行为基准与本树各 <b>10</b> 处。数量一差就说明有支命令在裸挂。</p>
 *
 * <p>⚠️ 判据落在<b>方法体</b>内而不是整份文件：断言「每个 X 都跟着 Y」这类相邻关系时，
 * 范围不缩到那个方法体，文件里别处的同名调用会让断言失去意义。</p>
 */
class RootCommandPermissionContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path ROOT_COMMAND = Path.of("..", "..", "src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "command", "RootCommand.java");

    @Test
    void everySubcommandCarriesItsOwnPermissionGate() throws IOException {
        String body = registerBody();

        List<String> naked = new ArrayList<>();
        int branches = 0;
        int at = 0;
        while (true) {
            at = body.indexOf("root.then(", at);
            if (at < 0) {
                break;
            }
            branches++;
            String argument = parenGroupAt(body, body.indexOf('(', at));
            if (!argument.contains(".requires(")) {
                naked.add(argument.replaceAll("\\s+", " "));
            }
            at += "root.then(".length();
        }

        // 活性断言：与结论正交。「没有裸挂的支」与「一支都没认出来」在结果上完全一样，
        // 所以活性要用独立于结论的量——认出了几支命令。
        assertTrue(branches >= 7,
                "只认出 " + branches + " 支子命令，识别依据（root.then(…)）可能已失效");
        assertTrue(naked.isEmpty(),
                "这些子命令没有自己的权限闸，会直接继承根上那道放宽过的闸：" + naked);
    }

    /**
     * 根上那道闸必须放行单人档房主——它与界面侧的判定是同一件事的两个入口，
     * 只放行其中一个会造出「界面能改、命令说没权限」的半开门。
     */
    @Test
    void theRootGateAlsoAdmitsTheSingleplayerHost() throws IOException {
        String body = registerBody();
        assertTrue(body.contains("isSingleplayerOwner"),
                "根上的闸不认单人档房主：界面里房主本就能改这些东西，命令层再卡一道就是半开门");
    }

    /** 取 {@code register} 的方法体——大括号配对，别用整份文件。 */
    private static String registerBody() throws IOException {
        String source = stripComments(Files.readString(ROOT_COMMAND, StandardCharsets.UTF_8));
        int at = source.indexOf("public static void register(");
        assertTrue(at > 0, "找不到 RootCommand.register，判据的识别依据已失效");
        return braceBlockAt(source, source.indexOf('{', at));
    }

    /** 取一对圆括号里的内容（含嵌套），用于把 {@code then(...)} 的实参整段截出来。 */
    private static String parenGroupAt(String source, int open) {
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        return source.substring(open);
    }

    private static String braceBlockAt(String source, int open) {
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
        return source.substring(open);
    }

    /** 去掉注释——否则只在 javadoc 里出现的 {@code .requires} 会被判为「存在」。 */
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }
}
