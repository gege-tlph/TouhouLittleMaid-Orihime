package com.github.tartaricacid.touhoulittlemaid.entity.backpack;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.IMaidBackpack;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.MaidBackpackRenderData;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.backpack.CraftingTableBackpackRenderData;
// TODO: 1.21.11 client exclusion
// import com.github.tartaricacid.touhoulittlemaid.client.resource.BedrockModelLoader;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityTombstone;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.AbstractMaidContainer;
import com.github.tartaricacid.touhoulittlemaid.inventory.container.backpack.CraftingTableBackpackContainer;
import com.github.tartaricacid.touhoulittlemaid.item.BackpackLevel;
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
import org.jetbrains.annotations.Nullable;

// TODO: 1.21.11 client exclusion
// import static com.github.tartaricacid.touhoulittlemaid.client.resource.BedrockModelLoader.CRAFTING_TABLE_BACKPACK;

public class CraftingTableBackpack extends IMaidBackpack {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "crafting_table_backpack");

    @Override
    public void onPutOn(ItemStack stack, Player player, EntityMaid maid) {
    }

    @Override
    public void onTakeOff(ItemStack stack, Player player, EntityMaid maid) {
        dropRelativeItems(stack, maid);
    }

    @Override
    public void onSpawnTombstone(EntityMaid maid, EntityTombstone tombstone) {
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
                return Component.literal("Maid Crafting Table Container");
            }

            @Override
            public AbstractMaidContainer createMenu(int index, Inventory playerInventory, Player player) {
                return new CraftingTableBackpackContainer(index, playerInventory, entityId);
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
    public int getAvailableMaxContainerIndex() {
        return BackpackLevel.CRAFTING_TABLE_CAPACITY;
    }

    @Override
    public MaidBackpackRenderData getRenderData() {
        // 模型已入 InternalBedrockModelRegistry（origin 本就渲染此背包，旧 EMPTY 注释系误判）
        return new CraftingTableBackpackRenderData();
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void offsetBackpackItem(PoseStack poseStack) {
        poseStack.translate(0, 0.625, 0.25);
    }

    @Nullable
    @Override
    @Environment(EnvType.CLIENT)
    public EntityModel<?> getBackpackModel(EntityModelSet modelSet) {
        // TODO: 1.21.11 client exclusion
        // return BedrockModelLoader.getModel(CRAFTING_TABLE_BACKPACK);
        return null;
    }

    @Nullable
    @Override
    @Environment(EnvType.CLIENT)
    public Identifier getBackpackTexture() {
        return Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "textures/bedrock/entity/backpack/crafting_table_backpack.png");
    }

    @Override
    public Identifier getId() {
        return ID;
    }

    @Override
    public Item getItem() {
        return InitItems.CRAFTING_TABLE_BACKPACK;
    }
}
