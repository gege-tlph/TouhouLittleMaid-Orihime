package com.github.tartaricacid.touhoulittlemaid.command.subcommand;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.ChatClientInfo;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/**
 * 开发环境专用：从服务端驱动一整轮女仆对话。
 *
 * <p><b>为什么需要它。</b>女仆对话的唯一入口是聊天界面里的输入框，经 C2S
 * {@code SendUserChatPackage} 进服务端。本仓库的自动化载体（两个 HTTP MCP 端点）
 * <b>不能点击也不能按键</b>，于是「多轮对话后 TTS 语言漂移」这类**只在第 N 轮才出现**的缺陷
 * 一直只能靠人手打字复现——而它恰恰需要精确控制轮数、语种与每轮的措辞。</p>
 *
 * <p><b>守卫与玩家真实路径逐条相同</b>（女仆归发起者所有 + 存活）。这不是形式主义：
 * 探针一旦绕过某个守卫，就可能走通一条玩家永远走不到的路径，那样测出来的结论是关于
 * 一个不存在的程序的。语种作为参数显式传入，因为真实路径里它来自客户端的
 * {@code ChatClientInfo.language()}——那是本次对话「聊天语言」的唯一来源，
 * 也是 {@code needsSeparateTtsText} 的判据之一。</p>
 *
 * <p><b>只在开发环境注册</b>（{@link TouhouLittleMaid#DEBUG}，即
 * {@code FabricLoader.isDevelopmentEnvironment()}）。发布产物里这条命令不存在，
 * 因此它不构成攻击面：它能做的事玩家本来就能做，只是不必用鼠标。</p>
 */
public final class AiChatDevCommand {
    private static final String NAME = "dev_say";
    private static final String MAID = "maid";
    private static final String LANGUAGE = "language";
    private static final String MESSAGE = "message";

    private AiChatDevCommand() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> get() {
        return LiteralArgumentBuilder.<CommandSourceStack>literal(NAME)
                // 1.21.11：CommandSourceStack.hasPermission(int) 已移除，用 Commands.hasPermission(PermissionCheck)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(RequiredArgumentBuilder.<CommandSourceStack, net.minecraft.commands.arguments.selector.EntitySelector>
                                argument(MAID, EntityArgument.entity())
                        .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument(LANGUAGE, StringArgumentType.word())
                                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument(MESSAGE, StringArgumentType.greedyString())
                                        .executes(AiChatDevCommand::say))));
    }

    private static int say(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sender = context.getSource().getPlayerOrException();
        Entity entity = EntityArgument.getEntity(context, MAID);
        if (!(entity instanceof EntityMaid maid)) {
            context.getSource().sendFailure(Component.literal("目标不是女仆：" + entity.getType()));
            return 0;
        }
        // 与 SendUserChatPackage.onHandle 的守卫逐条相同，不得放宽
        if (!maid.isOwnedBy(sender) || !maid.isAlive()) {
            context.getSource().sendFailure(Component.literal(
                    "女仆不归你所有或已死亡，玩家路径同样走不通，探针不绕过这个判据"));
            return 0;
        }

        String language = StringArgumentType.getString(context, LANGUAGE);
        String message = StringArgumentType.getString(context, MESSAGE);
        // name/description 只进提示词的人设段，与本探针要测的东西无关，给最小可用值
        ChatClientInfo clientInfo = new ChatClientInfo(language, maid.getName().getString(), Lists.newArrayList());

        // 前缀不用 [TLM-QA-*]：那是「用完即删」的临时探针约定，而这条命令是长期开发设施
        TouhouLittleMaid.LOGGER.info("[TLM-DEV-SAY] maid={} chatLanguage={} ttsLanguage={} message={}",
                maid.getId(), language, maid.getAiChatManager().getTTSLanguage(), message);
        maid.getAiChatManager().chat(message, clientInfo, sender);
        context.getSource().sendSuccess(() -> Component.literal("已发送给女仆 " + maid.getId()), false);
        return Command.SINGLE_SUCCESS;
    }
}
