package cn.sh1rocu.touhoulittlemaid.util.kilt;

import com.mojang.blaze3d.audio.Channel;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

// 来自 Kilt
public class SoundConsumerStorage {
    // 声音引擎是 lambda hell，因此我们必须使用此存储来确保 ChannelAccessHandleMixin 中的通道访问执行注入实际上会在正确的位置运行。我们也不能包装消费者，因为否则，尝试做同样事情的其他一些模组将导致我们或他们失败。
    public static final Set<Consumer<Channel>> soundConsumerChannels = Collections.synchronizedSet(new HashSet<>());
}