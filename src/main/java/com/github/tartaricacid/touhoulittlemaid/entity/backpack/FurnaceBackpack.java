package com.github.tartaricacid.touhoulittlemaid.entity.backpack;

import cn.sh1rocu.touhoulittlemaid.util.itemhandler.InvWrapper;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.IBackpackData;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.IMaidBackpack;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.MaidBackpackRenderData;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.backpack.FurnaceBackpackRenderData;
// TODO: 1.21.11 client exclusion
// import com.github.tartaricacid.touhoulittlemaid.client.resource.BedrockModelLoader;
import com.github.tartaricacid.touhoulittlemaid.entity.backpack.data.FurnaceBackpackData;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityTombstone;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.AbstractMaidContainer;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack.FurnaceBackpackContainer;
import com.github.tartaricacid.touhoulittlemaid.item.BackpackLevel;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

// TODO: 1.21.11 client exclusion
// import static com.github.tartaricacid.touhoulittlemaid.client.resource.BedrockModelLoader.FURNACE_BACKPACK;

public class FurnaceBackpack extends IMaidBackpack {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "furnace_backpack");

    @Override
    public void onPutOn(ItemStack stack, Player player, EntityMaid maid) {
    }

    @Override
    public void onTakeOff(ItemStack stack, Player player, EntityMaid maid) {
        IBackpackData backpackData = maid.getBackpackData();
        if (backpackData instanceof FurnaceBackpackData furnaceBackpackData) {
            InvWrapper inv = new InvWrapper(furnaceBackpackData);
            ItemsUtil.dropEntityItems(maid, inv);
        }
        dropRelativeItems(stack, maid);
    }

    @Override
    public void onSpawnTombstone(EntityMaid maid, EntityTombstone tombstone) {
        IBackpackData backpackData = maid.getBackpackData();
        if (backpackData instanceof FurnaceBackpackData furnaceBackpackData) {
            InvWrapper inv = new InvWrapper(furnaceBackpackData);
            for (int i = 0; i < inv.getSlots(); i++) {
                int size = inv.getSlotLimit(i);
                tombstone.insertItem(inv.extractItem(i, size, false));
            }
        }
    }

    @Override
    public MenuProvider getGuiProvider(int entityId) {
        return new ExtendedScreenHandlerFactory<Integer>() {
            @Override
            public Integer getScreenOpeningData(ServerPlayer player) {
                return entityId;
            }

            @Override
            public Component getDisplayName() {
                return Component.literal("Maid Furnace Container");
            }

            @Override
            public AbstractMaidContainer createMenu(int index, Inventory playerInventory, Player player) {
                return new FurnaceBackpackContainer(index, playerInventory, entityId);
            }

            @Override
            public boolean shouldCloseCurrentScreen() {
                return false;
            }

/*            @Override
            public boolean shouldTriggerClientSideContainerClosingOnOpen() {
                return false;
            }*/
        };
    }

    @Override
    public boolean hasBackpackData() {
        return true;
    }

    @Nullable
    @Override
    public IBackpackData getBackpackData(EntityMaid maid) {
        return new FurnaceBackpackData(maid);
    }

    @Override
    public int getAvailableMaxContainerIndex() {
        return BackpackLevel.FURNACE_CAPACITY;
    }

    @Override
    public MaidBackpackRenderData getRenderData() {
        // 模型已入 InternalBedrockModelRegistry（origin 本就渲染此背包，旧 EMPTY 注释系误判）
        return new FurnaceBackpackRenderData();
    }

    @Nullable
    @Override
    @Environment(EnvType.CLIENT)
    public EntityModel<?> getBackpackModel(EntityModelSet modelSet) {
        // TODO: 1.21.11 client exclusion
        // return BedrockModelLoader.getModel(FURNACE_BACKPACK);
        return null;
    }

    @Nullable
    @Override
    @Environment(EnvType.CLIENT)
    public Identifier getBackpackTexture() {
        return Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/bedrock/entity/backpack/furnace_backpack.png");
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void offsetBackpackItem(PoseStack poseStack) {
        poseStack.translate(0, 0.625, 0.25);
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Item getItem() {
        return InitItems.FURNACE_BACKPACK;
    }
}


