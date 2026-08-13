package com.github.tartaricacid.touhoulittlemaid.network.message.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path ROOT = Path.of("..", "..");
    private static final Path SAVE_PACKET = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "network", "message", "config", "SaveServerRulesPacket.java"));

    @Test
    void permissionIsCheckedBeforeAnyConfigMutation() throws IOException {
        String active = activeSource(SAVE_PACKET);
        int guard = active.indexOf("GameModeUtil.canEditSite");
        int mutation = active.indexOf("ServerRuleConfig.applyJson");

        assertTrue(guard >= 0, "保存处理必须调用 GameModeUtil.canEditSite 判定权限");
        assertTrue(mutation >= 0, "保存处理必须经由 ServerRuleConfig.applyJson 落盘世界规则");
        assertTrue(guard < mutation,
                "权限判据必须早于 applyJson：次序颠倒时未授权玩家的载荷会先写进配置文件再被拒绝");
        assertTrue(active.substring(guard, mutation).contains("return"),
                "权限不足时必须提前 return，仅提示而继续执行等于没有守卫");
    }

    @Test
    void exactlyOneMutationCallSiteSoTheOrderingAssertionIsSufficient() throws IOException {
        assertEquals(1, countMatches(activeSource(SAVE_PACKET), "ServerRuleConfig\\.applyJson"),
                "出现第二个 applyJson 调用点时，上面基于首次出现位置的次序断言就不再覆盖全部变更入口，"
                        + "新增入口必须自带权限守卫并在此更新断言");
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
        assertTrue(Pattern.compile("applyJson\\(.*,\\s*activate\\s*\\)").matcher(active).find(),
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
