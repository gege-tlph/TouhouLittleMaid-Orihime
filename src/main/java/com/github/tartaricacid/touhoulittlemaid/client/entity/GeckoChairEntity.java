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
     * 与 {@code GeckoMaidEntity.setMaidInfo} 对称。缺了这句 {@code setModelId}，实体的 modelId
     * 恒为 null，{@code checkGeckoContainerUpdateInner} 便从不去查容器，{@code isModelPresent()}
     * 恒 false，{@code createUpdateTask} 返回空任务——gecko 坐垫于是什么都不提交：模型切换器的
     * 格子、模型详情屏、掉落物、手持与放置在世界里，一律为空。
     *
     * <p>origin/1.21.1 没有 modelId 字段（它覆写 getModelLocation 直接读 chairInfo），所以这是
     * 26.1 式容器查找在坐垫上的接线；女仆侧一直有（GeckoMaidEntity 的 setMaidInfo），坐垫侧
     * 在移植中丢了。2026-08-20 实机四症状（切换器 Geckolib 页整页空白、详情屏空白、掉落物零
     * 顶点、放置态不显示）由本条一并解释。</p>
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
