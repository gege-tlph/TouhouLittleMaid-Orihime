package com.github.tartaricacid.touhoulittlemaid.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 每个配置键都必须有人真的读它。
 *
 * <p><b>这道闸为什么存在</b>：宿主从 1.21.1 迁到 26.1 时把
 * {@code MaidEatenReturnContainerList} 的消费链整条丢了，只留下配置键与配置菜单里那一栏，
 * 于是玩家能在菜单里配「食物 → 归还容器」，配了却永远不生效。
 * 编译、启动、全部 JUnit 与 GameTest 都照不出来——**声明与菜单都还在，只有行为没了**。
 * 本项目自己的产品判据写着「一个能设置却不起作用的选项，比没有这个选项更糟」，
 * 那条判据此前没有任何机械保障。</p>
 *
 * <p><b>判据按成因写</b>：成因是「除了声明它、把它列进认领清单、把它摆进配置菜单之外，
 * 还有没有人拿到过这个键」。故：
 * <ul>
 *   <li>不要求写成 {@code KEY.get()}——<b>读点可能藏在形参后面</b>
 *       （{@code addTips("...", ENABLE_GOLDEN_APPLE_TIP, ...)} 把键对象递进 helper，
 *       helper 里才取值）。按 {@code .get()} 抽会误报 15 个。</li>
 *   <li>但落在 {@code config/} 包内的引用只有被 {@code get(…)} 包住才算真读——
 *       {@code ServerRuleConfig} 里的 {@code if (value == KEY)} 是**类型分派**，
 *       不是消费。不排除它，这道闸对它本该抓的那个缺陷恰好是绿的。</li>
 *   <li>纯登记行（整行只有 {@code Owner.KEY,}）是认领清单，不算消费。</li>
 * </ul>
 *
 * <p><b>红测已做</b>：把 {@code MaidItemManager} 里那处读点抽掉，本类当场判红
 * （证伪表：「把改动前的值代进去，断言会不会红？不会红就是选错了维度」）。</p>
 */
class ConfigKeyHasConsumerContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path ROOT = Path.of("..", "..");
    private static final Path SOURCE_ROOT = ROOT.resolve(Path.of("src", "main", "java"));

    /** {@code public static ModConfigSpec.XxxValue<泛型可嵌套> KEY;}——泛型不兜住会漏掉 21 个键。 */
    private static final Pattern DECL = Pattern.compile(
            "public\\s+static\\s+ModConfigSpec\\.[\\w.]+(?:<[^;=]*>)?\\s+([A-Z][A-Z0-9_]*)\\s*[;=]");

    /** 配置菜单：它摆出这个键是「让人能改」，不是「有人在读」。 */
    private static final String MENU_PACKAGE = "/compat/cloth/";

    /**
     * 允许没有消费点的键，**必须写明理由**——理由才是下一个人复用的东西。
     */
    private static final Map<String, String> ALLOWED_WITHOUT_CONSUMER = new LinkedHashMap<>(Map.of(
            "ENABLE_SCRIPT_BOOK_TIP",
            "上游死配置：origin/1.21.1、origin/26.1、port/1.21.11-fabric 与本树四棵树里都是"
                    + "「声明了、进了认领清单、从来没人读」。不是本分支的移植回归，故不修也不删"
                    + "（无症状不改）；一旦将来有人给它接上消费点，把这条从表里删掉即可。"));

    @Test
    void everyConfigKeyIsReadBySomeone() throws IOException {
        List<Path> sources = javaSources();
        Map<Path, String> srcs = new LinkedHashMap<>();
        for (Path p : sources) {
            srcs.put(p, Files.readString(p, StandardCharsets.UTF_8));
        }

        Map<String, Path> declared = new LinkedHashMap<>();
        for (Map.Entry<Path, String> e : srcs.entrySet()) {
            if (!normalize(e.getKey()).contains("/config/")) {
                continue;
            }
            Matcher m = DECL.matcher(e.getValue());
            while (m.find()) {
                declared.put(m.group(1), e.getKey());
            }
        }

        // 活性判据与结论正交：「没有违规」与「扫描根本没跑」在结果上一模一样，
        // 故这两个下限必须来自与结论无关的量（走过多少文件、认出多少键）。
        assertTrue(srcs.size() > 1200,
                "扫描面异常：只走了 " + srcs.size() + " 个源文件，扫描器多半坏了");
        assertTrue(declared.size() >= 80,
                "只认出 " + declared.size() + " 个配置键，声明正则多半又被写法变化绕过了");

        Set<String> orphans = new LinkedHashSet<>();
        for (Map.Entry<String, Path> e : declared.entrySet()) {
            if (consumersOf(e.getKey(), e.getValue(), srcs).isEmpty()) {
                orphans.add(e.getKey());
            }
        }

        Set<String> unexpected = new LinkedHashSet<>(orphans);
        unexpected.removeAll(ALLOWED_WITHOUT_CONSUMER.keySet());
        assertTrue(unexpected.isEmpty(),
                "这些配置键在菜单里能改，却没有任何代码读它——玩家配了不生效：" + unexpected);

        // 允许表不许腐化：键已经被接上消费点了就要从表里删掉，否则表会慢慢变成免罪符
        Set<String> staleAllowances = new LinkedHashSet<>(ALLOWED_WITHOUT_CONSUMER.keySet());
        staleAllowances.removeAll(orphans);
        assertTrue(staleAllowances.isEmpty(),
                "允许表里的这些键现在已经有消费点了，把它们从表里删掉：" + staleAllowances);
    }

    /** 归还容器那条链的接线判据：世界规则读点必须落在进食完成路径上，而不是别处。 */
    @Test
    void eatenContainerRuleIsReadOnTheEatCompletionPath() throws IOException {
        Path manager = SOURCE_ROOT.resolve(Path.of("com", "github", "tartaricacid",
                "touhoulittlemaid", "entity", "passive", "MaidItemManager.java"));
        Path maid = SOURCE_ROOT.resolve(Path.of("com", "github", "tartaricacid",
                "touhoulittlemaid", "entity", "passive", "EntityMaid.java"));
        String managerSrc = Files.readString(manager, StandardCharsets.UTF_8);
        String maidSrc = Files.readString(maid, StandardCharsets.UTF_8);

        String body = methodBody(managerSrc, "returnFoodContainer");
        assertTrue(body != null, "MaidItemManager 里找不到 returnFoodContainer 的方法体");
        assertTrue(body.contains("MAID_EATEN_RETURN_CONTAINER_LIST"),
                "returnFoodContainer 不读 MaidEatenReturnContainerList，那条配置又成了摆设");
        assertTrue(body.contains("getCraftingRemainder"),
                "returnFoodContainer 丢了合成剩余物那条通道（碗 / 桶 / 瓶）");

        String complete = methodBody(maidSrc, "completeUsingItem");
        assertTrue(complete != null, "EntityMaid 里找不到 completeUsingItem 的方法体");
        assertTrue(complete.contains("returnFoodContainer"),
                "completeUsingItem 没有调用 returnFoodContainer——写了实现却没人调，等于没修");
        // 顺序：必须在 backCurrentHandItemStack 之前，否则手上的剩余物已被收走、判据的输入就变了
        assertTrue(complete.indexOf("returnFoodContainer") < complete.indexOf("backCurrentHandItemStack"),
                "returnFoodContainer 必须早于 backCurrentHandItemStack");
    }

    /**
     * 大括号配对截出方法体，并**剥掉注释**。
     * <p>不截方法体就等于在整份文件里找；不剥注释则顺序断言会被注释里的同名词骗过——
     * 本类首跑即栽在后者：{@code completeUsingItem} 里有一行注释先提到了
     * {@code backCurrentHandItemStack}，于是「A 早于 B」被读成假。两个坑本仓库都记着。
     */
    private static String methodBody(String src, String method) {
        int i = src.indexOf(" " + method + "(");
        if (i < 0) {
            return null;
        }
        int open = src.indexOf('{', i);
        if (open < 0) {
            return null;
        }
        int depth = 0;
        for (int j = open; j < src.length(); j++) {
            char c = src.charAt(j);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return stripComments(src.substring(open, j + 1));
                }
            }
        }
        return null;
    }

    private static String stripComments(String s) {
        return s.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\n]*", " ");
    }

    private static List<Path> consumersOf(String key, Path declaredIn, Map<Path, String> srcs) {
        Pattern word = Pattern.compile("\\b" + Pattern.quote(key) + "\\b");
        Pattern registrationLine = Pattern.compile("^\\s*[\\w.]*\\b" + Pattern.quote(key) + "\\b\\s*,?\\s*$");
        Pattern getter = Pattern.compile("get\\s*\\(\\s*[\\w.]*\\b" + Pattern.quote(key) + "\\b\\s*\\)");
        List<Path> hits = new ArrayList<>();
        for (Map.Entry<Path, String> e : srcs.entrySet()) {
            String path = normalize(e.getKey());
            if (e.getKey().equals(declaredIn) || path.contains(MENU_PACKAGE)) {
                continue;
            }
            boolean insideConfig = path.contains("/config/");
            for (String line : e.getValue().split("\n")) {
                if (!word.matcher(line).find() || registrationLine.matcher(line).matches()) {
                    continue;
                }
                if (insideConfig && !getter.matcher(line).find()) {
                    continue;
                }
                hits.add(e.getKey());
                break;
            }
        }
        return hits;
    }

    private static String normalize(Path p) {
        return p.toString().replace('\\', '/');
    }

    private static List<Path> javaSources() throws IOException {
        try (Stream<Path> walk = Files.walk(SOURCE_ROOT)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();
        }
    }
}
