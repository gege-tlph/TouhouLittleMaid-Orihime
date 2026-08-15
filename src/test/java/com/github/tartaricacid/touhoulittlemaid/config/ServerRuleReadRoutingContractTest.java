package com.github.tartaricacid.touhoulittlemaid.config;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ChairConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MiscConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 钉住「世界规则的唯一读口是 {@code ServerRuleConfig.get(...)}」。
 *
 * <p>为什么需要一道机械闸：{@link ServerConfig} 的 spec 有意不注册，这些值上的 {@code XXX.get()}
 * 会抛 {@code Cannot get config value before config is loaded}。**运行期会炸**总比静默读错值好，
 * 但「会炸」只在那条代码路径被走到时才暴露——冷门路径可能几个版本都没人踩。本测试把它提前到构建期。</p>
 *
 * <p>识别依据取自 {@link ServerRuleConfig#values()} **本身**，不是手抄的名字表：
 * 往 {@code values()} 里加一项，本测试自动开始看管它。反过来，反射一旦认不出字段
 * （改名、换类型、挪窝），{@link #everyRuleValueIsAccountedFor()} 那条下限断言会当场红——
 * 枚举型断言必须自带「找到了几个看管对象」的下限，否则识别依据一变就静默零覆盖，而零覆盖恒绿。</p>
 */
class ServerRuleReadRoutingContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path PROJECT_ROOT = Path.of("..", "..");
    private static final Path MAIN_JAVA = PROJECT_ROOT.resolve("src/main/java");
    private static final Path CONFIG_PACKAGE =
            MAIN_JAVA.resolve("com/github/tartaricacid/touhoulittlemaid/config");

    private static Map<ModConfigSpec.ConfigValue<?>, String> ruleFieldNames;

    @BeforeAll
    static void buildSpec() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        ServerConfig.init();

        Map<ModConfigSpec.ConfigValue<?>, String> byIdentity = new IdentityHashMap<>();
        for (Class<?> owner : List.of(MaidConfig.class, ChairConfig.class, MiscConfig.class, ServerConfig.class)) {
            for (Field field : owner.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())
                        || !ModConfigSpec.ConfigValue.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                field.setAccessible(true);
                try {
                    Object value = field.get(null);
                    if (value != null) {
                        byIdentity.put((ModConfigSpec.ConfigValue<?>) value,
                                owner.getSimpleName() + "." + field.getName());
                    }
                } catch (IllegalAccessException exception) {
                    throw new IllegalStateException("读不到配置字段 " + owner.getSimpleName() + "." + field.getName(),
                            exception);
                }
            }
        }
        ruleFieldNames = byIdentity;
    }

    /** 下限断言：{@code values()} 里的每一项都得能反射回一个字段名，否则下面的扫描就是零覆盖。 */
    @Test
    void everyRuleValueIsAccountedFor() {
        List<ModConfigSpec.ConfigValue<?>> values = ServerRuleConfig.values();
        assertTrue(values.size() >= 40, "世界规则集意外缩水到 " + values.size() + " 项，先确认是不是搬漏了");
        List<String> unresolved = new ArrayList<>();
        for (ModConfigSpec.ConfigValue<?> value : values) {
            if (!ruleFieldNames.containsKey(value)) {
                unresolved.add(String.join(".", value.getPath()));
            }
        }
        assertEquals(List.of(), unresolved,
                "这些世界规则值反射不回声明字段，本测试对它们零覆盖");
    }

    /**
     * 有资格持有规则值引用的消费者白名单。
     *
     * <p>配置菜单把规则值**当键用**递给 {@link com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache.Session}
     * ——那边只取 {@code getPath()}（经 {@code ServerRuleConfig.key}）与 {@code getDefault()}，
     * 从不 {@code get()}，所以是安全的。名单必须短且逐个说明理由；
     * 名单里的文件仍然受下面第一条（禁止直接 {@code .get()}）看管，只是豁免第二条。</p>
     *
     * <p>白名单按**路径**匹配：文件改名会让它落回严格检查而不是继续豁免——失效方向是安全的那一侧。</p>
     *
     * <p>{@code GunRecognitionRange}：枪种 → 距离档位的**唯一映射口**，只返回规则值引用、
     * 自己从不 {@code get()}（映射刻意脱离 TaCZ 类型，好被 {@code GunRecognitionRangeTest} 覆盖）。
     * 两个消费者（{@code TaskGunAttack.searchRadius} / {@code TacInnerCompat.canSee}）
     * 都在调用点经 {@code ServerRuleConfig.get(...)} 解析，仍受第一条看管。</p>
     */
    private static final List<String> REFERENCE_HOLDERS_ALLOWED = List.of(
            "compat/cloth/MenuIntegration.java",
            "compat/gun/common/GunRecognitionRange.java");

    /** 第一层：任何地方都不许直接 {@code XXX.get()}——这才是运行期真正会抛的那个写法。 */
    @Test
    void ruleValuesAreNeverReadThroughTheUnregisteredSpec() throws IOException {
        List<String> guarded = guardedFieldNames();
        List<Pattern> patterns = guarded.stream()
                .map(name -> Pattern.compile("\\b" + Pattern.quote(name) + "\\s*\\.\\s*get\\s*\\(\\s*\\)"))
                .toList();
        // 这一层命中数**应当是 0**，所以不能拿命中数当「扫描还活着」的下限——
        // 那正是「零覆盖的测试永远是绿的」。活性判据改成「走到了足够多的文件」，由 scan 内部断言。
        assertEquals(List.of(), scan(patterns, false).offenders(),
                "世界规则不能直接 get()：SERVER spec 未注册，运行期会抛 "
                        + "\"Cannot get config value before config is loaded\"");
    }

    /**
     * 第二层：白名单之外，连**持有引用**都不许——只能作为 {@code ServerRuleConfig.get/key} 的实参出现。
     *
     * <p>这一层是被红测逼出来的：只查 {@code Owner.FIELD.get()} 时，
     * {@code IRangedAttackTask.targetConditionsTest(maid, target, MaidConfig.BOW_RANGE)}
     * 把配置对象**当参数传进去**、在方法体里 {@code configRange.get()}——读点藏在形参后面，扫不出来，
     * 四条攻击任务因此全都漏改，运行期一攻击就炸。**按成因维度定判据**：
     * 成因是「拿到了规则值的引用」，不是「写了 .get()」。</p>
     */
    @Test
    void ruleValueReferencesDoNotEscapeOutsideTheAllowlist() throws IOException {
        List<String> guarded = guardedFieldNames();
        List<Pattern> patterns = guarded.stream()
                .map(name -> Pattern.compile("\\b" + Pattern.quote(name) + "\\b"))
                .toList();
        ScanResult result = scan(patterns, true);
        assertTrue(result.inspected() >= 40,
                "全仓只扫到 " + result.inspected() + " 处规则值引用，识别依据可能已失效");
        assertEquals(List.of(), result.offenders(),
                "规则值在 config 包外只能作为 ServerRuleConfig.get(...) / key(...) 的实参出现；"
                        + "把配置对象传给别的方法会让读点藏进形参。确实需要持有引用的消费者，"
                        + "登记进 REFERENCE_HOLDERS_ALLOWED 并写清它为什么不会 get()");
    }

    private static List<String> guardedFieldNames() {
        List<String> guarded = new ArrayList<>();
        for (ModConfigSpec.ConfigValue<?> value : ServerRuleConfig.values()) {
            String fieldName = ruleFieldNames.get(value);
            if (fieldName != null) {
                guarded.add(fieldName);
            }
        }
        assertTrue(guarded.size() >= 40, "看管对象只有 " + guarded.size() + " 个，识别依据可能已失效");
        return guarded;
    }

    private record ScanResult(List<String> offenders, int inspected) {
    }

    /**
     * @param honourAllowlist true = 第二层（白名单文件豁免）；false = 第一层（谁都不豁免）
     */
    private static ScanResult scan(List<Pattern> patterns, boolean honourAllowlist) throws IOException {
        Pattern sanctioned = Pattern.compile("ServerRuleConfig\\s*\\.\\s*(get|key)\\s*\\(\\s*$");
        List<String> offenders = new ArrayList<>();
        int inspected = 0;
        int filesScanned = 0;
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (file.toAbsolutePath().normalize().startsWith(CONFIG_PACKAGE.toAbsolutePath().normalize())) {
                    // 定义方自己读自己是允许的：spec 的默认值与校验都在这里。
                    continue;
                }
                String unified = file.toString().replace('\\', '/');
                if (honourAllowlist && REFERENCE_HOLDERS_ALLOWED.stream().anyMatch(unified::endsWith)) {
                    continue;
                }
                filesScanned++;
                String[] lines = Files.readString(file, StandardCharsets.UTF_8).split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    String code = stripComment(lines[i]);
                    for (Pattern pattern : patterns) {
                        Matcher matcher = pattern.matcher(code);
                        while (matcher.find()) {
                            inspected++;
                            if (!sanctioned.matcher(code.substring(0, matcher.start())).find()) {
                                offenders.add(PROJECT_ROOT.relativize(file) + ":" + (i + 1)
                                        + "  " + code.strip());
                            }
                        }
                    }
                }
            }
        }
        // 活性判据：先证明这次扫描真的走过了源码树。走了 0 个文件（路径错了、workingDir 变了）
        // 与「一处违规都没有」在结果上完全一样，而后者才是我们想断言的。
        assertTrue(filesScanned >= 500,
                "只走到 " + filesScanned + " 个源文件，扫描没有真的跑起来（PROJECT_ROOT 相对路径？）");
        return new ScanResult(offenders, inspected);
    }

    /**
     * 禁止静态导入规则值。
     *
     * <p>同样是红测逼出来的：{@code import static ...MiscConfig.MAID_FAIRY_BLACKLIST_DIMENSION}
     * 之后代码里写的是**裸的** {@code MAID_FAIRY_BLACKLIST_DIMENSION.get()}，
     * 按 {@code Owner.FIELD} 扫一样扫不到。与其让扫描去追静态导入的别名，不如直接堵住这种写法——
     * 判据越靠近成因越不容易失效。</p>
     */
    @Test
    void ruleValuesAreNeverStaticallyImported() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (file.toAbsolutePath().normalize().startsWith(CONFIG_PACKAGE.toAbsolutePath().normalize())) {
                    continue;
                }
                String[] lines = Files.readString(file, StandardCharsets.UTF_8).split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].strip();
                    if (!line.startsWith("import static ")
                            || !line.contains("touhoulittlemaid.config.")) {
                        continue;
                    }
                    String imported = line.substring("import static ".length()).replace(";", "").strip();
                    String member = imported.substring(imported.lastIndexOf('.') + 1);
                    boolean hitsRule = member.equals("*")
                            ? ruleFieldNames.values().stream()
                            .anyMatch(name -> imported.startsWith(name.substring(0, name.indexOf('.'))
                                    + ".") || imported.contains("." + name.substring(0, name.indexOf('.')) + "."))
                            : ruleFieldNames.values().stream()
                            .anyMatch(name -> name.endsWith("." + member));
                    if (hitsRule) {
                        offenders.add(PROJECT_ROOT.relativize(file) + ":" + (i + 1) + "  " + line);
                    }
                }
            }
        }
        assertEquals(List.of(), offenders,
                "世界规则值不许静态导入：裸名字会让读点绕开 ServerRuleConfig 的扫描");
    }

    /** 去掉整行注释与行内 {@code //} 尾注；行内块注释按最保守的方式只去成对的那种。 */
    private static String stripComment(String line) {
        String trimmed = line.stripLeading();
        if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
            return "";
        }
        String result = line.replaceAll("/\\*.*?\\*/", "");
        int lineComment = result.indexOf("//");
        return lineComment >= 0 ? result.substring(0, lineComment) : result;
    }
}
