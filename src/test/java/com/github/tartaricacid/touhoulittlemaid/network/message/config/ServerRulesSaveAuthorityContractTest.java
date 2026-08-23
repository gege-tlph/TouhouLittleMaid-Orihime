package com.github.tartaricacid.touhoulittlemaid.network.message.config;

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
 * 客户端载荷会导致服务端改写世界配置文件，因此「先验权限、再落盘」这条**次序**本身就是安全边界。
 *
 * <p>判据正确与判据被正确**使用**是两回事：把 {@code handle} 里的两段调换，
 * {@code canEditSite} 依然完全正确，未授权载荷却已经写进文件。这类次序不变量没法用普通单元测试钉住
 * ——拒绝分支会调用 Fabric 的 {@code ServerPlayNetworking.send}，纯 JUnit 环境里没有可用连接。
 * 故在源码层断言接线次序。</p>
 */
class ServerRulesSaveAuthorityContractTest {
    /** 会改写配置文件的两个店。加第三家店时这张表必须同步，否则下面那条总数断言当场红。 */
    private static final List<String> MUTATION_ENTRY_POINTS = List.of("ServerRuleConfig", "AiServerRuleConfig");

    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path ROOT = Path.of("..", "..");
    private static final Path SAVE_PACKET = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "network", "message", "config", "SaveServerRulesPacket.java"));

    /**
     * 变更入口有两个（世界规则店与 AI 店），守卫必须早于**每一个**。
     *
     * <p>⚠️ 判据用带词边界的正则而不是 {@code indexOf}：{@code "ServerRuleConfig.applyJson"}
     * 作为子串同样命中 {@code AiServerRuleConfig.applyJson}，用 {@code indexOf} 会把两个入口
     * 读成一个，次序断言随之只覆盖其中先出现的那个。</p>
     */
    @Test
    void permissionIsCheckedBeforeEveryConfigMutation() throws IOException {
        String active = activeSource(SAVE_PACKET);
        int guard = active.indexOf("GameModeUtil.canEditSite");
        assertTrue(guard >= 0, "保存处理必须调用 GameModeUtil.canEditSite 判定权限");

        for (String store : MUTATION_ENTRY_POINTS) {
            Matcher matcher = Pattern.compile("\\b" + store + "\\.applyJson").matcher(active);
            assertTrue(matcher.find(), "保存处理必须经由 " + store + ".applyJson 落盘");
            int mutation = matcher.start();
            assertTrue(guard < mutation,
                    "权限判据必须早于 " + store + ".applyJson：次序颠倒时未授权玩家的载荷会先写进配置文件再被拒绝");
            assertTrue(active.substring(guard, mutation).contains("return"),
                    "权限不足时必须提前 return，仅提示而继续执行等于没有守卫");
        }
    }

    /**
     * 每个店恰好一个变更调用点，上面基于首次出现位置的次序断言才够用。
     *
     * <p>本用例是「新增入口必须回来更新断言」的强制装置：任一店多出第二个 {@code applyJson}
     * 调用点就当场红。<b>同时钉住入口总数</b>——出现第三家店时 {@link #MUTATION_ENTRY_POINTS}
     * 没跟上，这条也会红，而不是让那家店的载荷绕过次序断言。</p>
     */
    @Test
    void exactlyOneMutationCallSitePerStoreSoTheOrderingAssertionIsSufficient() throws IOException {
        String active = activeSource(SAVE_PACKET);
        for (String store : MUTATION_ENTRY_POINTS) {
            assertEquals(1, countMatches(active, "\\b" + store + "\\.applyJson"),
                    store + " 出现第二个 applyJson 调用点：新增入口必须自带权限守卫并在此更新断言");
        }
        assertEquals(MUTATION_ENTRY_POINTS.size(), countMatches(active, "\\b\\w*ServerRuleConfig\\.applyJson"),
                "出现了不在 MUTATION_ENTRY_POINTS 里的配置店——它的载荷没有被次序断言覆盖");
    }

    /**
     * AI 店<b>没有</b> activate 参数：它保存即激活，专服也一样（见 {@code AiServerRuleConfig} 类注释）。
     * 而激活后必须告知在线玩家默认值变了，否则出现「配置已生效但玩家完全不知情」的半条路。
     */
    @Test
    void theAiStoreActivatesImmediatelyAndAnnouncesDefaultChanges() throws IOException {
        String active = activeSource(SAVE_PACKET);
        assertTrue(Pattern.compile("AiServerRuleConfig\\.applyJson\\([^,)]*\\)").matcher(active).find(),
                "AI 店的 applyJson 只收一个实参：它没有「写文件但不激活」这条路");

        int capture = active.indexOf("DefaultAiSnapshot.capture()");
        int mutation = active.indexOf("AiServerRuleConfig.applyJson");
        int notify = active.indexOf("diffAndNotify");
        assertTrue(capture >= 0, "激活前必须先 capture 默认值快照，否则无从判断它变没变");
        assertTrue(notify >= 0, "AI 默认值变更必须广播给在线玩家");
        assertTrue(capture < mutation, "capture 必须早于 applyJson：晚了就永远比较不出差异");
        assertTrue(mutation < notify, "diffAndNotify 必须晚于 applyJson：早了比较的是旧值与旧值");
    }

    /**
     * 保存必须**落盘**，不能只改内存快照。
     *
     * <p>{@code applyJson(json, activate)} 的第二个参数只决定「要不要同时激活为运行期值」，
     * 写文件是无条件的。这里钉住调用形态：专服传 false（等 /tlm config reload），
     * 其余传 true（保存即生效）。写成常量 true 或常量 false 都会丢掉一半行为。</p>
     */
    @Test
    void activationIsDecidedByServerTypeRatherThanHardCoded() throws IOException {
        String active = activeSource(SAVE_PACKET);
        assertTrue(active.contains("isDedicatedServer()"),
                "激活与否必须按服务器形态判定：专服写文件等重载，单人/局域网保存即生效");
        assertTrue(Pattern.compile("applyJson\\(.*,\\s*activateWorld\\s*\\)").matcher(active).find(),
                "applyJson 的 activate 实参必须是那个判定出来的变量，不能是写死的 true/false");
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
     * 被当成真实存在——本仓库栽过这一次，故这里连 {@code /* … *&#47;} 一并处理。</p>
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
