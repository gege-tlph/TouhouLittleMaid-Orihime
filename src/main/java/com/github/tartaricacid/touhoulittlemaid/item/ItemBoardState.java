package com.github.tartaricacid.touhoulittlemaid.item;


import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.BoardStateTooltip;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public class ItemBoardState extends Item {
    public static final String DATA_TAG = "BoardStateData";
    public static final String DESC_TAG = "BoardStateDesc";
    public static final String AUTHOR_TAG = "BoardStateAuthor";

    public ItemBoardState(Identifier id) {
        super((new Properties()).setId(ResourceKey.create(Registries.ITEM, id)));
    }

    public static void setState(ItemStack stack, String data, String desc, String author) {
        BoardStateInfo info = new BoardStateInfo(data, desc, author);
        stack.set(InitDataComponent.BOARD_STATE_TAG, info);
    }

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
        // SWEEP R12-2：Screen.hasShiftDown() 1.21.11 已删（shift 态迁入 InputWithModifiers 事件对象）；
        // tooltip 时刻的全局 shift 查询 = InputConstants.isKeyDown(window, KEY_LSHIFT/RSHIFT)
        // （即旧 hasShiftDown 的底层实现）——还原 origin「按 shift 显示棋盘图」行为
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

    // TODO: 1.21.11 fix - check if appendHoverText override matches supertype
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag tooltipFlag){
        String[] state = getState(stack);

        if (state == null) {
            tooltip.accept(Component.translatable("tooltips.touhou_little_maid.board_state.empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        String descKey = state[1];
        if (StringUtils.isNotBlank(descKey)) {
            tooltip.accept(Component.translatable(descKey).withStyle(ChatFormatting.GRAY));
        }

        String author = state[2];
        if (StringUtils.isNotBlank(author)) {
            tooltip.accept(Component.translatable("tooltips.touhou_little_maid.board_state.author", author).withStyle(ChatFormatting.GRAY));
        }

        // SWEEP R12-2：同上，还原 origin「未按 shift 时显示提示行」
        if (!tlm$hasShiftDown()) {
            tooltip.accept(Component.translatable("board_state.touhou_little_maid.show_picture")
                    .withStyle(ChatFormatting.DARK_GRAY).withStyle(ChatFormatting.ITALIC));
        }
    }

    // SWEEP R12-2：旧 Screen.hasShiftDown() 的等价实现（底层同为 GLFW 键态轮询）。
    // 仅客户端 tooltip 路径调用（getTooltipImage 有 @Environment(CLIENT)；appendHoverText 只在客户端执行，
    // 与 origin import Screen 的 client-in-common 形态一致）。
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