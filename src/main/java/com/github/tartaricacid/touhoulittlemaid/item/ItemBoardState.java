package com.github.tartaricacid.touhoulittlemaid.item;

import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * 残局道具：把一局棋存进物品，右键棋盘即可摆上去。
 *
 * <p>三种棋各一个物品（{@code gomoku_/cchess_/wchess_board_state}）。
 * 数据存在 {@link InitDataComponent#BOARD_STATE_TAG} 这个数据组件里，
 * 内容是「棋局数据 + 描述 key + 作者」三段。五子棋那段的编码见 {@code GomokuCodec}，
 * 中国象棋/国际象棋那段是 FEN。</p>
 *
 * <p>⚠️ <b>缺一块，是有意的</b>：行为基准 {@code port/1.21.11-fabric} 的本类还有一个
 * {@code getTooltipImage}——按住 Shift 时在物品提示框里画出棋盘缩略图。
 * 那需要 {@code BoardStateTooltip} 与客户端的 {@code ClientBoardStateTooltip} 渲染器，
 * 属棋局那一簇的「提示框预览」那一刀。**在它落地之前不要贸然加回 getTooltipImage**：
 * 返回一个没有注册客户端渲染器的 TooltipComponent，画不出来还可能直接抛。</p>
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
