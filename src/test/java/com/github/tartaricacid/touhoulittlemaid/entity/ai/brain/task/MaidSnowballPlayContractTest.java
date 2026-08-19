package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 打雪仗的两条契约，都是 2026-08-19 用户实机报出来的。
 *
 * <p>两条缺陷**行为基准与上游同样有**（三棵树逐字相同，已实查），所以都不是移植回归；
 * 但玩雪是上游功能、威胁响应是我们自己造的，二者相撞那条因此没人会来替我们修。</p>
 */
class MaidSnowballPlayContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path PROJECT_ROOT = Path.of("..", "..");
    private static final Path BRAIN_TASKS = PROJECT_ROOT.resolve(
            "src/main/java/com/github/tartaricacid/touhoulittlemaid/entity/ai/brain/task");
    private static final Path SNOWBALL_TASK = BRAIN_TASKS.resolve("MaidSnowballTargetTask.java");

    private static final String ERASE = "eraseMemory(MemoryModuleType.ATTACK_TARGET)";
    private static final String READ = "getMemory(MemoryModuleType.ATTACK_TARGET)";

    private static final List<String> CONTROL_FLOW = List.of(
            "if", "else", "for", "while", "switch", "case", "default", "do", "try", "catch", "finally", "synchronized");

    /**
     * 玩耍用的雪球必须从**眼高**出手，不能从脚底。
     *
     * <p>触发玩雪的前提正是女仆站在雪片上，从 {@code getY()}（脚底）出手会让雪球在贴地处
     * 出生、弹道极低，落在目标前方的地上——这个功能因此**从来没打中过任何人**，
     * 而症状表现为「打到人身上没有任何反应」，极易被误诊成受伤处理的问题（本轮就误诊过一次）。
     * 原版基于射手的构造器取的正是 {@code getEyeY() - 0.1}（26.1.2 字节码实证：
     * {@code ThrowableItemProjectile(Level, LivingEntity, ItemStack)} 内联 getX/getEyeY-0.1/getZ）。</p>
     *
     * <p>识别依据取「传坐标三元组的那一次 {@code super(...)}」——按 <b>第一个</b> {@code super(}
     * 找会拿到本任务自己那个 {@code Behavior} 构造器，首跑即栽在此。</p>
     */
    @Test
    void playSnowballLeavesFromEyeHeight() throws IOException {
        String source = stripComments(Files.readString(SNOWBALL_TASK, StandardCharsets.UTF_8));
        List<String> spawnCalls = new ArrayList<>();
        int at = source.indexOf("super(");
        while (at >= 0) {
            String args = parenGroupAt(source, source.indexOf('(', at + "super".length()));
            if (args != null && args.contains("getX()")) {
                spawnCalls.add(args);
            }
            at = source.indexOf("super(", at + 1);
        }
        assertEquals(1, spawnCalls.size(),
                "认出的坐标型 super(...) 不是一处，识别依据可能已失效：" + spawnCalls);
        assertTrue(spawnCalls.get(0).contains("getEyeY()"),
                "玩耍雪球的生成高度不是眼高——从脚底出手会直接砸在目标前方的地上：" + spawnCalls.get(0));
    }

    /**
     * 共享槽 {@code ATTACK_TARGET} 只能由放它进去的人擦掉。
     *
     * <p><b>成因</b>：这个槽被威胁响应、敌我策略与各战斗行为共用，而打雪仗只是借它存玩伴。
     * 无条件擦除会擦掉**别人刚写进去的**东西——取证得机制是「威胁来了 → 应战写入攻击者并切换
     * 活动 → 雪仗任务因活动切换被 stop → 擦掉应战刚设好的目标 → 应战下一 tick 发现目标
     * 对不上而自我撤销 → 雪仗夺回控制」。症状为用户实机报告，机制链为逐行取证，未插桩实证。</p>
     *
     * <p>判据按成因写：**擦之前必须先读**（读了才谈得上判断槽里的还是不是自己放的），
     * 所以断言擦除所在的方法体内必须同时出现读取。这条对全部 brain task 生效，不只雪仗——
     * 同一个坑任何借用共享槽的行为都能踩。</p>
     */
    @Test
    void sharedAttackTargetIsErasedOnlyByWhoeverSetIt() throws IOException {
        List<String> unguarded = new ArrayList<>();
        int eraseSites = 0;
        int filesScanned = 0;
        try (Stream<Path> files = Files.walk(BRAIN_TASKS)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (file.getFileName().toString().endsWith("GameTest.java")) {
                    continue;
                }
                filesScanned++;
                String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
                int at = source.indexOf(ERASE);
                while (at >= 0) {
                    eraseSites++;
                    String body = enclosingMethodBody(source, at);
                    if (body == null || !body.contains(READ)) {
                        unguarded.add(file.getFileName() + " 无条件擦除了共享槽 ATTACK_TARGET");
                    }
                    at = source.indexOf(ERASE, at + 1);
                }
            }
        }
        assertTrue(filesScanned >= 30, "只走过 " + filesScanned + " 个 brain task 文件，扫描范围可能已失效");
        assertTrue(eraseSites >= 1, "一处擦除点都没认出来，识别依据可能已失效");
        assertEquals(List.of(), unguarded, "这些行为会擦掉别人写进共享槽的状态");
    }

    /**
     * 从 index 往外找到**方法体**——中途要穿过 if/for/lambda 这些块。
     *
     * <p>只找最近那个左大括号会停在守卫自己的 {@code if (...) {} 上，
     * 把正确实现判成违规（本测试首跑就栽在这里）。</p>
     */
    private static String enclosingMethodBody(String source, int index) {
        int from = index;
        while (true) {
            int open = enclosingBlockStart(source, from);
            if (open < 0) {
                return null;
            }
            if (!isNestedBlockHeader(headerBefore(source, open))) {
                return parenOrBraceBlock(source, open, '{', '}');
            }
            from = open - 1;
        }
    }

    /** 从 index 往回找最近一个尚未配对的左大括号。 */
    private static int enclosingBlockStart(String source, int index) {
        int depth = 0;
        for (int i = index; i >= 0; i--) {
            char c = source.charAt(i);
            if (c == '}') {
                depth++;
            } else if (c == '{') {
                if (depth == 0) {
                    return i;
                }
                depth--;
            }
        }
        return -1;
    }

    /** 左大括号之前那一段头部文本（回溯到上一个语句边界为止）。 */
    private static String headerBefore(String source, int open) {
        int start = open;
        while (start > 0) {
            char previous = source.charAt(start - 1);
            if (previous == ';' || previous == '{' || previous == '}') {
                break;
            }
            start--;
        }
        return source.substring(start, open).trim();
    }

    /** 这个块是控制流块或 lambda 体吗——是就得继续往外找方法体。 */
    private static boolean isNestedBlockHeader(String header) {
        if (header.endsWith("->")) {
            return true;
        }
        for (String keyword : CONTROL_FLOW) {
            if (header.equals(keyword) || header.startsWith(keyword + " ") || header.startsWith(keyword + "(")) {
                return true;
            }
        }
        return false;
    }

    private static String parenGroupAt(String source, int open) {
        return open < 0 ? null : parenOrBraceBlock(source, open, '(', ')');
    }

    private static String parenOrBraceBlock(String source, int open, char openChar, char closeChar) {
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == openChar) {
                depth++;
            } else if (c == closeChar) {
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
