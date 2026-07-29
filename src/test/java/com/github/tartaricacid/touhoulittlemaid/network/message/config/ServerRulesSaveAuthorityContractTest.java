package com.github.tartaricacid.touhoulittlemaid.network.message.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客户端载荷会导致服务端改写世界配置文件，因此「先验权限、再落盘」这条次序本身就是安全边界。
 *
 * <p>{@code GameModeUtilPermissionTest} 只覆盖判据 {@code canEditSite} 自身；判据被正确**使用**
 * 是另一回事——把 {@code handle} 里的两行调换，判据依然完全正确，未授权载荷却已经写进文件。
 * 这类次序不变量没法用普通单元测试钉住：拒绝分支会调用 Fabric 的 {@code ServerPlayNetworking.send}，
 * 在纯 JUnit 环境里没有可用的连接。故与 {@code VanillaReplaceRendererWiringContractTest} 同法，
 * 在源码层断言接线次序。</p>
 */
class ServerRulesSaveAuthorityContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path SAVE_PACKET = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "network", "message", "config", "SaveServerRulesPacket.java"));

    /** §17 v2 后保存包有**两个**变更入口：存档级世界规则 + 实例级 AI 规则，权限守卫必须先于二者 */
    @Test
    void permissionIsCheckedBeforeAnyConfigMutation() throws IOException {
        String active = activeSource(SAVE_PACKET);
        int guard = active.indexOf("GameModeUtil.canEditSite");
        int worldMutation = indexOfWorldApply(active);
        int aiMutation = active.indexOf("AiServerRuleConfig.applyJson");

        assertTrue(guard >= 0, "保存处理必须调用 GameModeUtil.canEditSite 判定权限");
        assertTrue(worldMutation >= 0, "保存处理必须经由 ServerRuleConfig.applyJson 落盘世界规则");
        assertTrue(aiMutation >= 0, "保存处理必须经由 AiServerRuleConfig.applyJson 落盘 AI 规则");
        int firstMutation = Math.min(worldMutation, aiMutation);
        assertTrue(guard < firstMutation,
                "权限判据必须早于任何 applyJson：次序颠倒时未授权玩家的载荷会先写进配置文件再被拒绝");

        assertTrue(active.substring(guard, firstMutation).contains("return"),
                "权限不足时必须提前 return，仅记录日志或提示而继续执行等于没有守卫");
    }

    @Test
    void eachStoreHasExactlyOneMutationCallSiteSoTheOrderingAssertionIsSufficient() throws IOException {
        String active = activeSource(SAVE_PACKET);
        assertEquals(1, countMatches(active, "(?<!Ai)ServerRuleConfig\\.applyJson"),
                "世界规则出现第二个 applyJson 调用点时，上面基于首次出现位置的次序断言就不再覆盖全部变更入口，"
                        + "新增入口必须自带权限守卫并在此更新断言");
        assertEquals(1, countMatches(active, "AiServerRuleConfig\\.applyJson"),
                "AI 规则同理：变更入口恰一个，多出来的必须回此登记");
    }

    /** 「ServerRuleConfig.applyJson」是「AiServerRuleConfig.applyJson」的子串，用负向后顾区分两店 */
    private static int indexOfWorldApply(String source) {
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("(?<!Ai)ServerRuleConfig\\.applyJson").matcher(source);
        return matcher.find() ? matcher.start() : -1;
    }

    private static int countMatches(String source, String regex) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(regex).matcher(source);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static String activeSource(Path path) throws IOException {
        StringBuilder active = new StringBuilder();
        for (String line : Files.readAllLines(path)) {
            if (line.trim().startsWith("//")) {
                continue;
            }
            active.append(line).append('\n');
        }
        return active.toString();
    }
}
