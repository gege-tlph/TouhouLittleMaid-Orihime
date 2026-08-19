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

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 只在物理客户端注册的两个 spec（{@link GeneralConfig} → {@code -global.toml}、
 * {@link AiClientConfig} → {@code -ai.toml}），它们的字段在**专服上恒为 null**。
 * 本类机械保证：没有任何一处读点会在专服上被执行。
 *
 * <p><b>判据按成因写，不按包名写。</b>成因是「这行代码会不会在专服上跑」，所以：
 * 落在 {@code client/} 包下算过；带客户端短路算过；除此之外一律要在
 * {@link #REACHABLE_ONLY_FROM_CLIENT} 里**具名登记并写明理由**——写不出理由的，
 * 就是真的够得着，专服会 NPE。</p>
 *
 * <p><b>扫描面按「谁可能犯这个错」划，不按「配置属于谁」划</b>：整个 {@code src/main/java}。
 * 本仓库栽过一次「SCOPE 只圈了功能所在的那几层，UI 层里的同款漏网白绿一轮」。</p>
 *
 * <p><b>为什么必须是机械闸</b>：漏掉一处的症状是专服每 tick（或每次触碰）抛
 * 「Cannot get config value before config is loaded」/ NPE，而单人档、局域网、
 * 全部 JUnit 与 GameTest 都照不出来——那些环境里物理端都是 CLIENT。</p>
 */
class ClientOnlyConfigReadContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path ROOT = Path.of("..", "..");
    private static final Path SOURCE_ROOT = ROOT.resolve(Path.of("src", "main", "java"));
    private static final Path CONFIG_PACKAGE = SOURCE_ROOT.resolve(
            Path.of("com", "github", "tartaricacid", "touhoulittlemaid", "config"));

    /** 从这两个类的 {@code values()} 里读出被看管的字段——不写死清单，免得清单与实现各说各话。 */
    private static final List<String> CLIENT_ONLY_SPEC_SOURCES = List.of(
            "GeneralConfig.java", "AiClientConfig.java");

    /** {@code XxxConfig.FIELD_NAME} */
    private static final Pattern CLAIM = Pattern.compile(
            "\\b(?<owner>\\w+Config)\\s*\\.\\s*(?<field>[A-Z][A-Z0-9_]*)\\b");

    /** 客户端短路的各种写法。命中即认为这条语句在专服上到不了那次读。 */
    private static final Pattern CLIENT_GUARD = Pattern.compile(
            "isClientSide|isClient\\s*\\(|EnvType\\.CLIENT|Minecraft\\.getInstance");

    /**
     * 不在 {@code client/} 包下、也没有短路，但**经调用链证明只有客户端到得了**的文件。
     * 每一条都要写清是谁把它拉起来的——理由才是下一个人复用的东西，结论不是。
     */
    private static final Map<String, String> REACHABLE_ONLY_FROM_CLIENT = new LinkedHashMap<>(Map.of(
            "compat/cloth/MenuIntegration.java",
            "Cloth 配置菜单。两个调用点：modmenu entrypoint 的 ModMenuApiImpl，"
                    + "与客户端 GUI 的 client/gui/entity/maid/MaidSideTabs（经 ClothConfigCompat）。"
                    + "专服既没有 ModMenu 也没有那个屏，且 me.shedaniel.clothconfig2 本身是客户端库。",
            "ai/service/stt/STTSite.java",
            "STT_PROXY_ADDRESS 只出现在一个 lambda 体内（不是方法引用），接口初始化时不解引用；"
                    + "而 AvailableSites#managesSttSites 已按物理端挡住，专服根本不读 stt.json、"
                    + "也就不会发起需要选代理的请求。两道判据各自独立，动任一处都要回到这里重判。"));

    @Test
    void noClientOnlyConfigValueIsReadWhereADedicatedServerCanReachIt() throws IOException {
        Set<String> claimed = claimedClientOnlyFields();
        assertTrue(claimed.size() >= 20,
                "活性：两个 CLIENT spec 至少该认领 20 个字段，只解析出 " + claimed.size()
                        + " 个——是解析器坏了，不是没有字段");

        List<Path> sources = javaSources();
        assertTrue(sources.size() >= 1000,
                "活性：src/main/java 至少该有 1000 个源文件，只走过 " + sources.size() + " 个");

        List<String> violations = new ArrayList<>();
        int consumers = 0;
        for (Path path : sources) {
            String relative = relative(path);
            if (relative.startsWith("com/github/tartaricacid/touhoulittlemaid/config/")) {
                continue;                       // 声明与 values() 清单不是消费点
            }
            String active = activeSource(path);
            String[] lines = active.split("\n", -1);
            for (int i = 0; i < lines.length; i++) {
                Matcher matcher = CLAIM.matcher(lines[i]);
                while (matcher.find()) {
                    if (!claimed.contains(matcher.group("field"))) {
                        continue;
                    }
                    consumers++;
                    if (isAcceptable(relative, statementAround(lines, i))) {
                        continue;
                    }
                    violations.add(relative + ":" + (i + 1) + "  " + matcher.group("field")
                            + "  ->  " + lines[i].trim());
                }
            }
        }

        assertTrue(consumers >= 20,
                "活性：这些字段至少该有 20 个读点，只扫到 " + consumers + " 个——识别模式坏了");
        assertTrue(violations.isEmpty(), () -> """
                下列读点读的是**只在客户端注册**的配置，而它既不在 client/ 包下、\
                也没有客户端短路、也没有登记进 REACHABLE_ONLY_FROM_CLIENT。
                专服上这些字段恒为 null，执行到就是 NPE 或「Cannot get config value before config is loaded」。
                三条出路：① 把读点挪进 client/ ② 在读之前加 isClientSide 短路 \
                ③ 若确证只有客户端到得了，具名登记并**写清是谁把它拉起来的**。
                （若这个键本来就该两侧都读，那它就不属于 GeneralConfig，请改放 CommonConfig。）

                """ + String.join("\n", violations));
    }

    /** 登记表不许留死条目：文件没了或已挪进 client/，就该把这条删掉，否则它会掩护下一个真违规。 */
    @Test
    void theAllowListHasNoStaleEntries() {
        List<String> stale = new ArrayList<>();
        REACHABLE_ONLY_FROM_CLIENT.forEach((relative, reason) -> {
            if (!Files.isRegularFile(SOURCE_ROOT.resolve(
                    "com/github/tartaricacid/touhoulittlemaid/" + relative))) {
                stale.add(relative + "（文件不存在）");
            }
            if (relative.startsWith("client/")) {
                stale.add(relative + "（已在 client/ 包下，无须登记）");
            }
            if (reason.isBlank()) {
                stale.add(relative + "（没写理由——理由才是下一个人复用的东西）");
            }
        });
        assertTrue(stale.isEmpty(), () -> "REACHABLE_ONLY_FROM_CLIENT 里有过期条目：\n"
                + String.join("\n", stale));
    }

    private static boolean isAcceptable(String relative, String statement) {
        String inPackage = relative.startsWith("com/github/tartaricacid/touhoulittlemaid/")
                ? relative.substring("com/github/tartaricacid/touhoulittlemaid/".length())
                : relative;
        return inPackage.startsWith("client/")
                || REACHABLE_ONLY_FROM_CLIENT.containsKey(inPackage)
                || CLIENT_GUARD.matcher(statement).find();
    }

    /**
     * 取这次读所在的**整条语句**：从上一条语句的结尾（{@code ; } { }}）之后到本行。
     * 短路守卫与读点常常不在同一行（{@code MaidParticleManager} 就隔了两行），
     * 只看当前行会把真守卫读成没守卫。
     */
    private static String statementAround(String[] lines, int index) {
        StringBuilder statement = new StringBuilder();
        int start = index;
        while (start > 0) {
            String previous = lines[start - 1].trim();
            if (previous.isEmpty()) {
                start--;
                continue;
            }
            if (previous.endsWith(";") || previous.endsWith("{") || previous.endsWith("}")) {
                break;
            }
            start--;
        }
        for (int i = start; i <= index; i++) {
            statement.append(lines[i]).append('\n');
        }
        return statement.toString();
    }

    private static Set<String> claimedClientOnlyFields() throws IOException {
        Set<String> claimed = new LinkedHashSet<>();
        for (String name : CLIENT_ONLY_SPEC_SOURCES) {
            Path path = CONFIG_PACKAGE.resolve(name);
            assertTrue(Files.isRegularFile(path), "找不到 CLIENT spec 源文件：" + path);
            String values = valuesBody(activeSource(path));
            Matcher matcher = CLAIM.matcher(values);
            while (matcher.find()) {
                claimed.add(matcher.group("field"));
            }
        }
        return claimed;
    }

    /** 截 {@code values()} 的方法体，别把整份文件里的其它字段引用也算进来。 */
    private static String valuesBody(String source) {
        String anchor = "values() {";
        int at = source.indexOf(anchor);
        assertTrue(at >= 0, "CLIENT spec 必须有 values()——迁移逐键搬旧值靠它");
        int open = source.indexOf('{', at + anchor.length() - 1);
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
        throw new AssertionError("values() 的大括号不配对");
    }

    private static List<Path> javaSources() throws IOException {
        try (Stream<Path> stream = Files.walk(SOURCE_ROOT)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".java"))
                    .toList();
        }
    }

    private static String relative(Path path) {
        return SOURCE_ROOT.relativize(path).toString().replace('\\', '/');
    }

    /**
     * 剥掉注释后的源码，**行号保持不变**（剥掉的位置留空行）。
     * 行注释、块注释与 javadoc 都要剥：只剥 {@code //} 会让「只在注释里出现的读点」被当成真读点，
     * 本仓库反过来也栽过——判为存在的接线其实只活在 javadoc 里。
     */
    private static String activeSource(Path path) throws IOException {
        StringBuilder active = new StringBuilder();
        boolean inBlockComment = false;
        // 用 readAllBytes 解码而不是 readAllLines：后者遇到非法字节直接抛，
        // 一个坏文件就会让整道闸变成「跑不起来」而不是「照出问题」。
        for (String line : new String(Files.readAllBytes(path), StandardCharsets.UTF_8).split("\n", -1)) {
            String text = line;
            if (inBlockComment) {
                int end = text.indexOf("*/");
                if (end < 0) {
                    active.append('\n');
                    continue;
                }
                inBlockComment = false;
                text = text.substring(end + 2);
            }
            text = text.replaceAll("/\\*.*?\\*/", "");
            int blockStart = text.indexOf("/*");
            if (blockStart >= 0) {
                inBlockComment = true;
                text = text.substring(0, blockStart);
            }
            int lineComment = text.indexOf("//");
            if (lineComment >= 0) {
                text = text.substring(0, lineComment);
            }
            active.append(text).append('\n');
        }
        return active.toString();
    }
}
