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

    private static final String EXTENDS_SNOWBALL = "extends Snowball";
    private static final String KNOCKBACK = "knockback(";
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
     * 玩耍雪球必须保持原版的**伤害量**与**无归属**——这两条不许碰。
     *
     * <p><b>这条契约为什么存在</b>：雪球砸中玩家时「粒子炸开，却没伤害、没击退、没受击闪烁」
     * 是**原版语义**，不是缺陷。原版 {@code Snowball.onHitEntity} 对非烈焰人一律 0 点，
     * 而 {@code Player.hurtServer} 在 {@code amount == 0} 处**直接 return false**
     * （26.1.2 offset 108-115 与 1.21.11 offset 107-114 字节码均实证），
     * **早于**击退所在的 {@code LivingEntity.hurtServer}。打怪则照常击退——
     * {@code LivingEntity} 那一层没有这道闸。</p>
     *
     * <p>本断言防的是下一个人把这个表现当 bug 去「修」：给它加伤害，或把归属加回去——
     * 后者会毁掉「这颗雪球不算女仆挑衅」那一半（砸到别的生物就招来反击）。</p>
     *
     * <p>⚠️ **击退是例外，而且是显式的例外**：世界规则 {@code SnowballKnockback} 开着时，
     * {@code onHitEntity} 会补一次针对玩家的击退。那条开关默认关，关着就是上面说的原版表现；
     * 它的「默认关」与「只补玩家」由另外两条判据钉住（见 {@code WorldRuleRegistrationContractTest}
     * 与 {@link #snowballKnockbackIsOptInAndPlayerOnly()}）。**伤害与归属没有这种例外。**</p>
     *
     * <p>判据按**成因**写：不点名某个方法，而是白名单——子类的覆写面只许是这三个。
     * 白名单的期望集**非空**，所以识别依据一旦失效就是红的，不会像禁止型断言那样静默零覆盖。</p>
     */
    @Test
    void playSnowballKeepsVanillaDamageAndOwnership() throws IOException {
        String source = stripComments(Files.readString(SNOWBALL_TASK, StandardCharsets.UTF_8));

        int at = source.indexOf(EXTENDS_SNOWBALL);
        assertTrue(at >= 0, "没认出继承原版雪球的那个子类，识别依据可能已失效");
        assertEquals(-1, source.indexOf(EXTENDS_SNOWBALL, at + 1),
                "认出的雪球子类不止一个，白名单该管哪一个已不唯一");
        String className = lastIdentifierBefore(source, at);
        assertTrue(!className.isEmpty(), "取不到雪球子类的类名，识别依据可能已失效");
        String body = parenOrBraceBlock(source, source.indexOf('{', at), '{', '}');
        assertTrue(body != null, "取不到雪球子类的类体，识别依据可能已失效");

        List<String> overrides = new ArrayList<>();
        for (String name : declaredMemberNames(body)) {
            if (!name.equals(className)) {
                overrides.add(name);
            }
        }
        overrides.sort(String::compareTo);
        assertEquals(List.of("canHitEntity", "onHitEntity", "tick"), overrides,
                "玩耍雪球的覆写面变了——它只该覆写出膛豁免（tick / canHitEntity）"
                        + "与击退开关（onHitEntity），碰别的就是在改原版语义");

        assertEquals(-1, body.indexOf("hurt("),
                "玩耍雪球动了伤害——原版对非烈焰人是 0 点，改它属超基准改上游行为");

        assertTrue(source.contains("new " + className + "("),
                "没认出雪球的生成点，下面那条「不得设归属」的断言会恒绿");
        assertEquals(-1, source.indexOf("setOwner"),
                "玩耍雪球被设了归属——它会因此算成女仆的攻击，砸到别的生物就招来反击");
    }

    /**
     * 「雪球击退效果」必须是**可选的**，而且**只补玩家**。
     *
     * <p>两条各钉一维：</p>
     * <ul>
     *   <li><b>可选</b>：击退所在的方法体里必须出现那个世界规则键。少了它，开关就从
     *       「可选的偏离」变成新的默认行为，而移植的唯一目标是与行为基准一致
     *       （默认值必须是 {@code false} 那一维在 {@code WorldRuleRegistrationContractTest} 里钉）。</li>
     *   <li><b>只补玩家</b>：怪物在原版那条路径上**本来就会被雪球推开**
     *       （{@code LivingEntity.hurtServer} 没有 {@code amount == 0} 那道闸），
     *       对它们再补一次就是**双份击退**。所以补的判据不是「谁看起来该被推」，
     *       而是「原版那条路径对谁没走完」——这个集合恰好等于 {@code Player}。</li>
     * </ul>
     *
     * <p>力度也一并钉住：原版那一段用的是 {@code knockback(0.4F, …)}（26.1.2 字节码 offset 453）。
     * 照抄原版的值，这个开关才是「把原版本来就会做的事补给玩家」，而不是我们自创一股力。</p>
     */
    @Test
    void snowballKnockbackIsOptInAndPlayerOnly() throws IOException {
        String source = stripComments(Files.readString(SNOWBALL_TASK, StandardCharsets.UTF_8));

        int at = source.indexOf(KNOCKBACK);
        assertTrue(at >= 0, "找不到击退调用，识别依据可能已失效");
        assertEquals(-1, source.indexOf(KNOCKBACK, at + 1),
                "击退调用不止一处，下面那些断言该管哪一处已不唯一");

        String body = enclosingMethodBody(source, at);
        assertTrue(body != null, "取不到击退所在的方法体，识别依据可能已失效");
        assertTrue(body.contains("SNOWBALL_KNOCKBACK"),
                "击退没有被那个世界规则守着——它会变成默认行为，而不是可选的偏离");
        assertTrue(body.contains("instanceof Player"),
                "击退没有限定在玩家上——怪物在原版那条路径上已经被推过一次了，再补就是双份");
        assertTrue(source.contains("KNOCKBACK_STRENGTH = 0.4F"),
                "击退力度不再是原版那一段用的 0.4F——这个开关就不是「把原版会做的事补给玩家」了");
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
     * 类体内**顶层成员**（方法与构造器）的名字，按声明顺序。
     *
     * <p>不认 {@code @Override}——**Java 覆写不依赖注解**，认它等于把漏写注解的覆写放过去。
     * 改认「类体内深度为 1 的那些块」，谁在那儿开了个块谁就是一个成员。</p>
     */
    private static List<String> declaredMemberNames(String classBody) {
        List<String> names = new ArrayList<>();
        int depth = 0;
        for (int i = 0; i < classBody.length(); i++) {
            char c = classBody.charAt(i);
            if (c == '{') {
                if (depth == 1) {
                    String name = memberNameOf(headerBefore(classBody, i));
                    if (name != null) {
                        names.add(name);
                    }
                }
                depth++;
            } else if (c == '}') {
                depth--;
            }
        }
        return names;
    }

    /** 成员头部里参数表之前的那个标识符——没有参数表就不是方法/构造器。 */
    private static String memberNameOf(String header) {
        int paren = header.indexOf('(');
        if (paren < 0) {
            return null;
        }
        String name = lastIdentifierBefore(header, paren);
        return name.isEmpty() ? null : name;
    }

    /** index 之前（跳过空白）的那个标识符。 */
    private static String lastIdentifierBefore(String text, int index) {
        int end = index;
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        int start = end;
        while (start > 0 && Character.isJavaIdentifierPart(text.charAt(start - 1))) {
            start--;
        }
        return text.substring(start, end);
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
