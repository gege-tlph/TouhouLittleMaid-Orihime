package com.github.tartaricacid.touhoulittlemaid.arch;

import com.github.tartaricacid.touhoulittlemaid.config.ServerConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 架构级不变量，判据取自**字节码**。自 1.21.11 分支照搬的门禁工具副本（隔离纪律允许共享），
 * 按本分支适配两处：认领清单只有世界规则一份（AI 店属审计 §3.C 未搬，落地时
 * {@link #claimedWorldRuleFields} 要加回 {@code AiServerRuleConfig.values()} 那一半——
 * 只取一份会把 AI 键的正当读法误报成未认领，1.21.11 分支首跑实证过）；
 * 引导走本分支的 {@code ServerConfig.init()}（与 {@code ServerRuleReadRoutingContractTest} 同款）。
 *
 * <p>为什么在既有源码扫描契约测试之外还要这层：源码文本扫描已两次栽在识别依据上
 * （剥注释剥漏 javadoc、按字面量识别改成常量后静默零覆盖）。ArchUnit 读编译产物的
 * 调用图与字段访问，不受注释、格式、字面量写法影响，也不需要猜「该扫哪几个目录」。</p>
 *
 * <p><b>下限断言是必需品</b>：识别依据（类全名、字段类型、包名）一旦变化，规则要因为
 * 「一个看守对象都没找到」而红，而不是安安静静地零覆盖恒绿。本类每条规则都带两条下限——
 * 一条验字段识别、一条验调用识别，分开验是因为它们会各自独立失效。</p>
 *
 * <p><b>已测量并否决的规则（1.21.11 分支 2026-08-14 实测，别再试一遍）</b>：
 * 「仅客户端类不得被服务端可达代码引用」。全仓 1000+ 处、单是 block/entity/command 这些
 * 确定跑在服务端的包就有 96 处——这是**基准架构本身**的形态：它靠 {@code @Environment}
 * 方法级注解与运行期 {@code isClientSide()} 分流，不靠包边界。按忠实移植纪律不会去重构它，
 * 该规则只能冻结成上千行基线，等于一条什么都不说的规则。**不是「以后再做」，是「在本仓库不适用」。</b></p>
 */
class ArchitectureRulesTest {
    private static final String PRODUCTION_PACKAGE = "com.github.tartaricacid.touhoulittlemaid";
    private static final String CONFIG_PACKAGE = PRODUCTION_PACKAGE + ".config";
    private static final String CONFIG_SPEC = ModConfigSpec.class.getName();

    private static JavaClasses production;

    @BeforeAll
    static void importProductionClasses() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        ServerConfig.init();

        production = new ClassFileImporter()
                // 测试源集与产品代码同包前缀，不排掉会把契约测试自己算进违规
                .withImportOption(location -> !location.contains("/classes/java/test/")
                        && !location.contains("/test-classes/"))
                .importPackages(PRODUCTION_PACKAGE);

        assertTrue(production.size() >= 1500,
                "只导入了 " + production.size() + " 个类，导入路径大概率错了——后面所有规则都会假绿");
    }

    /**
     * 世界规则不得裸读——{@link ServerRuleConfig} 认领的键必须经它的读口。
     *
     * <p>两种失败形态，一种崩一种静默：
     * ① 服务端：裸 spec 有意不注册（见 {@code ServerConfig} 类注释），tick 期一读就是
     * {@code IllegalStateException: Cannot get config value before config is loaded}；
     * ② 客户端：裸读拿到的是本机 TOML 的默认值，而不是服务端同步下来的那一份，
     * 专服上「显示的」与「生效的」不是同一个值，**单人档按定义测不出来**。</p>
     *
     * <p>既有的 {@code ServerRuleReadRoutingContractTest} 扫源码文本；这条是全仓字节码版，
     * 间接引用与静态导入的绕过形态在字节码上无所遁形。</p>
     */
    @Test
    void claimedWorldRulesAreNeverReadRaw() {
        Set<String> claimed = claimedWorldRuleFields();
        assertTrue(claimed.size() >= 40,
                "只认出 " + claimed.size() + " 个规则字段，认领清单的反射识别可能已失效");

        List<String> violations = new ArrayList<>();
        int specReadCalls = 0;
        int claimedFieldAccesses = 0;
        for (JavaClass clazz : production) {
            boolean insideConfig = clazz.getPackageName().startsWith(CONFIG_PACKAGE);
            for (JavaCodeUnit codeUnit : clazz.getCodeUnits()) {
                Set<Integer> claimedFieldLines = new TreeSet<>();
                for (JavaFieldAccess access : codeUnit.getFieldAccesses()) {
                    if (claimed.contains(fieldKey(access))) {
                        claimedFieldAccesses++;
                        claimedFieldLines.add(access.getLineNumber());
                    }
                }
                for (JavaMethodCall call : codeUnit.getMethodCallsFromSelf()) {
                    if (!isRawSpecRead(call)) {
                        continue;
                    }
                    specReadCalls++;
                    if (!insideConfig && claimedFieldLines.contains(call.getLineNumber())) {
                        violations.add(clazz.getName() + "#" + codeUnit.getName()
                                + " (行 " + call.getLineNumber() + ")");
                    }
                }
            }
        }

        // 两条下限分别看守两半识别依据：字段类型认错 → 第一条红；ModConfigSpec 全名或方法签名变 → 第二条红
        assertTrue(claimedFieldAccesses >= 20,
                "全仓只找到 " + claimedFieldAccesses + " 处世界规则字段访问，字段识别依据已失效");
        assertTrue(specReadCalls >= 20,
                "全仓只找到 " + specReadCalls + " 处 ModConfigSpec 读调用，调用识别依据已失效");

        assertTrue(violations.isEmpty(),
                "这些地方绕开 ServerRuleConfig 读口直接读世界规则——服务端会抛 IllegalStateException，"
                        + "客户端会读到本机默认值而非服务端同步值：" + violations);
    }

    /**
     * 反过来的一半：经读口读的键，必须真的在认领清单里。
     *
     * <p>{@code ServerRuleConfig.get} 对未认领的键返回默认值兜底（本分支形态：
     * {@code active == null ? value.getDefault() : active}）——「用对了访问器」并不等于「读得对」：
     * 未认领的键在专服上永远读到默认值，且没有任何报错。1.21.11 分支的同型缺陷更凶
     * （回落裸 spec 直接崩服，枪械三键同日实证两次）；TACZ 刀落地时那三键**必须**进
     * {@code values()}，本条规则会在编译产物层面看住它。</p>
     */
    @Test
    void everyRuleReadThroughTheAccessorIsClaimed() {
        Set<String> claimed = claimedWorldRuleFields();
        List<String> violations = new ArrayList<>();
        int recognizedCallSites = 0;

        for (JavaClass clazz : production) {
            if (clazz.getPackageName().startsWith(CONFIG_PACKAGE)) {
                continue;
            }
            for (JavaCodeUnit codeUnit : clazz.getCodeUnits()) {
                Set<Integer> accessorLines = new TreeSet<>();
                for (JavaMethodCall call : codeUnit.getMethodCallsFromSelf()) {
                    if (call.getTargetOwner().getName().equals(ServerRuleConfig.class.getName())
                            && call.getTarget().getName().equals("get")) {
                        accessorLines.add(call.getLineNumber());
                    }
                }
                for (JavaFieldAccess access : codeUnit.getFieldAccesses()) {
                    if (!accessorLines.contains(access.getLineNumber())
                            || !isConfigValueField(access)) {
                        continue;
                    }
                    recognizedCallSites++;
                    if (!claimed.contains(fieldKey(access))) {
                        violations.add(clazz.getName() + "#" + codeUnit.getName()
                                + " -> " + fieldKey(access));
                    }
                }
            }
        }

        // 下限：识别依据一变（读口改名、参数改成非字段）就会静默零覆盖，而零覆盖恒绿
        assertTrue(recognizedCallSites >= 10,
                "全仓只认出 " + recognizedCallSites + " 个经读口的世界规则读点，识别依据已失效");
        assertTrue(violations.isEmpty(),
                "这些键经 ServerRuleConfig 读，却不在 values() 认领清单里——读口只会兜底默认值，"
                        + "专服上显示与生效永远对不上：" + violations);
    }

    /**
     * 被认领的规则 spec，反查回它们的声明字段。
     *
     * <p>⚠️ 恢复锚点（审计 §3.C）：AI 店 {@code AiServerRuleConfig} 落地时，这里必须加回
     * {@code claimed.addAll(AiServerRuleConfig.values())}——只取一份会把 AI 键的正当读法
     * 误报成未认领，1.21.11 分支首跑实证过。</p>
     */
    private static Set<String> claimedWorldRuleFields() {
        List<ModConfigSpec.ConfigValue<?>> claimed = new ArrayList<>(ServerRuleConfig.values());
        Set<String> names = new LinkedHashSet<>();
        for (JavaClass clazz : production) {
            if (!clazz.getPackageName().startsWith(CONFIG_PACKAGE)) {
                continue;
            }
            Class<?> reflected;
            try {
                reflected = Class.forName(clazz.getName(), false, ArchitectureRulesTest.class.getClassLoader());
            } catch (Throwable ignored) {
                continue;
            }
            for (Field field : reflected.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())
                        || !ModConfigSpec.ConfigValue.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value != null && claimed.stream().anyMatch(each -> each == value)) {
                        names.add(reflected.getName() + "." + field.getName());
                    }
                } catch (Throwable ignored) {
                    // 取不到值的字段直接跳过：宁可少认，也不要把无关字段算成世界规则
                }
            }
        }
        return names;
    }

    private static String fieldKey(JavaFieldAccess access) {
        return access.getTargetOwner().getName() + "." + access.getTarget().getName();
    }

    /**
     * 调用点上**直接点名的 spec 常量**：字段类型是 {@code ModConfigSpec.ConfigValue} 且声明在 config 包里。
     *
     * <p>声明者必须在 config 包，是为了排掉合法的间接持有（spec 存进实例字段再交给读口的形态，
     * 静态分析解不出它实际持有哪个键）。只看点名常量，规则才既准确又可解释。</p>
     */
    private static boolean isConfigValueField(JavaFieldAccess access) {
        return access.getTarget().getRawType().getName().startsWith(CONFIG_SPEC)
                && access.getTargetOwner().getPackageName().startsWith(CONFIG_PACKAGE);
    }

    /** {@code ModConfigSpec.ConfigValue#get()} 及其子类的同名读取。 */
    private static boolean isRawSpecRead(JavaMethodCall call) {
        return call.getTargetOwner().getName().startsWith(CONFIG_SPEC)
                && call.getTarget().getName().equals("get")
                && call.getTarget().getRawParameterTypes().isEmpty();
    }
}
