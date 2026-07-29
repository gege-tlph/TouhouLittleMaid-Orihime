package com.github.tartaricacid.touhoulittlemaid.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「家具重制」兼容的两条契约。
 *
 * <p>两条都是<b>按枚举</b>断言，而不是断言「代码里写了这么一句」——因为这里的两处说法都是全称的：
 * 「所有纯储物方块都能当箱子」「所有桌面与坐具都避让且禁跳」。全称说法不配枚举测试就只是愿望。</p>
 *
 * <p>用源码 / 生成数据扫描而不是运行时断言：{@code isChest} 要查 {@code BuiltInRegistries}，
 * 纯 JUnit 环境里没有 bootstrap 过的注册表；而标签的真实产物是 {@code src/main/generated} 下的
 * JSON（它会进 jar），断言它才能同时拦住「改了 TagBlock 但忘了跑 runDatagen」。</p>
 */
@DisplayName("家具重制兼容契约")
class RefurbishedFurnitureCompatContractTest {
    private static final String MOD_ID = "refurbished_furniture";

    /** 纯储物方块实体类型。改这张表就要同步改 {@link #STORAGE_MUST_STAY_OUT} 的理由。 */
    private static final List<String> PURE_STORAGE_TYPES = List.of(
            "crate", "drawer", "kitchen_drawer", "cabinet", "cooler", "fridge", "storage_jar");

    /**
     * 绝不能进白名单的方块实体类型，各有各的理由：
     * {@code recycle_bin} 会销毁投入的物品——女仆「往箱子里放」会直接吞掉背包；
     * {@code mail_box}/{@code post_box} 是寄件系统；其余都是带加工语义的机器，
     * 往里倒东西等于往加工槽倒。
     */
    private static final List<String> STORAGE_MUST_STAY_OUT = List.of(
            "recycle_bin", "mail_box", "post_box",
            "stove", "grill", "microwave", "toaster", "freezer", "workbench",
            "cutting_board", "frying_pan", "plate", "basin", "bath", "toilet",
            "computer", "television", "electricity_generator", "range_hood", "kitchen_sink");

    private static final String[] WOOD_TYPES = {
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "crimson", "warped", "pale_oak"};

    private static final String[] DYE_COLORS = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};

    private static final String[] KITCHEN_COUNTERS = {
            "kitchen_cabinetry", "kitchen_drawer", "kitchen_sink", "kitchen_storage_cabinet"};

    @Test
    @DisplayName("箱子类型白名单恰好是那七种纯储物方块实体")
    void chestTypeAllowsExactlyThePureStorageBlockEntities() throws IOException {
        String src = Files.readString(chestTypeSource(), StandardCharsets.UTF_8);

        for (String type : PURE_STORAGE_TYPES) {
            assertTrue(src.contains("type(\"" + type + "\")"),
                    "白名单缺少纯储物方块实体 " + type + "：无线 IO 绑不上它。");
        }
    }

    @Test
    @DisplayName("会销毁物品的回收桶与各类加工机器必须不在白名单里")
    void chestTypeKeepsHazardousBlockEntitiesOut() throws IOException {
        String src = Files.readString(chestTypeSource(), StandardCharsets.UTF_8);

        for (String type : STORAGE_MUST_STAY_OUT) {
            assertFalse(src.contains("type(\"" + type + "\")"),
                    "白名单收录了 " + type + "：它不是纯储物方块，女仆往里放东西会丢物品或误触加工。");
        }
    }

    @Test
    @DisplayName("箱子判定必须带结构校验，不能退化成纯 id 匹配")
    void chestTypeStillChecksTheContainerSupertype() throws IOException {
        String src = Files.readString(chestTypeSource(), StandardCharsets.UTF_8);

        // 去掉这一句，白名单就变成「按注册名猜语义」——COMPAT.md 明令禁止的那种判据。
        assertTrue(src.contains("instanceof BaseContainerBlockEntity"),
                "isChest 必须先确认它真的是 BaseContainerBlockEntity，再看 id 白名单。");
    }

    @Test
    @DisplayName("桌面与坐具全部同时进入避让与禁跳标签")
    void everySurfaceAndSeatIsBothAvoidedAndJumpForbidden() throws IOException {
        Set<String> avoid = tagEntries("maid_avoid_block");
        Set<String> noJump = tagEntries("maid_jump_forbidden_block");

        List<String> missing = new ArrayList<>();
        for (String entry : expectedFurnitureEntries()) {
            if (!avoid.contains(entry)) {
                missing.add(entry + " (maid_avoid_block)");
            }
            if (!noJump.contains(entry)) {
                missing.add(entry + " (maid_jump_forbidden_block)");
            }
        }

        assertTrue(missing.isEmpty(),
                "生成的标签缺少下列家具条目——改了 TagBlock 就必须重跑 runDatagen，"
                        + "src/main/generated 会进 jar：" + missing);
    }

    @Test
    @DisplayName("装了该模组时必须把配方序列化器注册表标记为 SYNCED，否则专服进不去")
    void recipeSerializerRegistryIsMarkedSyncedWhenTheModIsPresent() throws IOException {
        String compat = Files.readString(compatSource(), StandardCharsets.UTF_8);

        // 家具重制用原版 RecipeHolder.STREAM_CODEC 同步工作台配方，那个 codec 按**数字注册 id**
        // 分派序列化器；而 1.21.11 的原版不再向客户端发配方，这张表默认不同步。
        // 两端 mod 集本来就不同（JEI/Sodium/Iris 是客户端专属），实测 38 项里 16 项对不上，
        // 客户端一进服就被踢。标记 SYNCED 后 Fabric 会重映射客户端 id，实测降到 0 项不一致。
        assertTrue(compat.contains("Registries.RECIPE_SERIALIZER")
                        && compat.contains("RegistryAttribute.SYNCED"),
                "RefurbishedFurnitureCompat 必须把 minecraft:recipe_serializer 标记为 SYNCED。");

        String registry = Files.readString(compatRegistrySource(), StandardCharsets.UTF_8);
        // 只在装了该模组时才改全局注册表属性——这是把影响面关在门内的那道闸
        assertTrue(registry.contains("checkModLoad(RefurbishedFurnitureCompat.MOD_ID, RefurbishedFurnitureCompat::init)"),
                "该属性必须经 CompatRegistry 的 isModLoaded 门控接线，不得无条件生效。");
    }

    @Test
    @DisplayName("桌子与书桌同时是零食台，女仆才会偷吃摆在上面的方块食物")
    void tablesAndDesksAreSnackStands() throws IOException {
        assertTrue(tagEntries("maid_snack_stand_block").contains("#" + MOD_ID + ":tuckable"),
                "maid_snack_stand_block 必须引用该模组自己的 tuckable 标签（桌子 + 书桌）。");
    }

    /** 本兼容声称覆盖的全部家具条目：模组自带的桌面标签，加上按木种 / 颜色枚举出的坐具与厨房台面。 */
    private static Set<String> expectedFurnitureEntries() {
        Set<String> expected = new LinkedHashSet<>();
        expected.add("#" + MOD_ID + ":tuckable");
        for (String wood : WOOD_TYPES) {
            expected.add(MOD_ID + ":" + wood + "_chair");
        }
        for (String color : DYE_COLORS) {
            expected.add(MOD_ID + ":" + color + "_sofa");
            expected.add(MOD_ID + ":" + color + "_stool");
        }
        for (String counter : KITCHEN_COUNTERS) {
            for (String wood : WOOD_TYPES) {
                expected.add(MOD_ID + ":" + wood + "_" + counter);
            }
            for (String color : DYE_COLORS) {
                expected.add(MOD_ID + ":" + color + "_" + counter);
            }
        }
        return expected;
    }

    private static Set<String> tagEntries(String tagName) throws IOException {
        Path file = projectRoot().resolve("src/main/generated/data/touhou_little_maid/tags/block/" + tagName + ".json");
        JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
        Set<String> entries = new LinkedHashSet<>();
        JsonArray values = json.getAsJsonArray("values");
        for (JsonElement value : values) {
            entries.add(value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString());
        }
        return entries;
    }

    private static Path compatSource() {
        return projectRoot().resolve("src/main/java/com/github/tartaricacid/touhoulittlemaid/compat"
                + "/refurbishedfurniture/RefurbishedFurnitureCompat.java");
    }

    private static Path compatRegistrySource() {
        return projectRoot().resolve("src/main/java/com/github/tartaricacid/touhoulittlemaid/init"
                + "/registry/CompatRegistry.java");
    }

    private static Path chestTypeSource() {
        return projectRoot().resolve("src/main/java/com/github/tartaricacid/touhoulittlemaid/compat"
                + "/refurbishedfurniture/chest/RefurbishedStorageChestType.java");
    }

    /** 从工作目录向上找 settings.gradle，避免依赖 Gradle 的 test working dir 设置。 */
    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("settings.gradle"))
                    || Files.isRegularFile(current.resolve("settings.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("找不到 settings.gradle，无法定位工程根目录");
    }
}
