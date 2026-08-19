package com.github.tartaricacid.touhoulittlemaid.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 世界规则键的**登记闸**：声明成世界规则的键，必须被某一家店的 {@code values()} 认领。
 *
 * <p><b>这道闸为什么存在</b>：漏登记是**完全静默**的。{@link ServerRuleConfig#get} 在
 * {@code activeValues} 里查不到就回落 {@code value.getDefault()}，于是这个键永远读默认值——
 * 文件里不会有它、菜单保存不会持久化它（{@code applyJson} 按 {@code values()} 逐键处理）、
 * 客户端快照里也没有它。编译、启动、全部 JUnit 与 GameTest 一路正常，**功能从不生效**。</p>
 *
 * <p>本仓库的证伪表把这一族叫「**静默注册面**」，已实证九枚（mixins.json 条目 /
 * {@code fabric-gametest} entrypoint / JEI 等插件 entrypoint / {@code MaidBrain} 的 memory 列表 /
 * 网络 payload 登记 / {@code @MaidManagerDef} …），规律是「**凡新增的类或键要被别处的一张表认领
 * 才生效，那张表就是一处静默面**」。世界规则的 {@code values()} 是第十枚，此前无任何机械保障。</p>
 *
 * <p><b>判据按成因写</b>：成因不是「有没有写 values()」，而是「**这个键被登记进了世界规则的
 * spec，却没有哪家店认领它**」。所以被看管集合从 {@code initServerRule} 的方法体推导——
 * 那个方法就是「把它变成世界规则」的动作本身——而不是从任何一张手抄清单来。</p>
 */
class WorldRuleRegistrationContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path ROOT = Path.of("..", "..");
    private static final Path CONFIG_PACKAGE = ROOT.resolve(Path.of(
            "src", "main", "java", "com", "github", "tartaricacid", "touhoulittlemaid", "config"));

    /**
     * 把键变成**服务端权威**的那些登记入口。
     *
     * <p>三个名字对应三家：{@code initServerRule} = 存档级世界规则（{@code MaidConfig} 等四家，
     * 归 {@code ServerRuleConfig}）；{@code initInstanceRule} = AI 实例级规则
     * （{@code AIConfig}，归 {@code AiServerRuleConfig}）；{@code initValues} = 运维参数
     * （{@code ServerConfig}，同属世界规则文件）。</p>
     *
     * <p>⚠️ <b>{@code initClient} 有意不在表内</b>：那一组是**个人配置**，本来就不该被任何一家
     * 服务端店认领。把它扫进来会造出一批假阳性——而方向是「更严」，所以它会表现成误报而不是漏报，
     * 同样是坏的。加名字前先问一句：这个入口登记出来的键，归服务端管吗？</p>
     */
    private static final List<String> SERVER_AUTHORITATIVE_ENTRIES =
            List.of("void initServerRule", "void initInstanceRule", "void initValues");

    /** {@code XXX_YYY = builder...}——把键变成世界规则的那一笔赋值。 */
    private static final Pattern ASSIGNMENT = Pattern.compile("([A-Z][A-Z0-9_]*)\\s*=\\s*builder\\b");

    /** {@code builder.define("Name", 默认值)}——只取布尔开关那一族。 */
    private static final Pattern DEFINE = Pattern.compile("builder\\.define\\(\\s*\"([^\"]+)\"\\s*,\\s*([^)]*)\\)");

    /**
     * 凡在 {@code initServerRule} 里登记的键，必须出现在某一家店的 {@code values()} 里。
     */
    @Test
    void everyWorldRuleKeyIsClaimedByAStore() throws IOException {
        String claims = valuesBody("ServerRuleConfig.java") + valuesBody("AiServerRuleConfig.java");

        Set<String> registered = new LinkedHashSet<>();
        int specsScanned = 0;
        for (Path file : configSources()) {
            String source = stripComments(Files.readString(file, StandardCharsets.UTF_8));
            for (String entry : SERVER_AUTHORITATIVE_ENTRIES) {
                String body = methodBody(source, entry);
                if (body == null) {
                    continue;
                }
                specsScanned++;
                Matcher matcher = ASSIGNMENT.matcher(body);
                while (matcher.find()) {
                    registered.add(matcher.group(1));
                }
            }
        }

        // 活性与结论正交：「没有漏登记」与「一个键都没认出来」在结果上一模一样。
        assertEquals(6, specsScanned,
                "认出的登记入口不是 6 个（世界规则 4 家 + AI 实例级 1 家 + 运维参数 1 家）——"
                        + "要么有新的一家没进 SERVER_AUTHORITATIVE_ENTRIES，要么识别依据已失效");
        assertTrue(registered.size() >= 40,
                "只认出 " + registered.size() + " 个世界规则键，赋值正则多半被写法变化绕过了");

        List<String> unclaimed = new ArrayList<>();
        for (String field : registered) {
            if (!claims.contains(field)) {
                unclaimed.add(field);
            }
        }
        assertEquals(List.of(), unclaimed,
                "这些键登记成了世界规则，却没有哪家店的 values() 认领——它们会永远读默认值，"
                        + "而且是静默的：文件、菜单保存与客户端快照里都不会有它们");
    }

    /**
     * 实验性开关**必须默认关**——「关掉就等于行为基准」这个承诺全靠它。
     *
     * <p>默认值一旦翻成 {@code true}，这个开关就不再是「可选的偏离」，而是**新的默认行为**，
     * 而移植的唯一目标是与行为基准一致。这条判据钉的正是那一维：把默认值改成 {@code true}
     * 就红，只改文案或注释不红。</p>
     */
    @Test
    void experimentalTogglesDefaultToOff() throws IOException {
        String source = stripComments(Files.readString(
                CONFIG_PACKAGE.resolve(Path.of("subconfig", "ExperimentalConfig.java")), StandardCharsets.UTF_8));
        String body = methodBody(source, "void initServerRule");
        assertTrue(body != null, "ExperimentalConfig 里找不到 initServerRule 的方法体");

        List<String> onByDefault = new ArrayList<>();
        int defines = 0;
        Matcher matcher = DEFINE.matcher(body);
        while (matcher.find()) {
            defines++;
            if (!matcher.group(2).trim().equals("false")) {
                onByDefault.add(matcher.group(1) + " = " + matcher.group(2).trim());
            }
        }
        assertTrue(defines >= 2, "只认出 " + defines + " 个实验性开关，识别依据可能已失效");
        assertEquals(List.of(), onByDefault,
                "实验性开关默认开着——它就不再是「可选的偏离」，而是把行为基准换掉了");
    }

    /** {@code values()} 的方法体；不取整份文件，免得把别处的字段引用也当成认领。 */
    private static String valuesBody(String fileName) throws IOException {
        String source = stripComments(Files.readString(CONFIG_PACKAGE.resolve(fileName), StandardCharsets.UTF_8));
        // ⚠️ 锚点必须钉在**声明**上：按 "values()" 找会先命中
        //   for (ModConfigSpec.ConfigValue<?> value : values())，取到 for 循环的块。
        //   本判据首跑就栽在这里，于是「认领清单」是空的、每个键都被报成漏登记。
        String anchor = "List<ModConfigSpec.ConfigValue<?>> values() {";
        assertEquals(source.indexOf(anchor), source.lastIndexOf(anchor),
                fileName + " 里的 values() 声明不止一处，认领清单该取哪一个已不唯一");
        String body = methodBody(source, anchor);
        assertTrue(body != null, fileName + " 里找不到 values() 的方法体——认领清单的识别依据已失效");
        return body;
    }

    private static List<Path> configSources() throws IOException {
        try (Stream<Path> files = Files.walk(CONFIG_PACKAGE)) {
            return files.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
    }

    /**
     * 取 {@code anchor} 之后第一个大括号块。
     *
     * <p>⚠️ {@code anchor} 必须能把目标与它的同名兄弟区分开——**同一个名字在一份文件里
     * 通常出现多次**（声明一次、被调用若干次），取第一处会拿到别人的块。</p>
     */
    private static String methodBody(String source, String anchor) {
        int at = source.indexOf(anchor);
        if (at < 0) {
            return null;
        }
        int open = source.indexOf('{', at);
        if (open < 0) {
            return null;
        }
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, i + 1);
                }
            }
        }
        return null;
    }

    /** 去掉块注释与行注释——否则只在注释里提到的键会被判为「已认领」。 */
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
    }
}
