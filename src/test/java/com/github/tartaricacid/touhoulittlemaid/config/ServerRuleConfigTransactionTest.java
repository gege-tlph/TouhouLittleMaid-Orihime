package com.github.tartaricacid.touhoulittlemaid.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerRuleConfigTransactionTest {
    @TempDir
    Path temporaryDirectory;
    Path worldConfig;

    @BeforeEach
    void initializeConfig() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        ServerConfig.init();
        CommentedConfig config = ConfigFileMigration.emptyConfig();
        ServerConfig.CONFIG.correct(config);
        config.set(java.util.List.of("third_party", "preserved"), "sentinel");
        worldConfig = temporaryDirectory.resolve("world/serverconfig/" + ConfigFileMigration.SERVER_FILE_NAME);
        ConfigFileMigration.writeAtomically(config, worldConfig);
        assertTrue(ServerRuleConfig.loadFromPath(worldConfig));
    }

    @Test
    void malformedAndExtremePayloadsDoNotChangeFileOrSnapshots() throws Exception {
        String diskBefore = sha256(worldConfig);
        String fileBefore = ServerRuleConfig.snapshotJson();
        String activeBefore = ServerRuleConfig.runtimeSnapshotJson();
        // 原先这里还有一条「未知枚举值」样本，用的是唯一的枚举型世界规则 SERVER_STT_TYPE。
        // 「服务器提供 STT」撤除后世界规则里不再有任何枚举，该样本随之删除，不另找替身：
        // 它覆盖的「值不在允许集合内 → 整份拒绝」，已由下面两条越界整数覆盖。
        //
        // 试过用布尔规则喂字符串当替身，结果测试红：Gson 的 getAsBoolean() 对字符串是强制转换
        // （"not_a_boolean" → false），根本不抛异常。这条路走不通，别再试。
        String intKey = ServerRuleConfig.key(MaidConfig.MAID_WORK_RANGE);

        for (String payload : new String[]{
                "", "{", "[]", "{}", "{\"unknown.field\":1}",
                "{\"" + intKey + "\":2147483647}",
                "{\"" + intKey + "\":-2147483648}",
                "{\"" + intKey + "\":NaN}"
        }) {
            assertFalse(ServerRuleConfig.applyJson(payload, false), payload);
            assertEquals(diskBefore, sha256(worldConfig), payload);
            assertEquals(fileBefore, ServerRuleConfig.snapshotJson(), payload);
            assertEquals(activeBefore, ServerRuleConfig.runtimeSnapshotJson(), payload);
        }
    }

    @Test
    void staleEditorsMergeDifferentFieldsAndSameFieldIsLastWriteWins() throws Exception {
        String workRange = ServerRuleConfig.key(MaidConfig.MAID_WORK_RANGE);
        String idleRange = ServerRuleConfig.key(MaidConfig.MAID_IDLE_RANGE);

        assertTrue(ServerRuleConfig.applyJson(json(workRange, 7), false));
        assertTrue(ServerRuleConfig.applyJson(json(idleRange, 11), false));
        JsonObject merged = JsonParser.parseString(ServerRuleConfig.snapshotJson()).getAsJsonObject();
        assertEquals(7, merged.get(workRange).getAsInt());
        assertEquals(11, merged.get(idleRange).getAsInt());

        assertTrue(ServerRuleConfig.applyJson(json(workRange, 9), false));
        assertTrue(ServerRuleConfig.applyJson(json(workRange, 13), false));
        JsonObject lastWrite = JsonParser.parseString(ServerRuleConfig.snapshotJson()).getAsJsonObject();
        assertEquals(13, lastWrite.get(workRange).getAsInt());
        assertEquals(11, lastWrite.get(idleRange).getAsInt(),
                "a stale page must not roll back an untouched field");
        assertEquals("sentinel", ConfigFileMigration.read(worldConfig)
                .get(java.util.List.of("third_party", "preserved")));
    }

    @Test
    void dedicatedSaveChangesFileSnapshotOnlyUntilReload() throws Exception {
        String key = ServerRuleConfig.key(MaidConfig.MAID_WORK_RANGE);
        int oldRuntime = JsonParser.parseString(ServerRuleConfig.runtimeSnapshotJson())
                .getAsJsonObject().get(key).getAsInt();

        assertTrue(ServerRuleConfig.applyJson(json(key, 17), false));
        assertEquals(17, JsonParser.parseString(ServerRuleConfig.snapshotJson())
                .getAsJsonObject().get(key).getAsInt());
        assertEquals(oldRuntime, JsonParser.parseString(ServerRuleConfig.runtimeSnapshotJson())
                .getAsJsonObject().get(key).getAsInt());

        assertTrue(ServerRuleConfig.reloadFromDisk());
        assertEquals(17, JsonParser.parseString(ServerRuleConfig.runtimeSnapshotJson())
                .getAsJsonObject().get(key).getAsInt());
    }

    @Test
    void integratedSaveActivatesImmediatelyAndBrokenReloadKeepsLastGoodRuntime() throws Exception {
        String key = ServerRuleConfig.key(MaidConfig.MAID_WORK_RANGE);
        assertTrue(ServerRuleConfig.applyJson(json(key, 19), true));
        assertEquals(19, JsonParser.parseString(ServerRuleConfig.runtimeSnapshotJson())
                .getAsJsonObject().get(key).getAsInt());
        String runtimeBeforeDamage = ServerRuleConfig.runtimeSnapshotJson();
        String diskBeforeDamage = sha256(worldConfig);

        Files.writeString(worldConfig, "truncated = [", StandardCharsets.UTF_8);
        assertFalse(ServerRuleConfig.reloadFromDisk());
        assertEquals(runtimeBeforeDamage, ServerRuleConfig.runtimeSnapshotJson());
        assertEquals(diskBeforeDamage, sha256(worldConfig));
        ConfigFileMigration.read(worldConfig);
    }

    @Test
    void runtimeSyncNeverWritesTheWorldFile() throws Exception {
        String key = ServerRuleConfig.key(MaidConfig.MAID_WORK_RANGE);
        String diskBefore = sha256(worldConfig);
        String fileBefore = ServerRuleConfig.snapshotJson();
        String activeBefore = ServerRuleConfig.runtimeSnapshotJson();
        Path clientGlobal = temporaryDirectory.resolve("client/config/" + ConfigFileMigration.GLOBAL_FILE_NAME);
        Path clientSite = temporaryDirectory.resolve("client/config/touhou_little_maid/sites/stt.json");
        Files.createDirectories(clientGlobal.getParent());
        Files.createDirectories(clientSite.getParent());
        Files.writeString(clientGlobal, "client_only = true\n", StandardCharsets.UTF_8);
        Files.writeString(clientSite, "{\"client_secret\":\"preserve\"}", StandardCharsets.UTF_8);
        String globalBefore = sha256(clientGlobal);
        String siteBefore = sha256(clientSite);

        assertTrue(ServerRuleConfig.applyRuntimeJson(json(key, 23)));
        assertEquals(diskBefore, sha256(worldConfig));
        assertEquals(globalBefore, sha256(clientGlobal));
        assertEquals(siteBefore, sha256(clientSite));
        assertEquals(fileBefore, ServerRuleConfig.snapshotJson());
        assertNotEquals(activeBefore, ServerRuleConfig.runtimeSnapshotJson());
    }

    private static String json(String key, int value) {
        JsonObject object = new JsonObject();
        object.addProperty(key, value);
        return object.toString();
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }
}
