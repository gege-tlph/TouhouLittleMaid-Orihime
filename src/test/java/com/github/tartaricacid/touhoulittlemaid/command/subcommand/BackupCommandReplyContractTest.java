package com.github.tartaricacid.touhoulittlemaid.command.subcommand;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code /tlm backup} 的每一条回执都必须到得了命令源。
 *
 * <p><b>2026-08-30 专服实测</b>：管理员在 MCDR 控制台跑 {@code /tlm backup get <玩家>}，
 * 一个字都没有，报成「命令失效」。查下来命令其实**执行成功了**——这个子命令的八条输出
 * 全部走 {@code player.displayClientMessage(...)}，发给的是 {@code EntityArgument} 选中的
 * 那个玩家，一条也不发给命令源。控制台不是玩家，于是它永远安静。</p>
 *
 * <p>这个坑之所以看不出来，是因为同一棵命令树里两种写法并存：{@code /tlm ai_chat status}
 * 用的是 {@code sendSuccess}，本来就回命令源。所以「命令回不回话」不是命令树的属性，
 * 是**每个子命令各自的写法**——它随时会被下一个人照着 backup 抄回去。</p>
 *
 * <p>断言按处理方法枚举，并自带「找到了几个看守对象」的下限：识别依据一旦失效
 * （方法改名、回执改走别的 helper），零覆盖的测试永远是绿的。</p>
 */
class BackupCommandReplyContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path BACKUP_COMMAND = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "command", "subcommand", "BackupCommand.java"));
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /** 三个 executes 处理方法。它们是玩家真正会敲到的全部档位。 */
    private static final String[] HANDLERS = {
            "private static int handlePlayerMaidIndex(",
            "private static int handlePlayerMaid(",
            "private static int handlePlayerMaidFile("
    };

    @Test
    void everyHandlerRepliesThroughTheSourceAwareHelper() throws IOException {
        String active = activeSource(BACKUP_COMMAND);

        int checked = 0;
        for (String handler : HANDLERS) {
            String body = methodBody(active, handler);
            assertTrue(!body.contains("displayClientMessage"),
                    handler + " 里不得直接 displayClientMessage：那只发给被选中的玩家，"
                            + "从控制台跑就是一片安静，看起来像命令失效");
            assertTrue(body.contains("reply(context, player,"),
                    handler + " 至少要有一条经 reply(...) 的回执，否则这一档跑完毫无反馈");
            checked++;
        }
        assertTrue(checked == HANDLERS.length,
                "本用例按方法名识别处理方法，只找到 " + checked + " 个——识别依据已失效，等于零覆盖");
    }

    /**
     * 光有 helper 不够，还要断言 helper 真的发给了命令源。
     * 只查「不直接 displayClientMessage」的话，把 reply 写成只发目标玩家照样全绿。
     */
    @Test
    void theHelperReachesTheCommandSource() throws IOException {
        String body = methodBody(activeSource(BACKUP_COMMAND), "private static void reply(");
        assertTrue(body.contains("context.getSource().sendSuccess"),
                "reply 必须经 sendSuccess 发给命令源——这正是控制台看不到回执的那一半");
        assertTrue(body.contains("getEntity() != player"),
                "源与目标是同一个玩家时不得重复发送：sendSuccess 已经送到他自己的聊天栏了");
    }

    /**
     * 可点击行里不得写 {@code @s}。
     *
     * <p>回执现在也发给命令源，于是同一条 RunCommand 会出现在**两个人**的聊天栏里，
     * 而 {@code @s} 在点击者身上求值——管理员点一下就跳去查自己的备份了。
     * 这是「把消息给了新的收件人」这件事必然带出的尾巴，不是原来就有的缺陷。</p>
     */
    @Test
    void clickableRowsNameTheTargetInsteadOfSelf() throws IOException {
        String active = activeSource(BACKUP_COMMAND);
        assertTrue(!active.contains("/tlm backup get @s"),
                "可点击行不得用 @s：它在点击者身上求值，而这些行现在也会出现在命令源的聊天栏里");
        assertTrue(active.contains("player.getScoreboardName()"),
                "可点击行必须写死目标玩家的名字");
    }

    /** 只保留会执行的源码：行注释与块注释（含 javadoc）全部剥掉。 */
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

    /** 按大括号配对截取方法体：断言范围必须缩到那个方法，不能对整份文件 indexOf。 */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "找不到 " + signature + "，接线已变，本用例需要同步更新");
        int open = source.indexOf('{', start);
        assertTrue(open >= 0, signature + " 后面没有方法体");
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
        throw new AssertionError(signature + " 的大括号不配对");
    }
}
