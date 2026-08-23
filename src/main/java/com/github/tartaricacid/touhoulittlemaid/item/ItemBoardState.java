package com.github.tartaricacid.touhoulittlemaid.item;

import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.BoardStateTooltip;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 残局道具：把一局棋存进物品，右键棋盘即可摆上去。
 *
 * <p>三种棋各一个物品（{@code gomoku_/cchess_/wchess_board_state}）。
 * 数据存在 {@link InitDataComponent#BOARD_STATE_TAG} 这个数据组件里，
 * 内容是「棋局数据 + 描述 key + 作者」三段。五子棋那段的编码见 {@code GomokuCodec}，
 * 中国象棋/国际象棋那段是 FEN。</p>
 *
 * <p>按住 Shift 时 {@link #getTooltipImage} 返回 {@link BoardStateTooltip}，
 * 由客户端的 {@code ClientBoardStateTooltip} 画出棋盘缩略图；未按 Shift 时
 * {@code appendHoverText} 会补一行「按下 Shift 显示细节」的提示。
 * （历史注：预览那一刀 {@code be630452f} 只落了组件与渲染器，这两处生产者是审计时
 * 发现「ofGomoku 三个工厂零调用者」后才补回的——链路两端都要有人，缺一端都是死代码。）</p>
 */
public class ItemBoardState extends Item {
    public ItemBoardState(Identifier id) {
        super(new Properties().setId(ResourceKey.create(Registries.ITEM, id)));
    }

    public static void setState(ItemStack stack, String data, String desc, String author) {
        stack.set(InitDataComponent.BOARD_STATE_TAG, new BoardStateInfo(data, desc, author));
    }

    /** @return {@code [棋局数据, 描述 key, 作者]}；物品里没存过残局时为 null */
    @Nullable
    public static String[] getState(ItemStack stack) {
        BoardStateInfo info = stack.get(InitDataComponent.BOARD_STATE_TAG);
        if (info == null) {
            return null;
        }
        return new String[]{info.data(), info.description(), info.author()};
    }

    @Override
    @Environment(EnvType.CLIENT)
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        // 26.1.2 与 1.21.11 同款：Screen.hasShiftDown() 已删（javap 实查无此方法），
        // tooltip 时刻的全局 shift 查询用 InputConstants.isKeyDown——即旧实现的底层。
        if (!tlm$hasShiftDown()) {
            return Optional.empty();
        }

        String[] state = getState(stack);
        if (state == null) {
            return Optional.empty();
        }
        String stateData = state[0];
        if (StringUtils.isBlank(stateData)) {
            return Optional.empty();
        }

        if (stack.is(InitItems.GOMOKU_BOARD_STATE)) {
            return Optional.of(BoardStateTooltip.ofGomoku(stateData));
        } else if (stack.is(InitItems.CCHESS_BOARD_STATE)) {
            return Optional.of(BoardStateTooltip.ofXiangqi(stateData));
        } else if (stack.is(InitItems.WCHESS_BOARD_STATE)) {
            return Optional.of(BoardStateTooltip.ofChess(stateData));
        }

        return Optional.empty();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay,
                                Consumer<Component> tooltip, TooltipFlag tooltipFlag) {
        String[] state = getState(stack);
        if (state == null) {
            tooltip.accept(Component.translatable("tooltips.touhou_little_maid.board_state.empty")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        String descKey = state[1];
        if (StringUtils.isNotBlank(descKey)) {
            tooltip.accept(Component.translatable(descKey).withStyle(ChatFormatting.GRAY));
        }

        String author = state[2];
        if (StringUtils.isNotBlank(author)) {
            tooltip.accept(Component.translatable("tooltips.touhou_little_maid.board_state.author", author)
                    .withStyle(ChatFormatting.GRAY));
        }

        // 未按 Shift 时提示「按下 Shift 显示细节」——与 getTooltipImage 的门控是同一对
        if (!tlm$hasShiftDown()) {
            tooltip.accept(Component.translatable("board_state.touhou_little_maid.show_picture")
                    .withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC));
        }
    }

    // 仅客户端 tooltip 路径调用（getTooltipImage 有 @Environment(CLIENT)；appendHoverText 只在
    // 客户端执行），与 origin 在 common 类里 import Screen 的 client-in-common 形态一致。
    @Environment(EnvType.CLIENT)
    private static boolean tlm$hasShiftDown() {
        var window = net.minecraft.client.Minecraft.getInstance().getWindow();
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, com.mojang.blaze3d.platform.InputConstants.KEY_LSHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, com.mojang.blaze3d.platform.InputConstants.KEY_RSHIFT);
    }

    public record BoardStateInfo(String data, String description, String author) {
        public static final Codec<BoardStateInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("data").forGetter(BoardStateInfo::data),
                Codec.STRING.fieldOf("description").forGetter(BoardStateInfo::description),
                Codec.STRING.fieldOf("author").forGetter(BoardStateInfo::author)
        ).apply(instance, BoardStateInfo::new));

        public static final StreamCodec<ByteBuf, BoardStateInfo> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, BoardStateInfo::data,
                ByteBufCodecs.STRING_UTF8, BoardStateInfo::description,
                ByteBufCodecs.STRING_UTF8, BoardStateInfo::author,
                BoardStateInfo::new
        );
    }
}
