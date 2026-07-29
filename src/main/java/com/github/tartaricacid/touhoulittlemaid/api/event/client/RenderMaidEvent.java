package com.github.tartaricacid.touhoulittlemaid.api.event.client;

import cn.sh1rocu.touhoulittlemaid.api.event.CancellableEvent;
import com.github.tartaricacid.touhoulittlemaid.api.animation.IAnimation;
import com.github.tartaricacid.touhoulittlemaid.api.entity.IMaid;
import com.github.tartaricacid.touhoulittlemaid.client.model.bedrock.EntityMaidModel;
import com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state.EntityMaidRenderState;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import javax.annotation.Nullable;
import java.util.List;

import static cn.sh1rocu.touhoulittlemaid.TouhouLittleMaidFabric.*;

@Environment(EnvType.CLIENT)
public class RenderMaidEvent extends CancellableEvent {
    private final IMaid maid;
    private final ModelData modelData;

    public RenderMaidEvent(IMaid maid, ModelData modelData) {
        this.maid = maid;
        this.modelData = modelData;
    }

    public IMaid getMaid() {
        return maid;
    }

    public ModelData getModelData() {
        return modelData;
    }

    public static final Event<Callback> CALLBACK = EventFactory.createWithPhases(Callback.class, callbacks -> event -> {
        for (Callback callback : callbacks) {
            callback.post(event);
        }
    }, HIGHEST, HIGH, Event.DEFAULT_PHASE, LOW, LOWEST);

    public interface Callback {
        void post(RenderMaidEvent e);
    }

    /**
     * origin/1.21.1 的载荷类型是 MaidModels.ModelData（BedrockModel&lt;Mob&gt;/MaidModelInfo/List&lt;Object&gt;）。
     * <p>
     * 1.21.11 RenderState 体系下 MaidModels 已按 26.1 形态重写（不再含 ModelData，模型类型改为
     * {@link EntityMaidModel}，动画改为 {@link IAnimation}&lt;{@link EntityMaidRenderState}&gt;），
     * 故 ModelData 迁移到事件内部，字段语义与 origin 一一对应。
     */
    public static class ModelData {
        private @Nullable EntityMaidModel model;
        private MaidModelInfo info;
        private @Nullable List<IAnimation<EntityMaidRenderState>> animations;

        public ModelData(@Nullable EntityMaidModel model, MaidModelInfo info, @Nullable List<IAnimation<EntityMaidRenderState>> animations) {
            this.model = model;
            this.info = info;
            this.animations = animations;
        }

        @Nullable
        public EntityMaidModel getModel() {
            return model;
        }

        public void setModel(@Nullable EntityMaidModel model) {
            this.model = model;
        }

        public MaidModelInfo getInfo() {
            return info;
        }

        public void setInfo(MaidModelInfo info) {
            this.info = info;
        }

        @Nullable
        public List<IAnimation<EntityMaidRenderState>> getAnimations() {
            return animations;
        }

        public void setAnimations(List<IAnimation<EntityMaidRenderState>> animations) {
            this.animations = animations;
        }
    }
}
