package com.github.tartaricacid.touhoulittlemaid.api.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

import static cn.sh1rocu.touhoulittlemaid.TouhouLittleMaidFabric.*;

/**
 * 为实体信息覆盖层追加自定义内容的事件接口。
 */
public class AddTopInfoEvent {


    public static final Event<Callback> CALLBACK = EventFactory.createWithPhases(Callback.class, callbacks -> event -> {
        for (Callback callback : callbacks) {
            callback.post(event);
        }
    }, HIGHEST, HIGH, Event.DEFAULT_PHASE, LOW, LOWEST);

    public interface Callback {
        void post(AddTopInfoEvent event);
    }
}
