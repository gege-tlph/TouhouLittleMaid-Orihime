package com.github.tartaricacid.touhoulittlemaid.command.subcommand;

import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.network.message.config.SyncServerRulesPacket;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

public final class ConfigCommand {
    private ConfigCommand() {
    }

    /**
     * 只重载**存档级玩法规则**，与 AI 零瓜葛。
     *
     * <p>§17 v2（用户 2026-07-28 定案）把 AI 规则整体搬去了实例级的
     * {@code AiServerRuleConfig}，与站点/技能同作用域，重载入口是 {@code /tlm ai_chat reload}。
     * 本命令从此不认识任何 AI 符号——这条边界由 {@code AiReloadWiringContractTest}
     * **负向**钉死（断言本文件不得引用 AI 侧类名），不是防今天写错，是防明天缝回来。</p>
     */
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
