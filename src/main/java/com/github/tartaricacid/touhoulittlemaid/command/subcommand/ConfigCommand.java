package com.github.tartaricacid.touhoulittlemaid.command.subcommand;

import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.network.message.config.SyncServerRulesPacket;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public final class ConfigCommand {
    private ConfigCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return Commands.literal("config")
                .then(Commands.literal("reload")
                        .then(Commands.literal("world").executes(context -> {
                            if (!ServerRuleConfig.reloadFromDisk()) {
                                context.getSource().sendFailure(
                                        Component.translatable("commands.touhou_little_maid.config.reload_failed"));
                                return 0;
                            }
                            SyncServerRulesPacket.syncToAll(context.getSource().getServer());
                            context.getSource().sendSuccess(
                                    () -> Component.translatable("commands.touhou_little_maid.config.reload_success"), true);
                            return Command.SINGLE_SUCCESS;
                        })));
    }
}
