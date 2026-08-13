package com.github.tartaricacid.touhoulittlemaid.command.subcommand;

import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.network.message.config.SyncServerRulesPacket;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * {@code /tlm config reload}：重读本存档的世界规则文件并广播。
 *
 * <p>专服上的两条改动路径都汇到这里：管理员在配置菜单里保存（只写文件），
 * 或者直接手改 {@code serverconfig/touhou_little_maid-server.toml}。
 * 两条都需要一次显式重载才激活——运行期值的切换会影响所有在线玩家，不该由一次点击静默完成。</p>
 *
 * <p>只重载**存档级玩法规则**，与 AI 无关：AI 规则在行为基准上是实例级的另一店，
 * 重载入口是 {@code /tlm ai_chat reload}。这条边界要在 §3.C 那一刀落地时保持住，别缝回来。</p>
 */
public final class ConfigCommand {
    private ConfigCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("config")
                .then(Commands.literal("reload").executes(context -> {
                    MinecraftServer server = context.getSource().getServer();
                    if (!ServerRuleConfig.reloadFromDisk()) {
                        context.getSource().sendFailure(Component.translatable(
                                "commands.touhou_little_maid.config.reload_failed"));
                        return 0;
                    }
                    SyncServerRulesPacket.syncToAll(server);
                    context.getSource().sendSuccess(
                            () -> Component.translatable("commands.touhou_little_maid.config.reload_success"), true);
                    return Command.SINGLE_SUCCESS;
                }));
    }
}
