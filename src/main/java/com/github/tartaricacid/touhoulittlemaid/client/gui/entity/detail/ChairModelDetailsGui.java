package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.detail;

import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.cache.CacheIconManager;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.ModelDetailsButton;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.ChairModelInfo;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EntitySpawnReason;

public class ChairModelDetailsGui extends AbstractModelDetailsGui<EntityChair, ChairModelInfo> {
    private EntityMaid maid;
    private boolean showPassenger = false;

    public ChairModelDetailsGui(EntityChair sourceEntity, ChairModelInfo modelInfo) {
        super(sourceEntity, InitEntities.CHAIR.create(sourceEntity.level(), EntitySpawnReason.COMMAND), modelInfo);
        guiEntity.setModelId(modelInfo.getModelId().toString());
        if (Minecraft.getInstance().level != null) {
            this.maid = InitEntities.MAID.create(Minecraft.getInstance().level, EntitySpawnReason.COMMAND);
            if (this.maid != null) {
                this.maid.setModelId("authors_and_credits:wine_fox_maid");
            }
        }
    }

    @Override
    protected void applyReturnButtonLogic() {
        CacheIconManager.openChairModelGui(sourceEntity);
    }

    @Override
    protected void initSideButton() {
        ModelDetailsButton showPassengerButton = new ModelDetailsButton(2, 17, "gui.touhou_little_maid.skin_details.show_passenger",
                this::applyShowPassengerLogic);
        this.addRenderableWidget(showPassengerButton);
    }

    @Override
    public void tick() {
        guiEntity.tickCount++;
        if (maid != null) {
            maid.tickCount++;
        }
    }

    @Override
    protected void renderExtraEntity(GuiGraphics graphics, float partialTicks) {
        if (showPassenger) {
            // origin: manager.render(maid, 0, -0.375 + mountedYOffset, 0, 0, 1, matrix, bufferIn, 0xf000f0)（partialTicks 固定 1）
            submitExtraEntity(graphics, maid, 0, (float) (-0.375 + modelInfo.getMountedYOffset()), 0, 1);
        }
    }

    private void applyShowPassengerLogic(boolean isStateTriggered) {
        this.showPassenger = isStateTriggered;
        if (isStateTriggered && maid != null) {
            // 1.21.11: startRiding(Entity, boolean) 移除 -> (Entity, boolean, boolean)（2 参版原语义 = 第三参 true）
            maid.startRiding(guiEntity, true, true);
        } else {
            guiEntity.ejectPassengers();
        }
    }
}
