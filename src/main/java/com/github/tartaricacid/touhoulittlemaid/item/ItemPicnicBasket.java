package com.github.tartaricacid.touhoulittlemaid.item;

import cn.sh1rocu.touhoulittlemaid.util.itemhandler.ItemStackHandler;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.other.PicnicBasketContainer;
import com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.ItemContainerTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class ItemPicnicBasket extends BlockItem implements ExtendedScreenHandlerFactory<ItemStack> {
    private static final int PICNIC_BASKET_SIZE = 9;

    public ItemPicnicBasket(Identifier id, Block block) {
        // 1.21.11: Item.getDescriptionId() 现 final → Properties.overrideDescription（javap 确认）。
        //   BlockItem 默认 descriptionId 可能带 block. 前缀，故显式设为 HEAD 原键，保证行为等价。
        super(block, (new Properties()).setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(1).overrideDescription("item.touhou_little_maid.picnic_basket"));
    }

    public static ItemStackHandler getContainer(ItemStack stack) {
        ItemStackHandler handler = new ItemStackHandler(PICNIC_BASKET_SIZE);
        if (stack.getItem() == InitItems.PICNIC_BASKET) {
            ItemContainerContents container = stack.get(DataComponents.CONTAINER);
            if (container != null) {
                List<ItemStack> itemStacks = container.stream().toList();
                for (int i = 0; i < container.stream().count(); i++) {
                    handler.setStackInSlot(i, itemStacks.get(i));
                }
            }
        }
        return handler;
    }

    public static void setContainer(ItemStack stack, ItemStackHandler itemStackHandler) {
        if (stack.getItem() == InitItems.PICNIC_BASKET) {
            NonNullList<ItemStack> items = NonNullList.withSize(PICNIC_BASKET_SIZE, ItemStack.EMPTY);
            for (int i = 0; i < itemStackHandler.getSlots(); i++) {
                items.set(i, itemStackHandler.getStackInSlot(i));
            }
            ItemContainerContents container = ItemContainerContents.fromItems(items);
            stack.set(DataComponents.CONTAINER, container);
        }
    }

    @Override
    public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {
        if (handIn == InteractionHand.MAIN_HAND && playerIn instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this/*, data -> ItemStack.STREAM_CODEC.encode(data, serverPlayer.getMainHandItem())*/);
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.use(worldIn, playerIn, handIn);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        ItemStackHandler container = getContainer(stack);
        return Optional.of(new ItemContainerTooltip(container));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(this.getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new PicnicBasketContainer(containerId, playerInventory, player.getMainHandItem());
    }

    @Override
    public ItemStack getScreenOpeningData(ServerPlayer player) {
        return player.getMainHandItem();
    }
}
