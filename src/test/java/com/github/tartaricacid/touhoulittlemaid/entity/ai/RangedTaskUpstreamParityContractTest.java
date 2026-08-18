package com.github.tartaricacid.touhoulittlemaid.entity.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 应战期弓/弩的手感，必须与上游同名任务在三个输入上一致。
 *
 * <p><b>为什么有这条</b>：B1 给应战活动接的是两条 {@code *AnyItemTask} 变体（为了支持模组远程武器
 * 与统一的敌我判定）。它们是照上游任务改写的，但**改写时把上游读的三样东西丢了**，
 * 于是应战期的弓/弩手感明显不如上游——2026-08-18 用户实机判定「不如原版」，逐条比对后坐实：</p>
 *
 * <ol>
 *   <li><b>快速射击附魔</b>：上游按等级缩短蓄力门槛，变体恒用固定值 → 附魔弓与白板弓射速一样；</li>
 *   <li><b>射击间隔属性</b>：上游读 {@code MAID_SHOOT_COOLDOWN}，变体恒用构造参数 →
 *       那条专为调射速存在的属性在应战期整个失效；</li>
 *   <li><b>走位阈值</b>：上游按**每把武器自己的**射程取，变体用一个固定值 →
 *       弩本该 1.6 格才后退却 3.2 格就退、本该 4 格压上却拖到 8 格。</li>
 * </ol>
 *
 * <p><b>判据为什么成对写</b>：只断言变体读了某个符号，会在上游哪天换掉它时静默失去意义
 * （变体读着一个上游已经不用的东西，而我们以为还对得上）。成对断言把「上游还在用它」
 * 也钉住，不成立时红给下一个人看，逼他回去重新推导，而不是让判据悄悄退化。</p>
 *
 * <p>⚠️ <b>强度只到接线</b>：这里验的是「那三个输入确实被读了」，不是「手感真的对」。
 * 手感只有实机能判——这条测试的作用是**防止再次悄悄丢掉**，不是替代实机。</p>
 */
class RangedTaskUpstreamParityContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path TASKS = Path.of("..", "..").resolve(
            "src/main/java/com/github/tartaricacid/touhoulittlemaid/entity/ai/brain/task");

    private static final String UPSTREAM_SHOOT = "MaidShootTargetTask.java";
    private static final String VARIANT_SHOOT = "MaidShootTargetAnyItemTask.java";
    private static final String UPSTREAM_STRAFE = "MaidAttackStrafingTask.java";
    private static final String VARIANT_STRAFE = "MaidAttackStrafingAnyItemTask.java";

    /** 附魔等级越高拉弓越快——丢了它，附魔弓与白板弓射速一样。 */
    @Test
    void variantShootTaskReadsQuickChargeLikeUpstream() throws IOException {
        assertPairedUse(UPSTREAM_SHOOT, VARIANT_SHOOT, "QUICK_CHARGE",
                "应战期的弓不再受快速射击附魔影响——附魔白附");
    }

    /** 射击间隔要读属性——丢了它，那条属性在应战期无效。 */
    @Test
    void variantShootTaskReadsShootCooldownAttributeLikeUpstream() throws IOException {
        assertPairedUse(UPSTREAM_SHOOT, VARIANT_SHOOT, "MAID_SHOOT_COOLDOWN",
                "应战期的射击间隔不再受 MAID_SHOOT_COOLDOWN 属性影响");
    }

    /** 走位阈值要按每把武器自己的射程取——丢了它，弩的进退距离全错。 */
    @Test
    void variantStrafingTaskUsesPerWeaponRangeLikeUpstream() throws IOException {
        assertPairedUse(UPSTREAM_STRAFE, VARIANT_STRAFE, "getDefaultProjectileRange",
                "应战期的走位阈值退回固定值——弩会在错误的距离上进退");
    }

    /**
     * 上游任务与我们的变体必须都用到 {@code symbol}。
     *
     * <p>先断言上游还在用它（否则本条判据的前提已经不成立，应当回去重新推导），
     * 再断言变体没把它丢掉。</p>
     */
    private static void assertPairedUse(String upstreamFile, String variantFile,
                                        String symbol, String consequence) throws IOException {
        String upstream = readStripped(upstreamFile);
        String variant = readStripped(variantFile);

        assertTrue(upstream.contains(symbol),
                upstreamFile + " 里已经找不到 " + symbol + " —— 上游换了做法，"
                + "本条判据的前提不再成立，需要回去重新推导应战变体该照抄什么，"
                + "**不要直接删掉这条测试**");
        assertTrue(variant.contains(symbol),
                variantFile + " 没有读 " + symbol + " —— " + consequence
                + "。它是照 " + upstreamFile + " 改写的，改写时不许把上游读的输入丢掉");
    }

    /** 剥注释后再比：符号可能只出现在注释里，那不算「读了它」。 */
    private static String readStripped(String fileName) throws IOException {
        Path path = TASKS.resolve(fileName);
        assertTrue(Files.isRegularFile(path), "找不到 " + path + " —— 任务类挪窝了，本测试已失效");
        String source = Files.readString(path, StandardCharsets.UTF_8);
        // 活性判据与结论正交：文件读空了也会让 contains 全假，看起来像「符号被丢了」
        assertTrue(source.length() > 1000, fileName + " 只读到 " + source.length() + " 字节，读文件这一步就不对");
        String noBlock = source.replaceAll("(?s)/\\*.*?\\*/", " ");
        return noBlock.replaceAll("(?m)//.*$", " ");
    }
}
