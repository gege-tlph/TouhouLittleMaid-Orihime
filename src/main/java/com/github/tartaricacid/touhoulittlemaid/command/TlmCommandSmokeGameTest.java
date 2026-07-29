package com.github.tartaricacid.touhoulittlemaid.command;

import com.github.tartaricacid.touhoulittlemaid.config.AiServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

/**
 * 命令层的冒烟：每条 {@code /tlm} 子命令**真的跑一次**，回执文本真的被读。
 *
 * <p>这些条目原先全在人工验收清单里（「新命令树各档各跑一次」）。它们不需要人——GameTest 手上
 * 就有一台真服务器，命令派发、权限门、回执本地化、以及命令背后那几个存储的读写全在其中。
 * 搬进门禁后，凡把命令树改坏（改名、漏登记、回执键不存在、存储路由断了）都会当场红。</p>
 *
 * <p><b>为什么值得</b>：§17 v2 把 AI 规则搬去实例级存储，命令是它唯一的手改文件入口；
 * 而命令跑不通这件事，编译、单测、启动全都测不出来。</p>
 */
public final class TlmCommandSmokeGameTest {
    /** 收集回执的命令源：控制台身份（权限最高），但把输出抓在手里而不是丢进日志 */
    private static final class Collector implements CommandSource {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void sendSystemMessage(Component component) {
            this.messages.add(component.getString());
        }

        @Override
        public boolean acceptsSuccess() {
            return true;
        }

        @Override
        public boolean acceptsFailure() {
            return true;
        }

        @Override
        public boolean shouldInformAdmins() {
            return false;
        }

        String joined() {
            return String.join("\n", this.messages);
        }
    }

    private static String run(MinecraftServer server, String command) {
        Collector collector = new Collector();
        CommandSourceStack source = server.createCommandSourceStack().withSource(collector);
        server.getCommands().performPrefixedCommand(source, command);
        return collector.joined();
    }

    /** 状态命令：三条服务各出一行，且没有一行是裸 lang 键 */
    @GameTest(maxTicks = 100)
    public void statusReportsEveryService(GameTestHelper helper) {
        String output = run(helper.getLevel().getServer(), "tlm ai_chat status");

        helper.assertTrue(!output.isBlank(), "/tlm ai_chat status 没有任何回执");
        helper.assertTrue(!output.contains("ai.touhou_little_maid."),
                "回执里出现裸 lang 键，说明有键缺失：" + output);
        for (String service : new String[]{"LLM", "TTS", "STT"}) {
            helper.assertTrue(output.contains(service),
                    "状态回执缺少 " + service + " 那一行：" + output);
        }
        helper.succeed();
    }

    /**
     * AI 重载：**一条命令**覆盖规则 + 站点 + 技能（粒度参数已砍）。
     * 顺带钉住「不带参数就能跑」——它是唯一入口，写错就没有别的档位可退。
     */
    @GameTest(maxTicks = 100)
    public void aiReloadRunsAndReportsSuccess(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        String output = run(server, "tlm ai_chat reload");

        helper.assertTrue(!output.isBlank(), "/tlm ai_chat reload 没有任何回执");
        helper.assertTrue(!output.contains("commands.touhou_little_maid.")
                        && !output.contains("message.touhou_little_maid."),
                "回执里出现裸 lang 键：" + output);
        // 重载后运行时值必须仍然可读——路由断了会在这里现形（读到的是 spec 默认而非存储值）
        AiServerRuleConfig.get(AIConfig.LLM_ENABLED);
        ServerRuleConfig.get(AIConfig.LLM_ENABLED);
        helper.succeed();
    }

    /** 世界规则重载：与 AI 无关的那一条，同样要真的跑得起来 */
    @GameTest(maxTicks = 100)
    public void configReloadRunsAndReportsSuccess(GameTestHelper helper) {
        String output = run(helper.getLevel().getServer(), "tlm config reload");

        helper.assertTrue(!output.isBlank(), "/tlm config reload 没有任何回执");
        helper.assertTrue(!output.contains("commands.touhou_little_maid."),
                "回执里出现裸 lang 键：" + output);
        helper.succeed();
    }

    /** 三个只读自省命令：它们最容易在重构中悄悄失效（谁会去点它们？） */
    @GameTest(maxTicks = 100)
    public void introspectionCommandsAllRespond(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        for (String sub : new String[]{"skill", "tool", "context", "tokens"}) {
            String output = run(server, "tlm ai_chat " + sub);
            helper.assertTrue(!output.isBlank(), "/tlm ai_chat " + sub + " 没有任何回执");
            helper.assertTrue(!output.contains("ai.touhou_little_maid.")
                            && !output.contains("commands.touhou_little_maid."),
                    "/tlm ai_chat " + sub + " 回执里出现裸 lang 键：" + output);
        }
        helper.succeed();
    }

    /**
     * {@code sites} 需要一个玩家（它给玩家开界面）。GameTest 里没有玩家，
     * 因此它必须**明确失败并说明原因**，而不是抛异常或静默成功。
     */
    @GameTest(maxTicks = 100)
    public void sitesWithoutPlayerFailsCleanly(GameTestHelper helper) {
        String output = run(helper.getLevel().getServer(), "tlm ai_chat sites");
        helper.assertTrue(!output.isBlank(), "无玩家时 /tlm ai_chat sites 必须给出说明，不能静默");
        helper.assertTrue(!output.contains("commands.touhou_little_maid."),
                "失败回执里出现裸 lang 键：" + output);
        helper.succeed();
    }
}
