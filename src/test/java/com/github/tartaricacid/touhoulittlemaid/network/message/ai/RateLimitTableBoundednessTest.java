package com.github.tartaricacid.touhoulittlemaid.network.message.ai;

import cn.sh1rocu.touhoulittlemaid.util.kilt.SoundConsumerStorage;
import com.mojang.blaze3d.audio.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 这三处都是「按无上限的键空间累积、且没有任何清理点」的静态结构。它们不会让服务器立刻出事，
 * 所以也没有任何症状会把人引到这里来——只能靠用例把上界钉住。
 *
 * <p>每条都红测过：撤掉对应的产品改动后，本文件对应的用例会失败。</p>
 */
class RateLimitTableBoundednessTest {
    private static final long HOUR_MS = 3_600_000L;

    @SuppressWarnings("unchecked")
    private static <T> T staticField(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(null);
    }

    private static Map<UUID, Long> checkSiteTable() throws Exception {
        return staticField(CheckSiteConfigPackage.class, "LAST_ACCEPTED");
    }

    private static Set<UUID> checkSiteInFlight() throws Exception {
        return staticField(CheckSiteConfigPackage.class, "IN_FLIGHT");
    }

    private static Map<UUID, Long> voicePreviewTable() throws Exception {
        return staticField(RequestVoicePreviewPackage.class, "LAST_ACCEPTED");
    }

    /** CheckSiteConfigPackage 的冷却常量是 private —— 读它，不为测试放宽产品可见性 */
    private static long checkSiteCooldownMs() throws Exception {
        return (Long) staticField(CheckSiteConfigPackage.class, "COOLDOWN_MS");
    }

    @BeforeEach
    void clearSharedStaticState() throws Exception {
        checkSiteTable().clear();
        checkSiteInFlight().clear();
        voicePreviewTable().clear();
        SoundConsumerStorage.soundConsumerChannels.clear();
    }

    /**
     * 键是玩家 UUID，没有下线钩子也没有淘汰。公共服上「历史上点过这个按钮的独立玩家」
     * 只增不减，所以判据必须是「表不随独立玩家数增长」，而不是「表不大」。
     */
    @Test
    void checkSiteTableDoesNotGrowWithDistinctPlayers() throws Exception {
        long cooldown = checkSiteCooldownMs();
        long now = HOUR_MS;
        for (int i = 0; i < 2000; i++) {
            // 每个玩家各自领先上一个一整个冷却窗口：全部都该被放行，且前一个当场作废
            now += cooldown * 2;
            UUID player = UUID.randomUUID();
            assertTrue(CheckSiteConfigPackage.tryAcquire(player, now), "冷却窗口之外应当放行");
            CheckSiteConfigPackage.release(player);
        }
        assertEquals(1, checkSiteTable().size(),
                "限流表应当只留下仍在冷却窗口内的那一条；留下 2000 条就是无界增长");
    }

    @Test
    void voicePreviewTableDoesNotGrowWithDistinctPlayers() throws Exception {
        long now = HOUR_MS;
        for (int i = 0; i < 2000; i++) {
            now += RequestVoicePreviewPackage.COOLDOWN_MS * 2;
            assertTrue(RequestVoicePreviewPackage.tryAcquire(UUID.randomUUID(), now),
                    "冷却窗口之外应当放行");
        }
        assertEquals(1, voicePreviewTable().size(),
                "限流表应当只留下仍在冷却窗口内的那一条；留下 2000 条就是无界增长");
    }

    /**
     * 清理判据与冷却判据严格互补，所以回收不该改变任何一次放行/拒绝的结果。
     * 没有这两条，「把表清空」会是一个让全部用例都变绿的假修复。
     */
    @Test
    void checkSiteCooldownStillRejectsRepeatWithinWindow() throws Exception {
        long cooldown = checkSiteCooldownMs();
        UUID player = UUID.randomUUID();
        long now = HOUR_MS;
        assertTrue(CheckSiteConfigPackage.tryAcquire(player, now));
        CheckSiteConfigPackage.release(player);
        assertFalse(CheckSiteConfigPackage.tryAcquire(player, now + cooldown - 1),
                "冷却窗口内的重复请求必须仍被拒绝");
        assertTrue(CheckSiteConfigPackage.tryAcquire(player, now + cooldown),
                "冷却窗口外必须放行");
    }

    @Test
    void voicePreviewCooldownStillRejectsRepeatWithinWindow() {
        UUID player = UUID.randomUUID();
        long now = HOUR_MS;
        assertTrue(RequestVoicePreviewPackage.tryAcquire(player, now));
        assertFalse(RequestVoicePreviewPackage.tryAcquire(player, now + RequestVoicePreviewPackage.COOLDOWN_MS - 1),
                "冷却窗口内的重复请求必须仍被拒绝");
        assertTrue(RequestVoicePreviewPackage.tryAcquire(player, now + RequestVoicePreviewPackage.COOLDOWN_MS),
                "冷却窗口外必须放行");
    }

    /**
     * IN_FLIGHT 漏放的后果不是内存，是把这个玩家**永久锁死**在按钮之外——
     * 所以这里钉的是「放过之后一定回得来」。
     */
    @Test
    void inFlightGateBlocksUntilReleasedAndThenReopens() throws Exception {
        UUID player = UUID.randomUUID();
        long now = HOUR_MS;
        assertTrue(CheckSiteConfigPackage.tryAcquire(player, now));
        assertFalse(CheckSiteConfigPackage.tryAcquire(player, now + HOUR_MS),
                "在途探测未释放时，即使冷却早过也必须拒绝");

        CheckSiteConfigPackage.release(player);
        assertTrue(checkSiteInFlight().isEmpty(), "释放之后在途集合必须为空");
        assertTrue(CheckSiteConfigPackage.tryAcquire(player, now + HOUR_MS * 2),
                "释放之后必须重新可用；回不来就是永久锁死");
    }

    /**
     * 探测线程的归还必须挂在 finally 上。{@code reachabilityFailure} 只 catch Exception，
     * Error 会让线程在 {@code server.execute} 入队之前就死掉；归还若写在那个任务里，
     * owner 就永远留在 IN_FLIGHT —— 后果不是内存，是这个玩家**永久**点不动这个按钮。
     */
    @Test
    void probeShellReleasesWhenBodyThrowsError() throws Exception {
        UUID player = UUID.randomUUID();
        assertTrue(CheckSiteConfigPackage.tryAcquire(player, HOUR_MS));
        assertFalse(checkSiteInFlight().isEmpty(), "前提：取得额度后必须在在途集合里");

        assertThrows(StackOverflowError.class,
                () -> CheckSiteConfigPackage.runProbe(player, () -> {
                    throw new StackOverflowError("probe body blew up");
                }),
                "壳不得吞掉 Error —— 吞掉会把故障变成静默");
        assertTrue(checkSiteInFlight().isEmpty(),
                "Error 逃逸时也必须归还额度；不归还 = 该玩家被永久锁死");
    }

    @Test
    void probeShellReleasesOnNormalCompletion() throws Exception {
        UUID player = UUID.randomUUID();
        assertTrue(CheckSiteConfigPackage.tryAcquire(player, HOUR_MS));
        CheckSiteConfigPackage.runProbe(player, () -> {
        });
        assertTrue(checkSiteInFlight().isEmpty(), "正常结束同样必须归还额度");
    }

    /**
     * 移除方 ChannelAccessHandleMixin 注入在原版 {@code execute} 的
     * {@code if (this.channel != null)} 内部，拿不到声道的音效根本不会触发注入，
     * 那个 consumer 就再也没有人会去 remove。判据落在「没人再引用时能不能被回收」。
     */
    @Test
    void soundConsumerStorageReleasesUnreachableConsumers() throws Exception {
        Set<Consumer<Channel>> storage = SoundConsumerStorage.soundConsumerChannels;

        // 必须是**捕获型** lambda：非捕获 lambda 会被 JVM 缓存成单例、恒强可达，
        // 那样这条用例即使在强引用集合上也永远测不出区别。
        Object token = new Object();
        Consumer<Channel> leaked = channel -> token.hashCode();
        storage.add(leaked);
        assertEquals(1, storage.size(), "加进去就应该在");

        leaked = null;
        boolean collected = false;
        for (int attempt = 0; attempt < 50 && !collected; attempt++) {
            System.gc();
            collected = storage.isEmpty();
            if (!collected) {
                Thread.sleep(20);
            }
        }
        assertTrue(collected,
                "漏掉的 consumer 在无人引用后必须可被回收；否则它会一直留在静态集合里");
    }

    /**
     * 弱引用不能弱到把还在用的条目提前清掉：正常路径上 remove 的那个 consumer
     * 在移除的那一刻是作为实参传进来的，必然强可达。
     */
    @Test
    void soundConsumerStorageKeepsReachableConsumers() throws Exception {
        Object token = new Object();
        Consumer<Channel> live = channel -> token.hashCode();
        SoundConsumerStorage.soundConsumerChannels.add(live);
        for (int attempt = 0; attempt < 5; attempt++) {
            System.gc();
            Thread.sleep(10);
        }
        assertTrue(SoundConsumerStorage.soundConsumerChannels.contains(live),
                "仍被引用的 consumer 不得被回收，否则正常路径的 remove 会找不到它");
        assertTrue(SoundConsumerStorage.soundConsumerChannels.remove(live),
                "正常路径必须仍然移除得掉");
    }
}
