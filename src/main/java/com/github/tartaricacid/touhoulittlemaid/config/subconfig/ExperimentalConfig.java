package com.github.tartaricacid.touhoulittlemaid.config.subconfig;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ExperimentalConfig {
    private static final String TRANSLATE_KEY = "config.touhou_little_maid.experimental";

    /**
     * 跟随手感：更早开始跟随、追赶更快、把传送留给更远的距离。
     *
     * <p><b>默认开</b>（用户 2026-08-20 定，与 26.1.2 对齐），所以本树开箱即用的跟随行为
     * **不再等于上游**——关掉它才回到 origin/1.21.1 那套起跟距离 / 速度 / 传送阈值。</p>
     *
     * <p>⚠️ <b>键留在 [experimental] 段，不跟 26.1.2 搬进 [maid]</b>：本分支已公开发布，
     * 而改段名等于换了一个键，旧存档里服主设过的值会被新默认值静默取代；
     * 本分支又没有 26.1.2 那套 inheritRenamedKey 搬家机制。段名只是组织方式，
     * 玩家可观察行为一致即达成对齐目标——为一次纯组织性的改名去冒静默丢值的风险不划算。</p>
     */
    public static ModConfigSpec.BooleanValue SMOOTH_FOLLOW;

    /**
     * 让女仆玩雪扔出的雪球把**玩家**也推开。
     *
     * <p>默认关：原版雪球对非烈焰人是 0 伤害，而玩家一侧在「伤害为 0」处就把整次受击丢掉了，
     * 比击退那一段早得多；怪物一侧没有这道闸，所以雪球一直推得动怪、推不动人。
     * 这个不对称观感上古怪，于是做成开关——想要对称的服打开它，想要原版的什么都不用做。</p>
     */
    public static ModConfigSpec.BooleanValue SNOWBALL_KNOCKBACK;

    public static void init(ModConfigSpec.Builder builder) {
        builder.translation(TRANSLATE_KEY).push("experimental");

        builder.comment(
                        "Experimental: start following earlier, move faster, and reserve teleporting for a larger distance",
                        "Disabled preserves the origin/1.21.1 follow behavior")
                .translation(TRANSLATE_KEY + ".smooth_follow");
        SMOOTH_FOLLOW = builder.define("SmoothFollow", true);

        builder.comment(
                        "Experimental: the maid's play snowball knocks players back, the way it already knocks mobs back",
                        "Disabled preserves vanilla, where a 0 damage hit on a player is dropped before knockback")
                .translation(TRANSLATE_KEY + ".snowball_knockback");
        SNOWBALL_KNOCKBACK = builder.define("SnowballKnockback", false);

        builder.pop();
    }

    private ExperimentalConfig() {
    }
}
