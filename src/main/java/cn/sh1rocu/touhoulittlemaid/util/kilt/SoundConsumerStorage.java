package cn.sh1rocu.touhoulittlemaid.util.kilt;

import com.mojang.blaze3d.audio.Channel;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

// From Kilt
public class SoundConsumerStorage {
    // The sound engine is lambda hell, and so we have to use this storage to be able to ensure that the channel access execute inject in
    // ChannelAccessHandleMixin will actually be run in the correct places. We also can't wrap the consumer, because otherwise,
    // some other mod that tries to do the same thing will cause either us or them to fail.
    //
    // 弱引用集，而不是上游那个 HashSet：移除方 ChannelAccessHandleMixin 注入在原版
    // `execute` 的 `if (this.channel != null) consumer.accept(channel)` **内部**，所以
    // 任何拿不到声道的音效（声道池占满时很常见）根本不会触发注入，那个 consumer 就永远
    // 留在集合里。调换 `&&` 的顺序救不了这一半——accept 压根没被调用过。
    //
    // 弱键是安全的：consumer 在排队期间由声音执行器强引用着，移除时又作为实参传进来，
    // 两个时刻都强可达，不会被提前清掉；只有那些**已经没人再引用**的漏网条目会被回收，
    // 而那种条目按定义不可能再被 remove 找到。add/remove/contains 语义不变。
    public static final Set<Consumer<Channel>> soundConsumerChannels =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
}