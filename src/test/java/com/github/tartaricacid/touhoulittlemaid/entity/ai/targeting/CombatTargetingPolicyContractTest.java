package com.github.tartaricacid.touhoulittlemaid.entity.ai.targeting;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 统一目标策略必须覆盖每一条战斗路径。
 *
 * <p><b>这条契约看守的是我们相对上游的差异化</b>：上游与代码宿主都<b>没有</b>这套策略，
 * 是我们自己加的（硬安全集 / 条件敌意适配器 / 攻击清单）。2026-08-19 逐行对照照出：
 * 移植到 26.1.2 时它只搬了「出手那一刻」的两道守卫，<b>目标保留</b>那一半整批丢了——
 * 11 处失效判据与 6 个行为任务一处不剩。净效果是女仆会继续锁定、走位、拉弓瞄准一个
 * 不该打的目标，只是每次出手被静默吞掉。</p>
 *
 * <p><b>判据按成因写</b>：成因是「战斗路径上有人绕过策略去决定要不要继续打」，
 * 所以两条断言分别钉住「谁来判定目标失效」与「谁来判定行为可否继续」，
 * 而不是去断言某个症状（那要装模组、进世界才测得到）。</p>
 *
 * <p><b>为什么不能只数「有没有引用 MaidTargetingPolicy」</b>：那个粒度下豁免项几乎和
 * 看守项一样多，清单会腐。这里改用<b>结构签名</b>识别看守对象——
 * 「只在有攻击目标时才运行」的行为，其内存表就写着这件事。</p>
 */
class CombatTargetingPolicyContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path PROJECT_ROOT = Path.of("..", "..");
    private static final Path MAIN_JAVA = PROJECT_ROOT.resolve("src/main/java");

    private static final String INVALIDATOR = "StopAttackingIfTargetInvalid.create";
    private static final String POLICY_CALL = "maid.canAttack(";
    private static final String CONTINUE_CALL = "canContinueTargeting";

    /**
     * 「只在有攻击目标时才运行」的三种结构写法。第三条是因为继承原版 {@code CrossbowAttack}
     * 的任务把内存表写在父类里，前两条签名照不到它——**扫描面要按成因划，不按写法划**。
     */
    private static final List<String> ATTACK_SCOPED_SIGNATURES = List.of(
            "MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT",
            "present(MemoryModuleType.ATTACK_TARGET)",
            "extends CrossbowAttack<");

    /**
     * 具名豁免 + 理由。每一条都必须能回答「行为基准在同一处是怎么写的」——
     * 全部是**基准同样不带**，所以加上去属于超基准改动，不属于移植回归。
     *
     * <p>⚠️ <b>两条断言各用一张表，哪怕内容看着像</b>：它们看管的东西语义不同
     * （一张是「谁来判定目标失效」，一张是「哪些行为只在有目标时才跑」）。
     * 共用一张表会在扩面时静默改变另一处的判定范围——本仓库栽过。</p>
     */
    private static final Map<String, String> EXEMPT_INVALIDATOR = Map.of(
            "TaskGunAttack.java", "枪械的失效判据走 GunCommonUtil.canStartAttacking，行为基准同样不带");

    /** 见 {@link #EXEMPT_INVALIDATOR} 的警告——这张表服务的是「受管行为」那条断言。 */
    private static final Map<String, String> EXEMPT_BEHAVIOR = Map.of(
            "GunAttackStrafingTask.java", "枪械走 GunCommonUtil 自己的判据，行为基准同样不带",
            "GunShootTargetTask.java", "同上，枪械那条链的判据在 GunCommonUtil",
            "MaidSnowballTargetTask.java", "玩雪不是战斗，雪球有意无归属，行为基准同样不带",
            "MaidEmergencyWalkToTarget.java",
            "应战期由 MaidEmergencyCombatManager.tick() 每 tick 用 canContinueTargeting 重校验并撤销，此处再查是冗余；行为基准同样不带");

    /**
     * 每一处「目标是否已失效」的判定都必须问过策略。
     *
     * <p>没有它，一个中途变成受保护的目标（主人当场驯服了那只狼、响应策略被切换、
     * 敌意适配器翻转）会被一直锁定下去，直到它跑出射程或武器条件不满足。</p>
     */
    @Test
    void everyTargetInvalidationPredicateConsultsThePolicy() throws IOException {
        List<String> unguarded = new ArrayList<>();
        int callSites = 0;
        int filesScanned = 0;
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                filesScanned++;
                String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                String name = file.getFileName().toString();
                int at = source.indexOf(INVALIDATOR);
                while (at >= 0) {
                    callSites++;
                    String argument = parenGroupAt(source, source.indexOf('(', at + INVALIDATOR.length() - 1));
                    if (argument != null && !argument.contains(POLICY_CALL) && !EXEMPT_INVALIDATOR.containsKey(name)) {
                        unguarded.add(name + " 的失效判据没有问过敌我策略");
                    }
                    at = source.indexOf(INVALIDATOR, at + 1);
                }
            }
        }
        // 活性与结论正交：命中数为 0 时「全都合规」与「扫描早已失效」在结果上一模一样
        assertTrue(filesScanned >= 1000, "只走过 " + filesScanned + " 个源文件，扫描范围可能已失效");
        assertTrue(callSites >= 13, "只认出 " + callSites + " 个失效判定点，识别依据可能已失效");
        assertEquals(List.of(), unguarded, "这些战斗任务会继续锁定策略已经否决的目标");
    }

    /**
     * 「只在有攻击目标时才运行」的行为，其启动判据必须问过策略。
     *
     * <p>失效判定管的是「还要不要保留这个目标」，本条管的是「这一帧还要不要对它动作」——
     * 走位、瞄准、投掷都在这一侧。两者缺一，症状不同：缺前者是目标不放，缺后者是动作不停。</p>
     */
    @Test
    void everyAttackScopedBehaviorConsultsThePolicy() throws IOException {
        List<String> unguarded = new ArrayList<>();
        List<String> staleExemptions = new ArrayList<>(EXEMPT_BEHAVIOR.keySet());
        int watched = 0;
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                if (ATTACK_SCOPED_SIGNATURES.stream().noneMatch(source::contains)) {
                    continue;
                }
                watched++;
                String name = file.getFileName().toString();
                staleExemptions.remove(name);
                if (!source.contains(CONTINUE_CALL) && !EXEMPT_BEHAVIOR.containsKey(name)) {
                    unguarded.add(name + " 在有攻击目标时动作，却没问过敌我策略");
                }
            }
        }
        assertTrue(watched >= 12, "只认出 " + watched + " 个受管行为，结构签名可能已失效");
        assertEquals(List.of(), unguarded, "这些行为会对策略已经否决的目标继续动作");
        // 豁免表本身也会腐：留着一条指向已不存在的文件，会掩护将来真正的漏网
        assertEquals(List.of(), staleExemptions, "豁免表里这些文件已不在扫描面内，应当删除");
    }

    /** 括号配对取实参跨度——按行截取会在多行 lambda 上把判据切一半。 */
    private static String parenGroupAt(String source, int open) {
        if (open < 0) {
            return null;
        }
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        return null;
    }

    /** 去掉块注释与行注释——否则只在注释里提到的调用会被判为「存在」。 */
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }
}
