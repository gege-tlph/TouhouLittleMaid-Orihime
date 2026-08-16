package com.github.tartaricacid.touhoulittlemaid.ai.service;

import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 不变量「凭据永不跨越它被输入的那一端」的可执行版本。
 *
 * <p><b>为什么要覆盖全部站点类型，而不是挑两个写死</b>：脱敏做在 codec 编码结果上，正是为了让
 * 新增站点类型不可能漏掉；如果测试只钉住其中一两个类型，这个保证就退化成「今天没漏」。
 * 覆盖面由 {@link #everySecretBearingSiteClassIsCovered()} 扫源码强制——**漏加一个类型会红**。</p>
 *
 * <p>同时钉住「保持原密钥」的三态语义。它是 §8.A3 那条回归的守卫：管理员只改地址、
 * 没碰密钥框，保存后密钥不能消失。</p>
 */
class SiteSecretRedactionTest {
    private static final String PLAINTEXT = "sk-not-a-real-key-0123456789";

    @BeforeAll
    static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    private record Case(String service, String apiType, SerializableSite<?> serializer) {
    }

    /**
     * 全部带密钥字段的站点类型。不带密钥的（Player2、系统 TTS 等）不在此列——
     * 它们没有可泄漏的东西，纳入只会让断言失去意义。
     *
     * <p><b>为什么手写而不是遍历 {@code SerializerRegister}</b>：那个 init 会碰
     * {@code TouhouLittleMaid.EXTENSIONS}，需要 Fabric launcher，纯 JUnit 里起不来。
     * 手写清单的风险是「新增类型忘了加」，故由 {@link #everySecretBearingSiteClassIsCovered()}
     * 扫源码兜底——**漏加会红**。</p>
     */
    private static List<Case> secretBearingSites() {
        List<Case> cases = new ArrayList<>();
        cases.add(new Case("llm", "openai",
                new com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAISite.Serializer()));
        cases.add(new Case("tts", "fish_audio",
                new com.github.tartaricacid.touhoulittlemaid.ai.service.tts.fishaudio.TTSFishAudioSite.Serializer()));
        cases.add(new Case("tts", "gpt_sovits",
                new com.github.tartaricacid.touhoulittlemaid.ai.service.tts.gptsovits.TTSGptSovitsSite.Serializer()));
        cases.add(new Case("tts", "minimax",
                new com.github.tartaricacid.touhoulittlemaid.ai.service.tts.minimax.TTSMiniMaxSite.Serializer()));
        cases.add(new Case("tts", "siliconflow",
                new com.github.tartaricacid.touhoulittlemaid.ai.service.tts.siliconflow.TTSSiliconflowSite.Serializer()));
        cases.add(new Case("stt", "aliyun",
                new com.github.tartaricacid.touhoulittlemaid.ai.service.stt.aliyun.STTAliyunSite.Serializer()));
        cases.add(new Case("stt", "siliconflow",
                new com.github.tartaricacid.touhoulittlemaid.ai.service.stt.siliconflow.STTSiliconflowSite.Serializer()));
        cases.add(new Case("stt", "tencent",
                new com.github.tartaricacid.touhoulittlemaid.ai.service.stt.tencent.STTTencentSite.Serializer()));
        assertFalse(cases.isEmpty(), "一个带密钥的站点都没有，断言的前提已变");
        return cases;
    }

    /**
     * 上面那张清单是手写的，这条负责证明它没漏：扫 {@code ai/service} 下所有站点源码，
     * 凡声明了密钥字段的类都必须被覆盖。
     *
     * <p><b>漏一个不会有任何编译或运行错误</b>——只会安静地把那一类的明文密钥发出去，
     * 而这正是本仓库反复吃亏的失败形态。</p>
     */
    @Test
    void everySecretBearingSiteClassIsCovered() throws java.io.IOException {
        java.nio.file.Path root = java.nio.file.Path.of("..", "..", "src", "main", "java", "com", "github",
                "tartaricacid", "touhoulittlemaid", "ai", "service");
        List<String> declared = new ArrayList<>();
        try (var files = java.nio.file.Files.walk(root)) {
            for (java.nio.file.Path file : files.filter(java.nio.file.Files::isRegularFile).toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith("Site.java")) {
                    continue;
                }
                String source = java.nio.file.Files.readString(file);
                if (source.contains("private String secretKey") || source.contains("protected String secretKey")
                        || source.contains("private String secretId") || source.contains("protected String secretId")) {
                    declared.add(name.substring(0, name.length() - ".java".length()));
                }
            }
        }
        assertFalse(declared.isEmpty(), "扫不到任何带密钥的站点类，本断言的前提已变");

        List<String> coveredClasses = new ArrayList<>();
        for (Case testCase : secretBearingSites()) {
            coveredClasses.add(testCase.serializer().defaultSite().getClass().getSimpleName());
        }
        for (String siteClass : declared) {
            assertTrue(coveredClasses.contains(siteClass),
                    siteClass + " 声明了密钥字段却没被脱敏测试覆盖——新增站点类型必须加进 secretBearingSites()");
        }
    }

    /**
     * {@link Site#SECRET_FIELDS} 这张表本身必须完整。
     *
     * <p>⚠️ 本类其余用例的字段集**取自这张表**（{@link #secretFieldsOf}）——那是自证式的：
     * 表里少一项，它们只是少测一项，照样全绿，而少的那一项会以明文下行。
     * 实测过：把 {@code SECRET_ID} 从表里删掉，本类原有的 7 条用例一条都不红。</p>
     *
     * <p>故判据必须**独立于那张表**：扫站点源码里声明过的密钥字段，反过来要求表里都有。
     * 这与 {@link #everySecretBearingSiteClassIsCovered} 同源——那条看管「类」，这条看管「字段」。</p>
     */
    @Test
    void theSecretFieldTableCoversEveryDeclaredSecretField() throws java.io.IOException {
        java.nio.file.Path root = java.nio.file.Path.of("..", "..", "src", "main", "java", "com", "github",
                "tartaricacid", "touhoulittlemaid", "ai", "service");
        // Java 字段名 → 它序列化成的 JSON 键。新增密钥字段时这里与 SECRET_FIELDS 都要加，
        // 而漏了哪一边都会被本用例照出来。
        Map<String, String> declaredToJsonKey = Map.of(
                "secretKey", Site.SECRET_KEY,
                "secretId", Site.SECRET_ID);

        Set<String> found = new java.util.LinkedHashSet<>();
        int scanned = 0;
        try (var files = java.nio.file.Files.walk(root)) {
            for (java.nio.file.Path file : files.filter(java.nio.file.Files::isRegularFile).toList()) {
                if (!file.getFileName().toString().endsWith("Site.java")) {
                    continue;
                }
                scanned++;
                String source = java.nio.file.Files.readString(file);
                declaredToJsonKey.forEach((javaField, jsonKey) -> {
                    if (source.contains("private String " + javaField)
                            || source.contains("protected String " + javaField)) {
                        found.add(jsonKey);
                    }
                });
            }
        }

        // 活性下限：扫描本身还活着吗（判据与结论正交——「没找到违规」与「根本没扫」不能长得一样）
        assertTrue(scanned >= 10, "只扫到 " + scanned + " 个站点类文件，源码扫描的路径或命名约定可能已失效");
        assertEquals(declaredToJsonKey.size(), found.size(),
                "源码里声明的密钥字段种类与预期不符，实得 " + found + "——新增密钥字段要同时登记进本用例与 SECRET_FIELDS");

        List<String> table = List.of(Site.SECRET_FIELDS);
        for (String jsonKey : found) {
            assertTrue(table.contains(jsonKey),
                    "站点源码里声明了密钥字段 '" + jsonKey + "'，但它不在 Site.SECRET_FIELDS 里——"
                            + "脱敏与「保持原密钥」都以那张表为准，漏项会安静地把明文发出去");
        }
    }

    private static List<String> secretFieldsOf(SerializableSite<?> serializer) {
        CompoundTag tag = encode(serializer, serializer.defaultSite());
        List<String> fields = new ArrayList<>();
        for (String field : Site.SECRET_FIELDS) {
            if (tag != null && tag.getString(field).isPresent()) {
                fields.add(field);
            }
        }
        return fields;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static CompoundTag encode(SerializableSite<?> serializer, Object site) {
        return (CompoundTag) ((SerializableSite) serializer).codec()
                .encodeStart(NbtOps.INSTANCE, site).result().orElse(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object decode(SerializableSite<?> serializer, CompoundTag tag) {
        return ((SerializableSite) serializer).codec()
                .parse(NbtOps.INSTANCE, tag).result().orElse(null);
    }

    @Test
    void aConfiguredSecretIsNeverWrittenInPlaintext() {
        for (Case testCase : secretBearingSites()) {
            CompoundTag tag = encode(testCase.serializer(), testCase.serializer().defaultSite());
            for (String field : secretFieldsOf(testCase.serializer())) {
                tag.putString(field, PLAINTEXT);
            }
            CompoundTag redacted = Site.redactSecrets(tag.copy());

            for (String field : secretFieldsOf(testCase.serializer())) {
                String value = redacted.getString(field).orElse("");
                assertNotEquals(PLAINTEXT, value,
                        testCase.service() + "/" + testCase.apiType() + " 的 " + field + " 明文下行了");
                assertEquals(Site.SECRET_KEPT, value,
                        testCase.service() + "/" + testCase.apiType() + " 的 " + field + " 应换成哨兵");
            }
        }
    }

    @Test
    void anUnconfiguredSecretStaysEmptySoTheClientCanTellThemApart() {
        for (Case testCase : secretBearingSites()) {
            CompoundTag tag = encode(testCase.serializer(), testCase.serializer().defaultSite());
            for (String field : secretFieldsOf(testCase.serializer())) {
                tag.putString(field, "");
            }
            CompoundTag redacted = Site.redactSecrets(tag.copy());

            for (String field : secretFieldsOf(testCase.serializer())) {
                assertEquals("", redacted.getString(field).orElse(null),
                        testCase.service() + "/" + testCase.apiType()
                                + " 未配置的密钥不得变成哨兵，否则界面分不出「已配置」和「未配置」");
            }
        }
    }

    /**
     * 这条就是 §8.A3 那个回归判据：只改地址、没碰密钥框，保存后密钥必须还在。
     */
    @Test
    void anUntouchedSecretSurvivesASaveThatOnlyChangedSomethingElse() {
        for (Case testCase : secretBearingSites()) {
            CompoundTag existing = encode(testCase.serializer(), testCase.serializer().defaultSite());
            for (String field : secretFieldsOf(testCase.serializer())) {
                existing.putString(field, PLAINTEXT);
            }

            // 客户端收到的是脱敏版，改了地址之后原样回传
            CompoundTag incoming = Site.redactSecrets(existing.copy());
            incoming.putString(Site.URL, "https://example.invalid/changed");

            assertTrue(Site.restoreKeptSecrets(incoming, existing), "应当检测到哨兵");
            for (String field : secretFieldsOf(testCase.serializer())) {
                assertEquals(PLAINTEXT, incoming.getString(field).orElse(""),
                        testCase.service() + "/" + testCase.apiType()
                                + "：只改了地址却把密钥弄丢了");
            }
            assertEquals("https://example.invalid/changed", incoming.getString(Site.URL).orElse(""),
                    "改动本身必须保留");
        }
    }

    @Test
    void anExplicitlyClearedSecretIsNotRestored() {
        for (Case testCase : secretBearingSites()) {
            CompoundTag existing = encode(testCase.serializer(), testCase.serializer().defaultSite());
            for (String field : secretFieldsOf(testCase.serializer())) {
                existing.putString(field, PLAINTEXT);
            }

            // 管理员点了「清除」：送回来的是空串，不是哨兵
            CompoundTag incoming = existing.copy();
            for (String field : secretFieldsOf(testCase.serializer())) {
                incoming.putString(field, "");
            }

            Site.restoreKeptSecrets(incoming, existing);
            for (String field : secretFieldsOf(testCase.serializer())) {
                assertEquals("", incoming.getString(field).orElse(null),
                        testCase.service() + "/" + testCase.apiType()
                                + "：显式清除必须生效，否则管理员永远删不掉一个密钥");
            }
        }
    }

    @Test
    void aNewlyTypedSecretReplacesTheOldOne() {
        for (Case testCase : secretBearingSites()) {
            CompoundTag existing = encode(testCase.serializer(), testCase.serializer().defaultSite());
            for (String field : secretFieldsOf(testCase.serializer())) {
                existing.putString(field, PLAINTEXT);
            }

            CompoundTag incoming = existing.copy();
            for (String field : secretFieldsOf(testCase.serializer())) {
                incoming.putString(field, "sk-brand-new-value");
            }

            Site.restoreKeptSecrets(incoming, existing);
            for (String field : secretFieldsOf(testCase.serializer())) {
                assertEquals("sk-brand-new-value", incoming.getString(field).orElse(""),
                        testCase.service() + "/" + testCase.apiType() + "：新填的密钥被旧值覆盖了");
            }
        }
    }

    /**
     * 脱敏后的 tag 必须仍然能被 codec 解出来——密钥字段在 codec 里是必需的，
     * 若为了脱敏把字段删掉，客户端会直接解析失败。
     */
    @Test
    void aRedactedSiteStillDecodes() {
        for (Case testCase : secretBearingSites()) {
            CompoundTag tag = encode(testCase.serializer(), testCase.serializer().defaultSite());
            for (String field : secretFieldsOf(testCase.serializer())) {
                tag.putString(field, PLAINTEXT);
            }
            Object decoded = decode(testCase.serializer(), Site.redactSecrets(tag.copy()));
            assertTrue(decoded instanceof LLMSite || decoded instanceof TTSSite
                            || decoded instanceof com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite,
                    testCase.service() + "/" + testCase.apiType() + " 脱敏后解不出来了");
        }
    }
}
