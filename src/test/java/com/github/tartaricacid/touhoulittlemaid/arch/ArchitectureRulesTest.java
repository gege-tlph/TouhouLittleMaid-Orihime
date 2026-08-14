package com.github.tartaricacid.touhoulittlemaid.arch;

import com.github.tartaricacid.touhoulittlemaid.config.AiServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.WorldRuleTestHarness;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 架构级不变量，判据取自**字节码**。
 *
 * <p>本仓库既有的契约测试绝大多数在扫源码文本，已两次栽在识别依据上：剥注释剥漏了 javadoc、
 * 按按钮字面量识别改成常量后静默零覆盖。ArchUnit 读的是编译产物的调用图与字段访问，
 * 不受注释、格式、字面量写法影响，也不需要我猜「该扫哪几个目录」。</p>
 *
 * <p><b>下限断言是必需品</b>：识别依据（注解全名、字段类型、包名）一旦变化，规则要因为
 * 「一个看守对象都没找到」而红，而不是安安静静地零覆盖恒绿。本类每条规则都带两条下限——
 * 一条验字段识别、一条验调用识别，分开验是因为它们会各自独立失效。</p>
 *
 * <p><b>已测量并否决的规则（别再试一遍）</b>：「仅客户端类不得被服务端可达代码引用」。
 * 2026-08-14 用 ArchUnit 实测：全仓 1000+ 处、单是 block/entity/command 这些确定跑在服务端的包
 * 就有 96 处（`BlockAltar -> ParticleEngine`、`BigBackpack -> EntityModel`……）。这是**基准架构本身**
 * 的形态——它靠 `@Environment` 方法级注解与运行期 `isClientSide()` 分流，而不是靠包边界。
 * 按「忠实移植」纪律我们不会去重构它，所以这条规则只能冻结成一个上千行的基线，
 * 那等于一条什么都不说的规则。**不是「以后再做」，是「在本仓库不适用」。**</p>
 */
class ArchitectureRulesTest {
    private static final String PRODUCTION_PACKAGE = "com.github.tartaricacid.touhoulittlemaid";
    private static final String CONFIG_PACKAGE = PRODUCTION_PACKAGE + ".config";
    private static final String CONFIG_SPEC = ModConfigSpec.class.getName();

    private static JavaClasses production;

    @TempDir
    static Path temporaryDirectory;

    @BeforeAll
    static void importProductionClasses() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        WorldRuleTestHarness.loadDefaults(temporaryDirectory);

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
     * ① 服务端：裸 spec 在集成服务端 tick 期没加载，一读就是
     * {@code IllegalStateException: Cannot get config value before config is loaded}；
     * ② 客户端：裸读拿到的是本机 TOML 的默认值，而不是服务端同步下来的那一份，
     * 于是专服上「显示的」与「生效的」不是同一个值，**单人档按定义测不出来**。</p>
     *
     * <p>2026-08-14 恢复 TACZ 时因此崩了两次。既有的 {@code ServerRuleReadContractTest} 扫源码，
     * 且只扫三个任务目录；这条是全仓字节码版，不依赖我猜对目录。</p>
     */
    @Test
    void claimedWorldRulesAreNeverReadRaw() {
        Set<String> claimed = claimedWorldRuleFields();
        assertTrue(claimed.size() >= 50,
                "只认出 " + claimed.size() + " 个规则字段，认领清单的反射识别可能已失效（两个店都要算）");

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
     * <p>{@code ServerRuleConfig.get} 对未认领的键会回落到裸 spec
     * （{@code active == null ? value.get() : active}），于是「用对了访问器」并不等于「读得到」——
     * 2026-08-14 第二次崩服就栽在这：三个新增的枪械距离键不在 {@code values()} 里，
     * 改用读口后照样崩，只是栈深了一层。</p>
     *
     * <p>既有的 {@code ServerRuleReadContractTest} 同样查这条，但只扫三个任务目录的源码文本；
     * 这里按字节码在全仓查所有直接调用点。</p>
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
                "这些键经 ServerRuleConfig 读，却不在 values() 认领清单里——读口会回落到未加载的裸 spec："
                        + violations);
    }

    /**
     * 被认领的规则 spec，反查回它们的声明字段。
     *
     * <p><b>认领清单是两份，不是一份</b>：{@code ServerRuleConfig.get} 开头就把 AI 规则委派给
     * {@code AiServerRuleConfig}（存档级世界规则 vs 实例级 AI 规则，§17 v2 的两个店）。
     * 只取前者会把 {@code AIConfig.LLM_ENABLED} 这类正当读法误报成未认领——2026-08-14 首跑即中。</p>
     */
    private static Set<String> claimedWorldRuleFields() {
        List<ModConfigSpec.ConfigValue<?>> claimed = new ArrayList<>(ServerRuleConfig.values());
        claimed.addAll(AiServerRuleConfig.values());
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
     * <p>声明者必须在 config 包，是为了排掉合法的间接——{@code ConfigProxySelector} 把 spec 作为
     * 构造参数存成实例字段再交给读口，静态分析解不出它实际持有哪个键，认领与否无从判断。
     * 只看点名常量，规则才既准确又可解释。</p>
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
