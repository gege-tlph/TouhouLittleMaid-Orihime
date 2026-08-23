package com.github.tartaricacid.touhoulittlemaid.command.subcommand;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 技能重载必须独立于站点加载的成败。
 *
 * <p>站点读的是 {@code sites/*.json}，技能读的是 {@code skills/}，本就是两件互不相干的事。
 * 原实现写成「站点加载不完整就 return」，于是一份写坏的 {@code stt.json} 会连带让技能不重载，
 * 也不再同步给客户端——局部失败被放大成整体放弃。</p>
 *
 * <p>这条不变量没法用普通单元测试钉住（{@code reload} 需要一个真实的 MinecraftServer），
 * 故与 {@code ServerRulesSaveAuthorityContractTest} 同法，在源码层断言接线。</p>
 */
class AiReloadWiringContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path AI_CHAT_COMMAND = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "command", "subcommand", "AIChatCommand.java"));
    private static final Path CONFIG_COMMAND = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "command", "subcommand", "ConfigCommand.java"));
    private static final java.util.regex.Pattern BLOCK_COMMENT =
            java.util.regex.Pattern.compile("/\\*.*?\\*/", java.util.regex.Pattern.DOTALL);

    @Test
    void skillsReloadEvenWhenSiteLoadingIsIncomplete() throws IOException {
        String active = methodBody(activeSource(AI_CHAT_COMMAND), "public static boolean reloadAll(");
        int sites = active.indexOf("AvailableSites.init()");
        int skills = active.indexOf("SkillLoader.init()");

        assertTrue(sites >= 0, "reload 必须调用 AvailableSites.init()");
        assertTrue(skills >= 0, "reload 必须调用 SkillLoader.init()");
        assertTrue(sites < skills, "站点加载应先于技能加载，本断言依赖该顺序");

        String between = active.substring(sites, skills);
        assertTrue(!between.contains("return"),
                "站点加载与技能加载之间不得有提前 return："
                        + "一份写坏的站点文件不该连带让技能不重载，两者读的是不同目录");
    }

    @Test
    void clientsAreSynchronisedRegardlessOfSiteLoadResult() throws IOException {
        String active = methodBody(activeSource(AI_CHAT_COMMAND), "public static boolean reloadAll(");
        int skills = active.indexOf("SkillLoader.init()");
        int syncSites = active.indexOf("SyncAISitesPacket.syncToSiteEditors");

        assertTrue(syncSites > skills, "站点同步必须在技能重载之后、且同样不被站点失败跳过");
        assertTrue(!active.substring(skills, syncSites).contains("return"),
                "技能重载与站点同步之间不得有提前 return，否则调用方需要自己补发同步");
    }

    /**
     * §17 v2 后的边界：**{@code /tlm ai_chat} 管实例级 AI 的一切（含 AI 规则），
     * {@code /tlm config} 管存档级玩法规则，且不认识任何 AI 符号。**
     *
     * <p>「拆干净」这种一次性动作最容易在几个月后被一行「顺手也刷一下」悄悄缝回去。
     * 故把它钉成断言：**不是防今天写错，是防明天缝回来。**</p>
     */
    @Test
    void theTwoReloadCommandsDoNotReachIntoEachOther() throws IOException {
        String ai = activeSource(AI_CHAT_COMMAND);
        String config = activeSource(CONFIG_COMMAND);

        // AI 命令重载的是自己的实例级规则店，不许伸手去转存档级世界规则
        assertTrue(!java.util.regex.Pattern.compile("(?<!Ai)ServerRuleConfig\\.reloadFromDisk").matcher(ai).find(),
                "AI 重载不得重载世界规则（AiServerRuleConfig.reloadFromDisk 才是它自己的店）");
        assertTrue(!config.contains("AIChatCommand"),
                "/tlm config reload 不得调用 AI 侧重载：AI 有自己的入口 /tlm ai_chat reload");
        assertTrue(!config.contains("AvailableSites") && !config.contains("SkillLoader"),
                "/tlm config reload 不得直接重载站点或技能，绕开命令层同样算耦合");
        // §17 v2 负向锁：世界规则命令不认识任何 AI 符号——AI 规则已整体搬去 AiServerRuleConfig
        assertTrue(!config.contains("DefaultAiSnapshot") && !config.contains("AiServerRuleConfig")
                        && !config.contains("AIConfig"),
                "/tlm config reload 与 AI 必须零耦合：AI 规则住实例级，归 /tlm ai_chat reload");

        // 站点保存路径走的就是 reloadSites。它原先调的是连技能一起重载的那个大 reload，
        // 于是保存一个站点会重扫整个 skills/ 目录。拆分把这层浪费一并去掉，这里钉住它别回来。
        String sitesOnly = methodBody(ai, "public static boolean reloadSites(");
        assertTrue(!sitesOnly.contains("SkillLoader"),
                "reloadSites 不得重载技能：站点保存每次都会走它，捎带重扫 skills/ 是纯浪费");
    }

    /**
     * {@code /tlm ai_chat reload} 收拢为单命令后：规则重载失败**不得放大**——
     * 站点与技能必须照常重载（与「坏一个站点文件不连坐技能」同一条纪律）。
     */
    @Test
    void aBrokenRulesFileDoesNotSkipSitesAndSkills() throws IOException {
        String body = methodBody(activeSource(AI_CHAT_COMMAND), "private static int reloadEverything(");
        int rules = body.indexOf("AiServerRuleConfig.reloadFromDisk");
        int sites = body.indexOf("reloadAll(server)");
        assertTrue(rules >= 0, "reload 必须重载实例级 AI 规则（AiServerRuleConfig.reloadFromDisk）");
        assertTrue(sites > rules, "规则重载应先于站点/技能，本断言依赖该顺序");
        assertTrue(!body.substring(rules, sites).contains("return"),
                "规则重载失败与站点/技能重载之间不得有提前 return：局部失败不许放大成整体放弃");
    }

    /**
     * 上面两条只防住了 {@code return} 这一种逃逸方式，而抛异常是它们盖不到的另一种。
     *
     * <p>{@code SiteConfigStorage} 的读取是严格版本，文件损坏且 {@code .last-good} 不可用时抛
     * {@link IllegalStateException}。三个 {@code Save*SitePacket} 都各自 catch 了它，唯独
     * {@code syncToSiteEditors} 没有——于是一份坏文件能让整条 reload 在中途逃逸，
     * 世界规则同步与管理员的失败回执一起消失。触发条件与当初那个 P0 完全重合。</p>
     */
    @Test
    void aDamagedSiteFileCannotEscapeReloadByThrowing() throws IOException {
        String body = syncToSiteEditorsBody();
        int guard = body.indexOf("try {");
        int read = body.indexOf("SiteConfigStorage.read");
        int caught = body.indexOf("catch (IllegalStateException");

        assertTrue(read >= 0, "syncToSiteEditors 必须读取站点文件，否则本断言的前提已变");
        assertTrue(guard >= 0 && guard < read,
                "严格读取必须处在 try 之内：它在文件损坏时会抛，而调用方还要继续同步世界规则");
        assertTrue(caught > read,
                "必须接住 IllegalStateException：三个 Save*SitePacket 都接了，这里漏接会让整条 reload 逃逸");
    }

    private static String syncToSiteEditorsBody() throws IOException {
        Path packet = ROOT.resolve(Path.of("src", "main", "java", "com", "github", "tartaricacid",
                "touhoulittlemaid", "network", "message", "ai", "SyncAISitesPacket.java"));
        String active = activeSource(packet);
        int start = active.indexOf("public static void syncToSiteEditors");
        assertTrue(start >= 0, "找不到 syncToSiteEditors，接线已变，本用例需要同步更新");
        int end = active.indexOf("private static", start);
        return end > start ? active.substring(start, end) : active.substring(start);
    }

    /**
     * 默认值变更告知必须覆盖**两条**生效路径：界面保存即激活（任何服务器形态，§17 v2 D2），
     * 与手改文件后的 {@code /tlm ai_chat reload}。
     *
     * <p>只挂一条的后果是「配置已生效但玩家完全不知情」——女仆悄悄换了模型，玩家只觉得她变笨了。
     * 这类一次性接线最容易在几个月后被一条「顺手简化」拆掉半边，故钉成断言。
     * （曾经的第二条路是 ConfigCommand——AI 规则搬去实例级后，那条路与 AI 零耦合。）</p>
     */
    @Test
    void defaultChangeNotificationCoversBothActivationPaths() throws IOException {
        Path savePacket = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
                "tartaricacid", "touhoulittlemaid", "network", "message", "config", "SaveServerRulesPacket.java"));
        String save = activeSource(savePacket);
        assertTrue(save.contains("DefaultAiSnapshot.capture") && save.contains("diffAndNotify"),
                "保存即激活路径必须做默认值变更告知（capture + diffAndNotify）");
        String reload = methodBody(activeSource(AI_CHAT_COMMAND), "private static int reloadEverything(");
        assertTrue(reload.contains("DefaultAiSnapshot.capture") && reload.contains("diffAndNotify"),
                "ai_chat reload 路径必须做同样的告知——缺这半边就是设计点名要消灭的半成品");
        assertTrue(reload.contains("SyncServerRulesPacket.syncToAll"),
                "规则重载成功后必须把合流快照广播给全体客户端，否则界面还显示旧默认");
    }

    /**
     * {@code /tlm ai_chat} 与 {@code /tlm config} 的权限门必须一致。
     *
     * <p>2026-07-28 实测同款缺口：`ai_chat` 只给 GAME_MASTER，而**界面**按 {@code canEditSite}
     * 把完整 AI 管理权给了关作弊的单人档房主——房主在屏上什么都能改，却跑不了自己的 reload。
     * 「界面能改、命令不能」这种半开门，钉死在这里。</p>
     */
    @Test
    void theAiCommandGateMatchesTheConfigCommandGate() throws IOException {
        Path rootCommand = ROOT.resolve(Path.of("src", "main", "java", "com", "github", "tartaricacid",
                "touhoulittlemaid", "command", "RootCommand.java"));
        String active = activeSource(rootCommand);
        int ai = active.indexOf("AIChatCommand.get()");
        int config = active.indexOf("ConfigCommand.get()");
        assertTrue(ai >= 0 && config >= 0, "两条命令都必须在 RootCommand 里登记");

        String aiGate = active.substring(ai, config > ai ? config : active.length());
        assertTrue(aiGate.contains("isSingleplayerOwner"),
                "/tlm ai_chat 必须同时放行单人档房主：界面已按 canEditSite 给了他完整 AI 管理权，"
                        + "命令层再卡 GAME_MASTER 就是「能改不能重载」的半开门");
    }

    /**
     * 只保留真正会执行的源码：行注释与块注释（含 javadoc）全部剥掉。
     *
     * <p><b>原实现只剥 {@code //}。</b>那意味着 javadoc 里写一句 {@code AvailableSites.init()}
     * 就会被 {@code indexOf} 命中，断言可能因为一句注释而通过——**一个只在注释里存在的接线会被判为存在**。
     * 2026-07-27 拆分 reload 命令时正好往这些方法上加了解释性 javadoc，把这个坑踩实了。</p>
     */
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

    /**
     * 取出某个方法的方法体，按大括号配对截取。
     *
     * <p><b>为什么不能再对整份文件用 {@code indexOf}</b>：拆分前 AI 侧只有一个 reload 方法，
     * 全文件搜索等价于搜那个方法。拆成 {@code reloadAll} / {@code reloadSites} / {@code reloadSkillsOnly}
     * 之后，{@code AvailableSites.init()} 与 {@code SkillLoader.init()} 各有多处，
     * 全文件 {@code indexOf} 找到的是**恰好排在前面的那个方法**里的调用，断言的先后关系随之失去意义。</p>
     */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "源码里找不到方法：" + signature);
        int brace = source.indexOf('{', start);
        assertTrue(brace >= 0, "方法没有方法体：" + signature);
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
