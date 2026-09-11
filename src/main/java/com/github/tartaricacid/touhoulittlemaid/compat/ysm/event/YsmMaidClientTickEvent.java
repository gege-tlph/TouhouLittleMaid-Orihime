package com.github.tartaricacid.touhoulittlemaid.compat.ysm.event;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * YSM 模型女仆的客户端 tick。OpenYSM 侧用它触发懒加载缓存预热等纯客户端副作用——
 * TLM 只负责在正确的时机（客户端、这只女仆确实是 YSM 模型）发出这个信号。
 */
@Environment(EnvType.CLIENT)
public class YsmMaidClientTickEvent {
    private final EntityMaid maid;

    public YsmMaidClientTickEvent(EntityMaid maid) {
        this.maid = maid;
    }

    public EntityMaid getMaid() {
        return maid;
    }

    public static final Event<Callback> CALLBACK = EventFactory.createArrayBacked(Callback.class, callbacks -> event -> {
                for (Callback callback : callbacks) {
                    callback.post(event);
                }
            }
    );

    public interface Callback {
        void post(YsmMaidClientTickEvent event);
    }
}
