package com.github.tartaricacid.touhoulittlemaid.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置引导的**次序**不变量，判据取自入口源码。
 *
 * <p><b>为什么必须钉住</b>：所有配置迁移都得跑在 COMMON spec 注册之前。注册加载那一刻
 * {@code correct()} 会把「已不在 spec 里」的键<b>整批剥掉</b>——迁移晚一步，源头就空了，
 * 玩家在旧文件里调过的值全部丢失，而且<b>不会有任何异常</b>：一切照常启动，只是值回到默认。
 * 这类「静默毁数据」的次序错误正是普通单元测试照不出来的形态（迁移函数本身在任何次序下都自洽），
 * 故在源码层断言接线次序。</p>
 *
 * <p>注释剥离的 helper 与 {@code ServerRulesSaveAuthorityContractTest} 等四个契约测试同款。
 * 本仓库的既有形态就是各自持一份，本类沿用而不顺手重构——无症状不改。</p>
 */
class ConfigBootstrapOrderContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path ROOT = Path.of("..", "..");
    private static final Path ENTRYPOINT = ROOT.resolve(Path.of("src", "main", "java", "cn", "sh1rocu",
            "touhoulittlemaid", "TouhouLittleMaidFabric.java"));

    /**
     * 入口里必须跑在 COMMON 注册之前的全部迁移。
     * 新增一条迁移就要进这张表，否则下面的总数断言当场红——枚举型断言若不自带总数下限，
     * 漏登记的那一条会安安静静地不被覆盖。
     */
    private static final List<String> MIGRATIONS_BEFORE_COMMON_REGISTRATION = List.of(
            "migrateServerFileIfNeeded",
            "migrateGlobalFileIfNeeded",
            "migrateAiFileIfNeeded",
            "inheritMagmaCubeFromSlime");

    /** 只在物理客户端建立与注册的 spec。它们的文件不该出现在专服的 config 目录里。 */
    private static final List<String> CLIENT_ONLY_SPECS = List.of("GeneralConfig", "AiClientConfig");

    /**
     * 入口里对 {@code ConfigFileMigration} 的调用中，**不是**迁移的那些。
     * 单列出来而不是混进上面那张表：混进去会让「必须先于 COMMON 注册」这条断言
     * 顺带覆盖一个与它无关的调用，日后谁把它挪走都不会红，断言就悄悄失效了。
     */
    private static final List<String> NON_MIGRATION_CALLS = List.of("logUnusedClientFilesOnServer");

    @Test
    void everyMigrationRunsBeforeTheCommonSpecIsRegistered() throws IOException {
        String active = activeSource(ENTRYPOINT);
        int registration = active.indexOf("CommonConfig.init()");
        assertTrue(registration >= 0, "入口必须注册 COMMON spec（CommonConfig.init()）");

        for (String migration : MIGRATIONS_BEFORE_COMMON_REGISTRATION) {
            int at = active.indexOf(migration);
            assertTrue(at >= 0, "入口必须调用 ConfigFileMigration." + migration);
            assertTrue(at < registration, migration
                    + " 必须早于 COMMON spec 注册：注册那一刻 correct() 会剥掉已不在 spec 里的键，"
                    + "迁移晚一步就再也读不到旧值，且不会报任何错");
        }
    }

    @Test
    void theMigrationListIsComplete() throws IOException {
        String active = activeSource(ENTRYPOINT);
        assertEquals(MIGRATIONS_BEFORE_COMMON_REGISTRATION.size() + NON_MIGRATION_CALLS.size(),
                countMatches(active, "ConfigFileMigration\\.\\w+\\("),
                "入口里出现了不在 MIGRATIONS_BEFORE_COMMON_REGISTRATION 里的迁移调用——"
                        + "它没有被次序断言覆盖，请登记进那张表");
    }

    /**
     * 岩浆怪继承必须**早于** global 迁移。
     *
     * <p>它作用在迁移源 {@code -common.toml} 上，不是目标 {@code -global.toml}。次序反了，
     * global 迁移会先按 spec 铺一份默认值（{@code ReplaceMagmaCubeModel=true}），源里没这个键
     * 便不覆盖；随后继承方法看到目标里已经有该键就直接返回。结果是玩家的旧选择被默认值吃掉，
     * <b>而且不会有任何报错</b>——只在「升级 + 老文件 + 没碰过岩浆怪开关」这一条路径上发作。</p>
     */
    @Test
    void theMagmaCubeInheritanceRunsBeforeTheGlobalFileIsSeeded() throws IOException {
        String body = registerConfigurationBody();
        int inherit = body.indexOf("inheritMagmaCubeFromSlime");
        int seed = body.indexOf("migrateGlobalFileIfNeeded");
        assertTrue(inherit >= 0 && seed >= 0, "入口必须同时调用这两条");
        assertTrue(inherit < seed,
                "inheritMagmaCubeFromSlime 必须早于 migrateGlobalFileIfNeeded：它补的是迁移源，"
                        + "晚一步旧值就被默认值静默盖掉");
    }

    /**
     * 玩家个人配置的建立与注册必须整段落在 {@code EnvType.CLIENT} 判断的**块内**。
     *
     * <p>这是「专服不生成 {@code -global.toml} / {@code -ai.toml}」的唯一实现手段。守卫没了，
     * 编译、启动、玩法全部正常，只是服主的 {@code config/} 里凭空多出两份写着渲染偏好与麦克风
     * 选择的文件——<b>载体的存在会被读成语义</b>，没有任何测试会因此变红。故在源码层钉住。</p>
     *
     * <p>判据缩到那个 {@code if} 的**块内**，不是「出现在方法体里且行号靠后」：
     * 后者在守卫被改成无条件之后依然全绿（本仓库栽过这一次）。</p>
     */
    @Test
    void thePlayerConfigIsBuiltAndRegisteredOnlyOnThePhysicalClient() throws IOException {
        String guarded = clientGuardedBlock();
        for (String spec : CLIENT_ONLY_SPECS) {
            assertTrue(guarded.contains(spec + ".getConfigSpec()"),
                    spec + " 的 spec 必须在 EnvType.CLIENT 块内建立——专服上建了它，"
                            + "FCAP 就会为它生成文件");
        }
        assertTrue(guarded.contains("migrateGlobalFileIfNeeded"),
                "global 迁移必须在客户端块内：它会创建 -global.toml，专服不该有这个文件");
        assertTrue(guarded.contains("migrateAiFileIfNeeded"),
                "个人 AI 迁移必须在客户端块内，理由同上");
        assertEquals(2, countMatches(guarded, "ConfigRegistry\\.INSTANCE\\.register\\("),
                "两份个人配置的注册都必须在客户端块内，且块内不该有别的注册");
    }

    /**
     * 两份个人配置必须以 {@code Type.CLIENT} + **显式文件名**注册，实例级那份必须是 {@code Type.COMMON}。
     *
     * <p>文件名不是装饰：不给显式名，FCAP 会按类型取默认名，{@code -ai.toml} 会变成
     * {@code -client.toml}，玩家配好的一切当场「消失」（其实是换了个文件读）。</p>
     */
    @Test
    void eachSpecIsRegisteredWithTheRightTypeAndFileName() throws IOException {
        String body = registerConfigurationBody();
        assertTrue(body.contains("ModConfig.Type.CLIENT,")
                        && body.contains("globalSpec, ConfigFileMigration.GLOBAL_FILE_NAME"),
                "个人偏好必须注册为 Type.CLIENT 且显式指定 GLOBAL_FILE_NAME");
        assertTrue(body.contains("aiClientSpec, ConfigFileMigration.AI_FILE_NAME"),
                "个人 AI 配置必须显式指定 AI_FILE_NAME，否则 FCAP 会按 Type.CLIENT 的默认名建 -client.toml");
        assertTrue(body.contains("ModConfig.Type.COMMON, CommonConfig.init()"),
                "两侧共用的那份必须仍是 Type.COMMON（专服要读 EnableMaidCurios）");
        assertEquals(2, countMatches(body, "ModConfig\\.Type\\.CLIENT"),
                "活性：Type.CLIENT 恰好出现两次（两份个人配置各一次）");
    }

    /** 取 {@code registerConfiguration} 的方法体（大括号配对），把顺序断言的范围缩到它里面。 */
    private static String registerConfigurationBody() throws IOException {
        return braceBlock(activeSource(ENTRYPOINT), "private static void registerConfiguration()");
    }

    /** 取客户端守卫 {@code if} 的块体。判据是那个条件表达式本身，不是行号。 */
    private static String clientGuardedBlock() throws IOException {
        String body = registerConfigurationBody();
        String guard = "if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)";
        assertTrue(body.contains(guard),
                "入口必须用物理端判据守住个人配置：" + guard + "。"
                        + "别换成 MinecraftServer#isDedicatedServer——这一步还没有服务器实例，"
                        + "而且开了局域网的客户端仍是 CLIENT，它自己就要用这两份文件");
        return braceBlock(body, guard);
    }

    /** 从 {@code anchor} 之后的第一个 '{' 起，按配对取到它的 '}'。 */
    private static String braceBlock(String source, String anchor) {
        int at = source.indexOf(anchor);
        assertTrue(at >= 0, "源码里找不到锚点：" + anchor);
        int open = source.indexOf('{', at + anchor.length());
        assertTrue(open >= 0, "锚点后没有块：" + anchor);
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
        throw new AssertionError("大括号不配对：" + anchor);
    }

    /**
     * AI 店的 spec 必须先建起来，再有人去 {@code initializeDefaults} 或路由读它。
     * {@code init()} 同时登记归属表（{@code owns}），没建之前路由判据恒为 false，
     * 症状是 AI 键静默走进世界规则店并退回默认值。
     */
    @Test
    void theAiStoreSpecIsBuiltBeforeAnythingUsesIt() throws IOException {
        String active = activeSource(ENTRYPOINT);
        int init = active.indexOf("AiServerRuleConfig.init()");
        int defaults = active.indexOf("AiServerRuleConfig.initializeDefaults()");
        int load = active.indexOf("AiServerRuleConfig.loadForServer");

        assertTrue(init >= 0, "入口必须建 AI 店的 spec 并登记归属表");
        assertTrue(defaults > init, "initializeDefaults 必须晚于 init：values() 在 spec 建好前是一片 null");
        assertTrue(load > init, "loadForServer 必须晚于 init");
    }

    /** 两个店都要装载与卸载，缺一半会让下一个世界读到上一个世界的残留快照。 */
    @Test
    void bothStoresAreLoadedOnStartAndUnloadedOnStop() throws IOException {
        String active = activeSource(ENTRYPOINT);
        for (String call : List.of("ServerRuleConfig.loadForServer", "AiServerRuleConfig.loadForServer",
                "ServerRuleConfig.unloadWorld", "AiServerRuleConfig.unload")) {
            assertTrue(active.contains(call), "入口必须调用 " + call);
        }
    }

    private static int countMatches(String source, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(source);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * 剥掉注释后的源码。
     *
     * <p>行注释、块注释与 javadoc **都要剥**：只剥 {@code //} 会让「仅出现在 javadoc 里的接线」
     * 被当成真实存在——本仓库栽过这一次。</p>
     */
    private static String activeSource(Path path) throws IOException {
        StringBuilder active = new StringBuilder();
        boolean inBlockComment = false;
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.trim();
            if (inBlockComment) {
                int end = trimmed.indexOf("*/");
                if (end < 0) {
                    continue;
                }
                inBlockComment = false;
                trimmed = trimmed.substring(end + 2).trim();
            }
            trimmed = trimmed.replaceAll("/\\*.*?\\*/", "");
            int blockStart = trimmed.indexOf("/*");
            if (blockStart >= 0) {
                inBlockComment = true;
                trimmed = trimmed.substring(0, blockStart);
            }
            int lineComment = trimmed.indexOf("//");
            if (lineComment >= 0) {
                trimmed = trimmed.substring(0, lineComment);
            }
            active.append(trimmed).append('\n');
        }
        return active.toString();
    }
}
