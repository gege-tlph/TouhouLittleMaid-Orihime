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

    public void setChair(ChairModelInfo chairInfo) {
        this.chairInfo = chairInfo;
    }

    /**
     * Item previews use the {@code EntityCacheUtil} chairs, which carry negative ids and are
     * never ticked, so {@code entity.tickCount} stays 0 forever. Gecko's frame time then never
     * advances, the per-frame tick flags never reset, and the immutable (in-level) update path
     * never re-extracts {@code mainModelState} — its render bone list stays empty and the
     * dropped item submits zero vertices. The mutable (held/GUI) path re-extracts
     * unconditionally, which is why only the dropped form was affected.
     *
     * <p>Reporting these entities as previews switches the animation clock to the global client
     * tick. {@link com.github.tartaricacid.touhoulittlemaid.client.entity.GeckoMaidEntity} uses
     * the same escape hatch, but keys off {@code MaidRenderState}; a chair has no such enum, so
     * the negative preview id assigned by {@code EntityCacheUtil.getEntity} is the judgment.</p>
     */
    @Override
    public boolean isPreviewEntity() {
        return entity.getId() < 0;
    }
}
