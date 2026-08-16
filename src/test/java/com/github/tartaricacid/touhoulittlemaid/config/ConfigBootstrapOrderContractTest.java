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
            "migrateAiFileIfNeeded",
            "inheritMagmaCubeFromSlime");

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
        assertEquals(MIGRATIONS_BEFORE_COMMON_REGISTRATION.size(),
                countMatches(active, "ConfigFileMigration\\.\\w+\\("),
                "入口里出现了不在 MIGRATIONS_BEFORE_COMMON_REGISTRATION 里的迁移调用——"
                        + "它没有被次序断言覆盖，请登记进那张表");
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
