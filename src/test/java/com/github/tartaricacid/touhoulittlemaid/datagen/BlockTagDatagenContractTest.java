package com.github.tartaricacid.touhoulittlemaid.datagen;

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
 * 方块标签的两条契约。
 *
 * <p><b>为什么需要第一条</b>：datagen 的产物是**入库的**，而「改了 datagen 源码却忘了重跑」
 * 是一处纯静默失败——编译、打包、启动一路正常，只是玩家拿到的标签是旧的。
 * 这与本仓库已实证的九处静默注册面同族：**新写的东西要被另一个产物认领才生效**。</p>
 */
class BlockTagDatagenContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path PROJECT_ROOT = Path.of("..", "..");
    private static final Path TAG_BLOCK_SOURCE = PROJECT_ROOT.resolve(
            "src/main/java/com/github/tartaricacid/touhoulittlemaid/datagen/tag/TagBlock.java");
    private static final Path GENERATED_BLOCK_TAGS = PROJECT_ROOT.resolve(
            "src/main/generated/data/touhou_little_maid/tags/block");

    /** {@code addOptional(createResourceKey(Identifier.parse("x")))} 与 {@code addOptionalTag(createTagKey(...))} 都要抓。 */
    private static final Pattern OPTIONAL_ID = Pattern.compile(
            "addOptional(?:Tag)?\\s*\\(\\s*create(?:ResourceKey|TagKey)\\s*\\(\\s*Identifier\\.parse\\s*\\(\\s*\"([^\"]+)\"");

    /**
     * 森罗厨房与森罗酒馆的桌椅。两张标签都要覆盖：
     * {@code maid_avoid_block} 管「不要站上去」，{@code maid_jump_forbidden_block} 管「不要为了上去而起跳」。
     * 少一张，女仆就会跳上桌子然后站在上面——玩家看到的就是「女仆爬家具」。
     */
    private static final List<String> KALEIDOSCOPE_FURNITURE = List.of(
            "#kaleidoscope_cookery:table",
            "#kaleidoscope_cookery:sittable",
            "#kaleidoscope_tavern:sittable",
            "kaleidoscope_tavern:table",
            "kaleidoscope_tavern:bar_counter",
            "kaleidoscope_tavern:bar_cabinet",
            "kaleidoscope_tavern:glass_bar_cabinet");

    /** 源码里声明过的每个 optional id，都必须真的出现在入库的产物里——否则就是「忘了重跑 datagen」。 */
    @Test
    void everyOptionalIdDeclaredInSourceReachesTheGeneratedJson() throws IOException {
        String source = Files.readString(TAG_BLOCK_SOURCE, StandardCharsets.UTF_8);
        Set<String> declared = new LinkedHashSet<>();
        Matcher matcher = OPTIONAL_ID.matcher(source);
        while (matcher.find()) {
            declared.add(matcher.group(1));
        }
        // 活性判据与结论正交：识别依据（那两个工厂方法名）一变就会认出 0 个，而「全都到位」与
        // 「一个都没认出来」在结论上完全一样
        assertTrue(declared.size() >= 20,
                "只从 TagBlock 源码认出 " + declared.size() + " 个 optional id，抽取正则可能已失效");

        String allGenerated = readAllGeneratedBlockTags();
        List<String> missing = new ArrayList<>();
        for (String id : declared) {
            // 标签引用在源码里写作 "ns:path"，在 json 里是 "#ns:path"；两种形态都算命中
            if (!allGenerated.contains("\"" + id + "\"") && !allGenerated.contains("\"#" + id + "\"")) {
                missing.add(id);
            }
        }
        assertEquals(List.of(), missing,
                "这些 id 在 TagBlock 源码里声明了，却不在入库的生成产物里——多半是改完没跑 "
                + "`./gradlew runDatagen -x downloadAssets`");
    }

    /** 女仆既不站上森罗家具，也不为了登上去而起跳——两张标签缺一不可。 */
    @Test
    void kaleidoscopeFurnitureIsBothAvoidedAndJumpForbidden() throws IOException {
        for (String tagFile : List.of("maid_avoid_block.json", "maid_jump_forbidden_block.json")) {
            String json = Files.readString(GENERATED_BLOCK_TAGS.resolve(tagFile), StandardCharsets.UTF_8);
            List<String> missing = new ArrayList<>();
            for (String id : KALEIDOSCOPE_FURNITURE) {
                if (!json.contains("\"" + id + "\"")) {
                    missing.add(id);
                }
            }
            assertEquals(List.of(), missing, tagFile + " 缺少森罗家具条目");
            assertTrue(json.contains("\"required\": false"),
                    tagFile + " 里的第三方条目必须是 optional，否则会变成硬依赖");
        }
    }

    private static String readAllGeneratedBlockTags() throws IOException {
        StringBuilder all = new StringBuilder();
        int files = 0;
        try (Stream<Path> paths = Files.walk(GENERATED_BLOCK_TAGS)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".json")).toList()) {
                all.append(Files.readString(path, StandardCharsets.UTF_8));
                files++;
            }
        }
        assertTrue(files >= 5, "只读到 " + files + " 个生成的方块标签文件，产物目录可能已挪窝");
        return all.toString();
    }
}
