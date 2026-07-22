package com.github.tartaricacid.touhoulittlemaid.api.event;


import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * 方便其他附属模将自己的事件注册到 KubeJS 的 MaidEvents 名下
 */
public class RegisterKubeJSEvent {


    public static final Event<Callback> CALLBACK = EventFactory.createArrayBacked(Callback.class, callbacks -> event -> {
        for (Callback callback : callbacks) {
            callback.onRegisterKubeJS(event);
        }
    });

    public interface Callback {
        void onRegisterKubeJS(RegisterKubeJSEvent event);
    }


}
