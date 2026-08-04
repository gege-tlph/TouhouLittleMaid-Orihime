package com.github.tartaricacid.touhoulittlemaid;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 《忆·幻想乡》手册配方的两份 json 不是 runData 产物：Patchouli 是 modCompileOnly，
 * runData 运行期没有该 mod，RecipeGenerator 里对应的构建块被冻结（见 COMPAT_PATCHOULI 注释），
 * json 按 origin/1.21.1 的 datagen 输出手工维护。重跑 runData 会清掉整个 generated 目录——
 * 本测试保证这两份文件被冲掉时门禁立刻红，而不是手册配方静默消失。
 */
public class PatchouliBookRecipeGuardTest {
    private static final String RECIPE = "src/main/generated/data/touhou_little_maid/recipe/memorizable_gensokyo.json";
    private static final String ADVANCEMENT = "src/main/generated/data/touhou_little_maid/advancement/recipes/misc/memorizable_gensokyo.json";

    @Test
    public void recipeJsonPresentAndGuarded() {
        JsonObject json = readJson(RECIPE);
        assertTrue(json.has("fabric:load_conditions"),
                "手册配方必须带 fabric:load_conditions（未装 Patchouli 时该配方应被跳过）");
        assertTrue(json.toString().contains("patchouli"),
                "load_conditions 必须以 patchouli 为条件");
        assertEquals("patchouli:guide_book",
                json.getAsJsonObject("result").get("id").getAsString(),
                "配方产物必须是 Patchouli 手册物品");
        assertEquals("touhou_little_maid:memorizable_gensokyo",
                json.getAsJsonObject("result").getAsJsonObject("components").get("patchouli:book").getAsString(),
                "手册组件必须指向本模组的 memorizable_gensokyo 书");
    }

    @Test
    public void advancementJsonPresentAndGuarded() {
        JsonObject json = readJson(ADVANCEMENT);
        assertTrue(json.has("fabric:load_conditions"),
                "手册配方解锁进度必须带 fabric:load_conditions");
        assertTrue(json.getAsJsonObject("rewards").toString().contains("memorizable_gensokyo"),
                "进度奖励必须解锁 memorizable_gensokyo 配方");
    }

    private static JsonObject readJson(String relativePath) {
        Path path = projectRoot().resolve(relativePath);
        assertTrue(Files.isRegularFile(path), "缺失文件：" + relativePath
                + "（若刚跑过 runData，请按 RecipeGenerator 的 COMPAT_PATCHOULI 注释恢复手工维护的 json）");
        try {
            return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** 与 RegistrationInvariantTest 同款：从工作目录向上找 settings.gradle 定位项目根。 */
    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("settings.gradle"))
                    || Files.isRegularFile(current.resolve("settings.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("未能从 " + Path.of("").toAbsolutePath()
                + " 向上定位到项目根目录（settings.gradle）");
    }
}
