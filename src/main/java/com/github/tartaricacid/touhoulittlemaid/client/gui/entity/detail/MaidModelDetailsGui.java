package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.detail;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.cache.CacheIconManager;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.ModelDetailsButton;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MaidModelDetailsGui extends AbstractModelDetailsGui<EntityMaid, MaidModelInfo> {
    private static final ItemStack MAIN_HAND_SWORD = Items.DIAMOND_SWORD.getDefaultInstance();
    private static final ItemStack OFF_HAND_SHIELD = Items.SHIELD.getDefaultInstance();
    private static final ItemStack ARMOR_ITEM = Items.GOLDEN_HELMET.getDefaultInstance();
    private EntityChair chair;
    private volatile boolean isEnableWalk = false;

    public MaidModelDetailsGui(EntityMaid sourceEntity, MaidModelInfo modelInfo) {
        super(sourceEntity, InitEntities.MAID.create(sourceEntity.level(), EntitySpawnReason.COMMAND), modelInfo);
        this.guiEntity.setModelId(modelInfo.getModelId().toString());
        this.guiEntity.setOnGround(true);
        this.guiEntity.yHeadRot = 0;
        this.guiEntity.yHeadRotO = 0;
        this.initChair();
    }

    private void initChair() {
        if (Minecraft.getInstance().level != null) {
            this.chair = InitEntities.CHAIR.create(Minecraft.getInstance().level, EntitySpawnReason.COMMAND);
            if (this.chair != null) {
                this.chair.setModelId("touhou_little_maid:low_stool");
            }
        }
    }

    @Override
    protected void applyReturnButtonLogic() {
        CacheIconManager.openMaidModelGui(sourceEntity);
    }

    @Override
    protected void initSideButton() {
        ModelDetailsButton begButton = new ModelDetailsButton(2, 17, "gui.touhou_little_maid.skin_details.beg",
                (isStateTriggered) -> guiEntity.setBegging(isStateTriggered));
        ModelDetailsButton walkButton = new ModelDetailsButton(2, 17 + 13, "gui.touhou_little_maid.skin_details.walk",
                (isStateTriggered) -> isEnableWalk = isStateTriggered);
        ModelDetailsButton sitButton = new ModelDetailsButton(2, 17 + 13 * 2, "gui.touhou_little_maid.skin_details.sit",
                (isStateTriggered) -> guiEntity.setInSittingPose(isStateTriggered));
        ModelDetailsButton rideButton = new ModelDetailsButton(2, 17 + 13 * 3, "gui.touhou_little_maid.skin_details.ride",
                this::applyRideButtonLogic);
        ModelDetailsButton helmetButton = new ModelDetailsButton(2, 17 + 13 * 4, "gui.touhou_little_maid.skin_details.helmet",
                (isStateTriggered) -> applyEquipmentButtonLogic(EquipmentSlot.HEAD, isStateTriggered));
        ModelDetailsButton chestPlateButton = new ModelDetailsButton(2, 17 + 13 * 5, "gui.touhou_little_maid.skin_details.chest_plate",
                (isStateTriggered) -> applyEquipmentButtonLogic(EquipmentSlot.CHEST, isStateTriggered));
        ModelDetailsButton leggingsButton = new ModelDetailsButton(2, 17 + 13 * 6, "gui.touhou_little_maid.skin_details.leggings",
                (isStateTriggered) -> applyEquipmentButtonLogic(EquipmentSlot.LEGS, isStateTriggered));
        ModelDetailsButton bootsButton = new ModelDetailsButton(2, 17 + 13 * 7, "gui.touhou_little_maid.skin_details.boots",
                (isStateTriggered) -> applyEquipmentButtonLogic(EquipmentSlot.FEET, isStateTriggered));
        ModelDetailsButton mainHandButton = new ModelDetailsButton(2, 17 + 13 * 8, "gui.touhou_little_maid.skin_details.main_hand",
                (isStateTriggered) -> applyEquipmentButtonLogic(EquipmentSlot.MAINHAND, isStateTriggered));
        ModelDetailsButton offHandButton = new ModelDetailsButton(2, 17 + 13 * 9, "gui.touhou_little_maid.skin_details.off_hand",
                (isStateTriggered) -> applyEquipmentButtonLogic(EquipmentSlot.OFFHAND, isStateTriggered));
        this.addRenderableWidget(begButton);
        this.addRenderableWidget(walkButton);
        this.addRenderableWidget(sitButton);
        this.addRenderableWidget(rideButton);
        this.addRenderableWidget(helmetButton);
        this.addRenderableWidget(chestPlateButton);
        this.addRenderableWidget(leggingsButton);
        this.addRenderableWidget(bootsButton);
        this.addRenderableWidget(mainHandButton);
        this.addRenderableWidget(offHandButton);
    }

    @Override
    public void tick() {
        // Tick count increment for some animations
        guiEntity.tickCount++;
        // For entity walk
        // Update walk speed
        float speed = isEnableWalk ? 0.5f : 0;
        // 1.21.11: WalkAnimationState.update 增第三参 positionScale；vanilla 非幼体固定传 1.0F（origin 2 参版即此语义）
        guiEntity.walkAnimation.update(speed, 0.4f, 1.0F);
    }

    @Override
    protected void renderExtraEntity(GuiGraphics graphics, float partialTicks) {
        if (guiEntity.isPassenger() && chair != null) {
            // origin: manager.render(chair, 0, -0.95, 0, 0, 1, matrix, bufferIn, 0xf000f0)（partialTicks 固定 1）
            submitExtraEntity(graphics, chair, 0, -0.95F, 0, 1);
        }
    }

    private void applyRideButtonLogic(boolean isStateTriggered) {
        if (isStateTriggered && chair != null) {
            // 1.21.11: startRiding(Entity, boolean) 移除 -> (Entity, boolean, boolean)（2 参版原语义 = 第三参 true）
            guiEntity.startRiding(chair, true, true);
        } else {
            guiEntity.removeVehicle();
        }
    }

    private void applyEquipmentButtonLogic(EquipmentSlot slot, boolean isStateTriggered) {
        if (isStateTriggered) {
            if (slot == EquipmentSlot.MAINHAND) {
                guiEntity.setItemSlot(slot, MAIN_HAND_SWORD);
            } else if (slot == EquipmentSlot.OFFHAND) {
                guiEntity.setItemSlot(slot, OFF_HAND_SHIELD);
            } else {
                guiEntity.setItemSlot(slot, ARMOR_ITEM);
            }
        } else {
            guiEntity.setItemSlot(slot, ItemStack.EMPTY);
        }
    }
}
