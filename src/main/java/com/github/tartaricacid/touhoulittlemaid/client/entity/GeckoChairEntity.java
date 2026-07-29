package com.github.tartaricacid.touhoulittlemaid.client.entity;

import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.ChairModelInfo;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.AnimatableEntity;

import java.util.function.Consumer;

public class GeckoChairEntity extends AnimatableEntity<EntityChair> {
    private ChairModelInfo chairInfo;

    public GeckoChairEntity(EntityChair entity) {
        super(entity, false);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void onSetupAnimationController() {
        var container = getGeckoContainer();
        if (container != null) {
            ((Consumer<GeckoChairEntity>) container.controllerFactory()).accept(this);
        }
    }

    public ChairModelInfo getChairInfo() {
        return chairInfo;
    }

    /**
     * Mirrors {@code GeckoMaidEntity.setMaidInfo}. Without the setModelId call the entity's
     * modelId stays null, so checkGeckoContainerUpdateInner never looks the container up,
     * isModelPresent() stays false and createUpdateTask returns the nop task: the gecko chair
     * then submits nothing at all, in the item icon, in hand and placed in the world alike.
     * origin/1.21.1 has no modelId field — it overrides getModelLocation() to read chairInfo
     * directly — so this is the 26.1-style container lookup this port adopted, wired up for
     * chairs the same way it already is for maids.
     */
    public void setChair(ChairModelInfo chairInfo) {
        waitForAsyncUpdate();
        if (this.chairInfo != chairInfo) {
            this.chairInfo = chairInfo;
            if (chairInfo != null) {
                setModelId(chairInfo.getModelId());
            }
        }
    }

    /**
     * Item previews use the EntityCacheUtil chairs, which carry negative ids and are never
     * ticked, so entity.tickCount stays 0 forever. Gecko's frame time then never advances,
     * the per-frame tick flags never reset, and the immutable (in-level) update path never
     * re-extracts mainModelState — its render bone list stays empty and the dropped item
     * submits zero vertices. Reporting these entities as previews switches the animation
     * clock to the global client tick, the same escape hatch GeckoMaidEntity uses via
     * MaidRenderState for its GUI/statue/garage-kit previews.
     */
    @Override
    public boolean isPreviewEntity() {
        return entity.getId() < 0;
    }
}
