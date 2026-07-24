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
/**
 * 可取消的女仆渲染事件，允许客户端扩展替换模型信息与动画集合。
 */
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
     * 本次渲染使用的可变模型数据。模型与动画允许为空，模型信息始终存在。
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
