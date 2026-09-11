package com.github.tartaricacid.touhoulittlemaid.compat.ysm.event;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * 玩家在 TLM 女仆 GUI 里点击"选择 YSM 模型"按钮时触发。OpenYSM 客户端监听本事件，
 * 打开它自己的模型选择界面——TLM 只管"有人想选 YSM 模型了"，不知道也不需要知道
 * 那个界面长什么样。
 */
@Environment(EnvType.CLIENT)
public class OpenYsmMaidScreenEvent {
    private final EntityMaid maid;

    public OpenYsmMaidScreenEvent(EntityMaid maid) {
        this.maid = maid;
    }

    public EntityMaid getMaid() {
        return maid;
    }

    public static final Event<Callback> CALLBACK = EventFactory.createArrayBacked(Callback.class, callbacks -> event -> {
        for (Callback callback : callbacks) {
            callback.post(event);
        }
    });

    public interface Callback {
        void post(OpenYsmMaidScreenEvent event);
    }
}
