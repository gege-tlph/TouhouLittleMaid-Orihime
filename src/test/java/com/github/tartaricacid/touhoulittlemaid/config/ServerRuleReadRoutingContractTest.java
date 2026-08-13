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
     * 唯一允许的用法：把规则值**交给** {@code ServerRuleConfig}。
     *
     * <p>判据不是「别写 {@code XXX.get()}」，而是「除了递给 {@code ServerRuleConfig.get/key}，
     * 不许对规则值做任何别的事」。这条更强的写法是被红测逼出来的：第一版只查 {@code Owner.FIELD.get()}，
     * 而 {@code IRangedAttackTask.targetConditionsTest(maid, target, MaidConfig.BOW_RANGE)}
     * 把配置对象**当参数传进去**、在方法体里 {@code configRange.get()}——读点藏在形参后面，扫不出来，
     * 四条攻击任务因此全都漏改，运行期一攻击就炸。**按成因维度定判据**：
     * 成因是「拿到了规则值的引用」，不是「写了 .get()」。</p>
     */
    @Test
    void ruleValuesAreNeverReadThroughTheUnregisteredSpec() throws IOException {
        List<String> guarded = new ArrayList<>();
        for (ModConfigSpec.ConfigValue<?> value : ServerRuleConfig.values()) {
            String fieldName = ruleFieldNames.get(value);
            if (fieldName != null) {
                guarded.add(fieldName);
            }
        }
        assertTrue(guarded.size() >= 40, "看管对象只有 " + guarded.size() + " 个，识别依据可能已失效");

        List<Pattern> patterns = guarded.stream()
                .map(name -> Pattern.compile("\\b" + Pattern.quote(name) + "\\b"))
                .toList();
        Pattern sanctioned = Pattern.compile("ServerRuleConfig\\s*\\.\\s*(get|key)\\s*\\(\\s*$");

        List<String> offenders = new ArrayList<>();
        int inspected = 0;
        try (Stream<Path> files = Files.walk(MAIN_JAVA)) {
            for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                if (file.toAbsolutePath().normalize().startsWith(CONFIG_PACKAGE.toAbsolutePath().normalize())) {
                    // 定义方自己读自己是允许的：spec 的默认值与校验都在这里。
                    continue;
                }
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

        assertTrue(inspected >= 40,
                "全仓只扫到 " + inspected + " 处规则值引用，扫描本身可能已失效（识别依据变了？）");
        assertEquals(List.of(), offenders,
                "规则值在 config 包外只能作为 ServerRuleConfig.get(...) / key(...) 的实参出现；"
                        + "直接 get() 会在运行期抛 \"Cannot get config value before config is loaded\"，"
                        + "把配置对象传给别的方法则会让读点藏进形参");
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
