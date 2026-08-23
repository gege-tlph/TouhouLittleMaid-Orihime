package com.github.tartaricacid.touhoulittlemaid.datagen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code create:automation_ignore} 这条数据契约的三道闸。
 *
 * <p><b>它守的是什么</b>：祭坛配方序列化器登记进机械动力的「不可自动化」标签，
 * 于是机械手 / 搅拌机 / 压床 / 动力锯 / 工厂面板都不会替玩家跑祭坛合成。
 * 这是行为基准就有的有意设计，26.1.2 上仍然活着（Create Fly 的 mod id 就是 {@code create}）。</p>
 *
 * <p><b>为什么三条都必要</b>：这条链上有三处各自独立的静默失败面——
 * ① provider 不登记进 {@code DataGenerator}，datagen 静默跳过（第 N 处「要被另一张表认领才生效」）；
 * ② 改了 datagen 源码忘了重跑，入库产物是旧的；
 * ③ 条目 id 写成字面量，而宿主已经把序列化器从基准的 {@code altar_recipe_serializers}
 * 改名成 {@code altar_recipe}——由于条目是 {@code required: false}，指向一个不存在的 id
 * <b>永远不会报错</b>，标签从此静默失效。三条谁都替不了谁。</p>
 */
class RecipeSerializerTagDatagenContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path PROJECT_ROOT = Path.of("..", "..");
    private static final Path PROVIDER_SOURCE = PROJECT_ROOT.resolve(
            "src/main/java/com/github/tartaricacid/touhoulittlemaid/datagen/tag/TagRecipeSerializer.java");
    private static final Path DATA_GENERATOR_SOURCE = PROJECT_ROOT.resolve(
            "src/main/java/com/github/tartaricacid/touhoulittlemaid/datagen/DataGenerator.java");
    private static final Path INIT_RECIPES_SOURCE = PROJECT_ROOT.resolve(
            "src/main/java/com/github/tartaricacid/touhoulittlemaid/init/InitRecipes.java");
    private static final Path GENERATED_TAG = PROJECT_ROOT.resolve(
            "src/main/generated/data/create/tags/recipe_serializer/automation_ignore.json");

    private static final Pattern REGISTER_SERIALIZER =
            Pattern.compile("registerSerializer\\s*\\(\\s*\"([^\"]+)\"");

    /** provider 不登记就永不执行——编译、打包、启动一路正常，只是产物永远不更新。 */
    @Test
    void providerIsRegisteredInDataGenerator() throws IOException {
        String source = stripComments(Files.readString(DATA_GENERATOR_SOURCE, StandardCharsets.UTF_8));
        assertTrue(source.contains("TagRecipeSerializer::new"),
                "TagRecipeSerializer 没有登记进 DataGenerator 的 pack.addProvider —— datagen 会静默跳过它");
    }

    /**
     * 入库产物必须落在机械动力的标签路径上，且条目是 optional。
     *
     * <p>{@code required: false} 不是可选项：机械动力不在场时，硬条目会让整份标签加载失败。</p>
     */
    @Test
    void generatedArtifactLandsOnCreateAutomationIgnoreAndStaysOptional() throws IOException {
        assertTrue(Files.isRegularFile(GENERATED_TAG),
                "缺少 " + GENERATED_TAG + " —— 多半是改完 datagen 没跑 `./gradlew runDatagen`");
        String json = Files.readString(GENERATED_TAG, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"required\": false"),
                "机械动力是软兼容，条目必须写成 addOptional，否则它不在场时标签加载失败");
    }

    /**
     * 产物里的条目 id 必须等于 {@code InitRecipes} 真正注册的那个 id。
     *
     * <p>这一条同时是另外两条的活性证明：它从 {@code InitRecipes} 源码机械重算期望值，
     * 与被断言的产物内容互相独立。</p>
     */
    @Test
    void generatedEntryIdMatchesTheSerializerActuallyRegistered() throws IOException {
        String initRecipes = stripComments(Files.readString(INIT_RECIPES_SOURCE, StandardCharsets.UTF_8));
        Matcher matcher = REGISTER_SERIALIZER.matcher(initRecipes);
        assertTrue(matcher.find(),
                "没能从 InitRecipes 源码里认出任何 registerSerializer(\"…\") —— 抽取正则已失效，"
                + "本条断言会退化成恒真");
        String expected = "touhou_little_maid:" + matcher.group(1);
        assertFalse(matcher.find(),
                "InitRecipes 现在注册了不止一个序列化器，本断言的「唯一那一个」前提已不成立，需重写");

        String json = Files.readString(GENERATED_TAG, StandardCharsets.UTF_8);
        assertTrue(json.contains("\"" + expected + "\""),
                "产物里的条目不是 " + expected + " —— 序列化器改过名而 datagen 没重跑；"
                + "因为条目是 required:false，这种错永远不会在运行期报出来。产物实际内容：\n" + json);
    }

    /**
     * provider 不许把条目 id 写成字面量——必须从注册表反查。
     *
     * <p>写死一次就与 {@code InitRecipes} 分家，而上一条断言只在「有人重跑了 datagen」时才照得出来。</p>
     */
    @Test
    void entryIdIsDerivedFromTheRegistryRatherThanHardcoded() throws IOException {
        String source = stripComments(Files.readString(PROVIDER_SOURCE, StandardCharsets.UTF_8));
        assertTrue(source.contains("getResourceKey"),
                "TagRecipeSerializer 必须从注册表反查序列化器的 key");
        assertTrue(source.contains("InitRecipes.ALTAR_RECIPE_SERIALIZER"),
                "反查的对象必须是 InitRecipes.ALTAR_RECIPE_SERIALIZER 本身");
        assertEquals(-1, source.indexOf("\"touhou_little_maid:"),
                "剥掉注释后仍出现 touhou_little_maid: 字面量 —— 条目 id 被写死了，会与 InitRecipes 分家");
    }

    /** 剥注释后去掉全部空白比较字符流：注释可能紧贴代码，按空白分词会产生假阳性。 */
    private static String stripComments(String source) {
        String noBlock = source.replaceAll("(?s)/\\*.*?\\*/", " ");
        return noBlock.replaceAll("(?m)//.*$", " ");
    }
}
