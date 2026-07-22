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
     * 设置椅子模型信息，并同步模型 ID，以便 Gecko 容器找到正确的模型与控制器。
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
     * 缓存中的预览椅子使用负实体 ID，也不会参与世界刻更新。
     * 将其标记为预览实体后，Gecko 会改用客户端全局时间推进动画并刷新模型骨骼。
     */
    @Override
    public boolean isPreviewEntity() {
        return entity.getId() < 0;
    }
}
