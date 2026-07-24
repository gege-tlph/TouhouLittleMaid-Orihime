package com.github.tartaricacid.touhoulittlemaid.command;

import com.github.tartaricacid.touhoulittlemaid.command.subcommand.*;
import com.github.tartaricacid.touhoulittlemaid.debug.command.MaidDebugCommand;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.function.Predicate;

public final class RootCommand {
    private static final String ROOT_NAME = "tlm";
    private static final Predicate<CommandSourceStack> GAME_MASTER =
            Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT_NAME)
                // 1.21.11: CommandSourceStack.hasPermission(int) 移除 → Commands.hasPermission(PermissionCheck)（LEVEL_GAMEMASTERS = 旧权限等级 2）
                .requires(source -> GAME_MASTER.test(source) || isSingleplayerOwner(source));
        root.then(PackCommand.get().requires(GAME_MASTER));
        root.then(PowerCommand.get().requires(GAME_MASTER));
        root.then(MaidNumCommand.get().requires(GAME_MASTER));
        root.then(MaidDebugCommand.get().requires(GAME_MASTER));
        root.then(AIChatCommand.get().requires(GAME_MASTER));
        root.then(ConfigCommand.get()
                .requires(source -> GAME_MASTER.test(source) || isSingleplayerOwner(source)));
        root.then(MaidCommand.get().requires(GAME_MASTER));
        root.then(BackupCommand.get().requires(GAME_MASTER));
        dispatcher.register(root);
    }

    private static boolean isSingleplayerOwner(CommandSourceStack source) {
        return source.getPlayer() != null
                && source.getServer().isSingleplayerOwner(source.getPlayer().nameAndId());
    }
}
