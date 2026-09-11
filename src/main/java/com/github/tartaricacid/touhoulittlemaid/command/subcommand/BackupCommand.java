package com.github.tartaricacid.touhoulittlemaid.command.subcommand;

import com.github.tartaricacid.touhoulittlemaid.item.ItemCamera;
import com.github.tartaricacid.touhoulittlemaid.world.backups.MaidBackupsManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class BackupCommand {
    private static final String BACKUP_NAME = "backup";
    private static final String GET_NAME = "get";
    private static final String PLAYER_NAME = "player";
    private static final String MAID_UUID = "maid_uuid";
    private static final String FILE_NAME = "file_name";

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(BACKUP_NAME);
        LiteralArgumentBuilder<CommandSourceStack> get = Commands.literal(GET_NAME);

        RequiredArgumentBuilder<CommandSourceStack, EntitySelector> player = Commands.argument(PLAYER_NAME, EntityArgument.player());
        RequiredArgumentBuilder<CommandSourceStack, UUID> maidUuid = Commands.argument(MAID_UUID, UuidArgument.uuid());
        RequiredArgumentBuilder<CommandSourceStack, String> fileName = Commands.argument(FILE_NAME, StringArgumentType.string());

        root.then(get.then(player.executes(BackupCommand::handlePlayerMaidIndex)));
        root.then(get.then(player.then(maidUuid.executes(BackupCommand::handlePlayerMaid))));
        root.then(get.then(player.then(maidUuid.then(fileName.executes(BackupCommand::handlePlayerMaidFile)))));

        return root;
    }

    /**
     * 回执同时发给**命令源**与被选中的那个玩家。
     *
     * <p>原实现一条也不发给命令源，只发给 {@code EntityArgument} 选中的玩家。于是从服务器控制台
     * 跑 {@code /tlm backup get <玩家>} 永远一片安静——看起来像命令失效，实际上它执行成功了，
     * 消息全进了那个玩家的聊天栏（2026-08-30 专服实测报的就是这个症状）。同一棵命令树里
     * {@code /tlm ai_chat status} 用的是 {@code sendSuccess}，本来就回命令源，两种写法一直并存。</p>
     *
     * <p>源与目标是同一个玩家时只发一份：{@code sendSuccess} 已经会送到他自己的聊天栏。
     * 第二个参数取 {@code false}，不广播给其他管理员——这是查询回执，不是需要公示的管理操作。</p>
     */
    private static void reply(CommandContext<CommandSourceStack> context, ServerPlayer player, Component message) {
        context.getSource().sendSuccess(() -> message, false);
        if (context.getSource().getEntity() != player) {
            player.displayClientMessage(message, false);
        }
    }

    private static String getFormattedTime(long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofEpochSecond(timestamp / 1000, 0, ZoneOffset.UTC);
        return dateTime.format(DateTimeFormatter.ofPattern("(MM/dd HH:mm)"));
    }

    private static int handlePlayerMaidIndex(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER_NAME);
        var maidIndexMap = MaidBackupsManager.getMaidIndexMap(player);

        // 如果没找到
        if (maidIndexMap.isEmpty()) {
            MutableComponent error = Component.translatable("message.touhou_little_maid.maid_backup.player.no_data", player.getScoreboardName());
            reply(context, player, error.withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }

        // 将 maidIndexMap 按照时间戳，从新到旧排序
        maidIndexMap = maidIndexMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(
                        Comparator.comparingLong(MaidBackupsManager.IndexData::timestamp).reversed()
                ))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        MutableComponent separator = Component.translatable("message.touhou_little_maid.maid_backup.player.separator", player.getScoreboardName());
        reply(context, player, separator.withStyle(ChatFormatting.DARK_GREEN));

        // 把女仆的信息按行打印出来，显示名称、坐标和维度
        int index = 1;
        for (var entry : maidIndexMap.entrySet()) {
            UUID id = entry.getKey();
            MaidBackupsManager.IndexData data = entry.getValue();

            String time = "§7" + getFormattedTime(data.timestamp());
            MutableComponent msg = Component.literal("§7%d.".formatted(index))
                    .append(CommonComponents.SPACE)
                    .append(data.name())
                    .withStyle(ChatFormatting.GOLD)
                    .append(CommonComponents.SPACE)
                    .append(time);

            MutableComponent dimension = Component.translatable("tooltips.touhou_little_maid.fox_scroll.dimension", data.dimension());
            MutableComponent pos = Component.translatable("tooltips.touhou_little_maid.fox_scroll.position", data.pos().toShortString());
            // 用目标玩家的名字而不是 @s：回执现在也发给命令源（管理员/控制台），
            // 而 @s 在**点击者**身上求值——管理员点一下就跳去查自己的备份了。
            String command = "/tlm backup get %s %s".formatted(player.getScoreboardName(), id);

            // B5: 1.21.11 HoverEvent 亦改为 sealed 抽象类 → new HoverEvent.ShowText(comp)（26.1+javap 一致）
            HoverEvent hoverEvent = new HoverEvent.ShowText(CommonComponents.joinLines(dimension, pos));
            // B5: 1.21.11 ClickEvent 改为 sealed 抽象类 → new ClickEvent.RunCommand(cmd)（26.1+javap 一致）
            ClickEvent clickEvent = new ClickEvent.RunCommand(command);

            msg.withStyle(style -> style.withHoverEvent(hoverEvent))
                    .withStyle(style -> style.withClickEvent(clickEvent));

            reply(context, player, msg);
            index++;
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int handlePlayerMaid(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER_NAME);
        UUID uuid = UuidArgument.getUuid(context, MAID_UUID);

        var maidIndexMap = MaidBackupsManager.getMaidIndexMap(player);
        var indexData = maidIndexMap.get(uuid);
        if (indexData == null) {
            MutableComponent error = Component.translatable("message.touhou_little_maid.maid_backup.maid.not_found", uuid);
            reply(context, player, error.withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }

        MutableComponent separator = Component.translatable("message.touhou_little_maid.maid_backup.maid.separator", indexData.name());
        reply(context, player, separator.withStyle(ChatFormatting.DARK_GREEN));

        var backupFiles = MaidBackupsManager.getMaidBackupFiles(player, uuid);
        for (String backupFile : backupFiles) {
            MutableComponent msg = Component.literal(backupFile).withStyle(ChatFormatting.DARK_PURPLE);

            // 同上：@s 会在点击者身上求值，而这条回执不再只发给目标玩家自己。
            String command = "/tlm backup get %s %s \"%s\"".formatted(player.getScoreboardName(), uuid, backupFile);
            // B5: 1.21.11 ClickEvent 改为 sealed 抽象类 → new ClickEvent.RunCommand(cmd)（26.1+javap 一致）
            ClickEvent clickEvent = new ClickEvent.RunCommand(command);
            msg.withStyle(style -> style.withClickEvent(clickEvent));

            reply(context, player, msg);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int handlePlayerMaidFile(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, PLAYER_NAME);
        UUID uuid = UuidArgument.getUuid(context, MAID_UUID);
        String fileName = StringArgumentType.getString(context, FILE_NAME);

        CompoundTag backupData = MaidBackupsManager.getMaidBackFile(player, uuid, fileName);
        if (backupData.isEmpty()) {
            MutableComponent error = Component.translatable("message.touhou_little_maid.maid_backup.file.not_found", fileName);
            reply(context, player, error.withStyle(ChatFormatting.RED));
            return Command.SINGLE_SUCCESS;
        }

        // 生成包含此女仆信息的照片
        ItemCamera.spawnMaidPhoto(player.level, backupData, player);
        // 发送成功信息
        MutableComponent success = Component.translatable("message.touhou_little_maid.maid_backup.file.success", fileName);
        reply(context, player, success.withStyle(ChatFormatting.GREEN));
        return Command.SINGLE_SUCCESS;
    }
}
