package com.github.tartaricacid.touhoulittlemaid.datagen.tag;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

// B4_DATAGEN_RESTORE 已恢复（Phase 2 datagen 解冻）：既是**运行时 TagKey 常量持有类**（女仆寻路/可食/避让方块
//   TagKey 被 gameplay 引用），又恢复其 datagen provider 职责（extends FabricTagProvider.BlockTagProvider）。
//   1.21.11 迁移：getOrCreateTagBuilder → valueLookupBuilder（对象/TagKey）+ getOrCreateRawBuilder（跨模组 Identifier 可选引用）。
public class TagBlock extends FabricTagProvider.BlockTagProvider {
    /**
     * 女仆有时候会在一些不该触发跳跃逻辑的方块上反复尝试跳来跳去，
     * 故添加此标签来将一些方块放入黑名单中
     */
    public static final TagKey<Block> MAID_JUMP_FORBIDDEN_BLOCK = createTagKey("maid_jump_forbidden_block");

    /**
     * 女仆避让方块标签，女仆在寻路、传送时会尽可能避让这些方块
     */
    public static final TagKey<Block> MAID_AVOID_BLOCK = createTagKey("maid_avoid_block");

    /**
     * 在修建祭坛时，可以当做祭坛鸟居部分的方块
     */
    public static final TagKey<Block> ALTAR_TORII = createTagKey("altar_torii");

    /**
     * 在修建祭坛时，可以当做祭坛柱子材料的方块；
     * <p>
     * 默认已经包含 <code>#minecraft:logs</code> 标签
     */
    public static final TagKey<Block> ALTAR_PILLAR = createTagKey("altar_pillar");

    /**
     * 女仆有偷吃方块食物的机制，但是这可能会误把一些拿来做装饰的食物方块也偷吃掉
     * <p>
     * 故我们现在为一些方块添加 tag，只有放在此方块上承载的食物方块女仆才会偷吃
     */
    public static final TagKey<Block> MAID_SNACK_STAND_BLOCK = createTagKey("maid_snack_stand_block");

    /**
     * 零食柜会在上方摆放特定方块时，渲染出玻璃橱窗的效果
     * <p>
     * 在此标签中的方块才会让下方零食柜渲染完整玻璃橱窗
     */
    public static final TagKey<Block> SNACK_CABINET_FULL = createTagKey("snack_cabinet_full");

    /**
     * 在此标签中的方块才会让下方零食柜渲染半高玻璃橱窗
     */
    public static final TagKey<Block> SNACK_CABINET_HALF = createTagKey("snack_cabinet_half");

    /**
     * CarryOn 黑名单标签，被此标签包含的方块将无法被 CarryOn 抱起
     */
    public static final TagKey<Block> CARRYON_BLOCK_BLACKLIST = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("carryon", "block_blacklist"));

    /**
     * 家具重制的木种变体前缀（与该模组自己的 {@code tuckable} 标签成员一一对应）。
     * 原版没有涵盖 {@code pale_oak} 之外全部值的现成枚举，故显式列出。
     */
    private static final String[] REFURBISHED_WOOD_TYPES = {
            "oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "crimson", "warped", "pale_oak"
    };

    /**
     * 家具重制的厨房台面家族。这几类会连成一整排台面，女仆踩上去与踩桌子是一回事。
     */
    private static final String[] REFURBISHED_KITCHEN_COUNTERS = {
            "kitchen_cabinetry", "kitchen_drawer", "kitchen_sink", "kitchen_storage_cabinet"
    };

    public TagBlock(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    public static TagKey<Block> createTagKey(String name) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, name));
    }

    public static TagKey<Block> createTagKey(Identifier resourceLocation) {
        return TagKey.create(Registries.BLOCK, resourceLocation);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        valueLookupBuilder(MAID_JUMP_FORBIDDEN_BLOCK)
                .forceAddTag(BlockTags.DOORS)
                .forceAddTag(BlockTags.FENCES)
                .forceAddTag(BlockTags.CLIMBABLE);
        addKaleidoscopeFurniture(getOrCreateRawBuilder(MAID_JUMP_FORBIDDEN_BLOCK));
        addRefurbishedFurniture(getOrCreateRawBuilder(MAID_JUMP_FORBIDDEN_BLOCK));

        valueLookupBuilder(ALTAR_TORII)
                .add(Blocks.RED_WOOL)
                .add(Blocks.RED_CONCRETE);
        getOrCreateRawBuilder(ALTAR_TORII).addOptionalElement(Identifier.parse("biomesoplenty:redwood_planks"));

        valueLookupBuilder(ALTAR_PILLAR).forceAddTag(BlockTags.LOGS);

        valueLookupBuilder(MAID_SNACK_STAND_BLOCK).add(InitBlocks.SNACK_CABINET);
        getOrCreateRawBuilder(MAID_SNACK_STAND_BLOCK).addOptionalTag(Identifier.parse("kaleidoscope_cookery:table"));
        // Tavern has no shared table tag. Keep these exact furniture ids optional so
        // the base mod neither loads Tavern classes nor creates a hard dependency.
        getOrCreateRawBuilder(MAID_SNACK_STAND_BLOCK)
                .addOptionalElement(Identifier.parse("kaleidoscope_tavern:table"))
                .addOptionalElement(Identifier.parse("kaleidoscope_tavern:bar_counter"));
        // 家具重制的桌子与书桌。该模组唯一的方块标签就是 tuckable（可把椅子塞进去的桌面），
        // 语义正好是「桌面」，新增木种会自动进来。
        getOrCreateRawBuilder(MAID_SNACK_STAND_BLOCK).addOptionalTag(Identifier.parse("refurbished_furniture:tuckable"));

        // 蛋糕全部是完整玻璃橱窗
        valueLookupBuilder(SNACK_CABINET_FULL).add(Blocks.CAKE);
        TagBuilder full = getOrCreateRawBuilder(SNACK_CABINET_FULL);
        full.addOptionalTag(Identifier.parse("forge:cakes"));
        full.addOptionalTag(Identifier.parse("c:cakes"));
        full.addOptionalTag(Identifier.parse("jmc:cakes"));
        // 农夫乐事的盛宴
        full.addOptionalElement(Identifier.parse("farmersdelight:roast_chicken_block"));
        full.addOptionalElement(Identifier.parse("farmersdelight:stuffed_pumpkin_block"));
        full.addOptionalElement(Identifier.parse("farmersdelight:honey_glazed_ham_block"));
        full.addOptionalElement(Identifier.parse("farmersdelight:shepherds_pie_block"));
        full.addOptionalElement(Identifier.parse("farmersdelight:rice_roll_medley_block"));

        TagBuilder half = getOrCreateRawBuilder(SNACK_CABINET_HALF);
        // 农夫乐事的糕点
        half.addOptionalElement(Identifier.parse("farmersdelight:apple_pie"));
        half.addOptionalElement(Identifier.parse("farmersdelight:sweet_berry_cheesecake"));
        half.addOptionalElement(Identifier.parse("farmersdelight:chocolate_pie"));
        // 森罗物语的方块菜，后续应该让森罗物语添加专门的 tag
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:dark_cuisine"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:suspicious_stir_fry"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:slime_ball_meal"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:fondant_pie"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:dongpo_pork"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:fondant_spider_eye"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:chorus_fried_egg"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:braised_fish"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:golden_salad"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:spicy_chicken"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:yakitori"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:pan_seared_knight_steak"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:stargazy_pie"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:sweet_and_sour_ender_pearls"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:crystal_lamb_chop"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:blaze_lamb_chop"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:frost_lamb_chop"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:nether_style_sashimi"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:end_style_sashimi"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:desert_style_sashimi"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:tundra_style_sashimi"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:cold_style_sashimi"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:shengjian_mantou"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:candied_potato"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:dough_drop_soup"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:stuffed_tiger_skin_pepper"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:spicy_rabbit_head"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:four_joy_meatball_soup"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:numbing_spicy_chicken"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:fried_caterpillar"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:fried_spring_roll"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:spicy_blood_stew"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:fruit_platter"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:braised_pork_ribs"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:cold_roasted_meat"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:oil_splashed_fish"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:brown_mushroom_pot_soup"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:red_mushroom_pot_soup"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:warped_fungus_pot_soup"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:crimson_fungus_pot_soup"));
        half.addOptionalElement(Identifier.parse("kaleidoscope_cookery:buddha_jumps_over_the_wall"));

        // 怎么能在吃饭的桌子上跳来跳去呢
        valueLookupBuilder(MAID_AVOID_BLOCK).addTag(MAID_SNACK_STAND_BLOCK);
        TagBuilder avoid = getOrCreateRawBuilder(MAID_AVOID_BLOCK);
        addKaleidoscopeFurniture(avoid);
        addRefurbishedFurniture(avoid);
        // 机械动力
        avoid.addOptionalElement(Identifier.parse("create:mechanical_saw"));
        avoid.addOptionalElement(Identifier.parse("create:crushing_wheel"));
        avoid.addOptionalElement(Identifier.parse("create:crushing_wheel_controller"));
        // 黄蜂领域
        avoid.addOptionalElement(Identifier.parse("the_bumblezone:heavy_air"));
        avoid.addOptionalElement(Identifier.parse("the_bumblezone:windy_air"));
        // 农夫乐事
        avoid.addOptionalElement(Identifier.parse("farmersdelight:stove"));
        // 暮色森林
        avoid.addOptionalElement(Identifier.parse("twilightforest:hedge"));
        avoid.addOptionalElement(Identifier.parse("twilightforest:fiery_block"));
        avoid.addOptionalElement(Identifier.parse("twilightforest:knightmetal_block"));
        // Alex 的洞穴
        avoid.addOptionalElement(Identifier.parse("alexscaves:primal_magma"));
        avoid.addOptionalElement(Identifier.parse("alexscaves:primal_magma"));
        // MEK 反应堆的聚变堆和超临界移相器
        avoid.addOptionalElement(Identifier.parse("mekanismgenerators:fusion_reactor_frame"));
        avoid.addOptionalElement(Identifier.parse("mekanism:sps_casing"));
        // 机械动力附属的铁丝网
        avoid.addOptionalElement(Identifier.parse("createaddition:barbed_wire"));
        // 沉浸工程的铁丝网
        avoid.addOptionalElement(Identifier.parse("immersiveengineering:razor_wire"));
        // 铁魔法的两个火堆
        avoid.addOptionalElement(Identifier.parse("irons_spellbooks:brazier"));
        avoid.addOptionalElement(Identifier.parse("irons_spellbooks:brazier_soul"));
        // 刷怪塔实用设备的锥刺和研磨机
        avoid.addOptionalElement(Identifier.parse("mob_grinding_utils:spikes"));
        avoid.addOptionalElement(Identifier.parse("mob_grinding_utils:saw"));

        TagBuilder blacklist = getOrCreateRawBuilder(CARRYON_BLOCK_BLACKLIST);
        BuiltInRegistries.BLOCK.keySet().stream()
                .filter(id -> id.getNamespace().equals(TouhouLittleMaid.MOD_ID))
                .forEach(blacklist::addElement);
    }

    private static void addKaleidoscopeFurniture(TagBuilder builder) {
        // Prefer the mods' own semantic tags so new wood/color variants are
        // covered without making either compatibility a hard dependency.
        builder.addOptionalTag(Identifier.parse("kaleidoscope_cookery:table"));
        builder.addOptionalTag(Identifier.parse("kaleidoscope_cookery:sittable"));
        builder.addOptionalTag(Identifier.parse("kaleidoscope_tavern:sittable"));

        // Kaleidoscope Tavern does not expose a general furniture/table tag.
        builder.addOptionalElement(Identifier.parse("kaleidoscope_tavern:table"));
        builder.addOptionalElement(Identifier.parse("kaleidoscope_tavern:bar_counter"));
        builder.addOptionalElement(Identifier.parse("kaleidoscope_tavern:bar_cabinet"));
        builder.addOptionalElement(Identifier.parse("kaleidoscope_tavern:glass_bar_cabinet"));
    }

    /**
     * MrCrayfish 的家具：重制。与森罗那批同一套处理：桌面与坐具既避让又禁跳。
     * <p>
     * 该模组只发布了一个方块标签 {@code tuckable}（= 11 种木头的桌子与书桌），正好是「桌面」那一类，
     * 直接引用它，新木种自动覆盖。其余家具的 {@code sofas} / {@code stools} / {@code kitchen} 等
     * <b>都是物品标签</b>，方块标签引用不到，只能按 id 枚举；好在 id 是「木种或颜色 × 家具类型」的
     * 规则组合，按前缀生成即可，且全部走 optional，未装该模组时不产生任何引用。
     * <p>
     * 收录的是<b>桌面与坐具</b>：桌子、书桌、椅子、沙发、圆凳，以及连成一排的厨房台面
     * （台柜 / 抽屉柜 / 水槽 / 储物台柜）。<b>不收</b>独立的储物家具（板条箱、储物罐、储物柜、
     * 冷藏箱、冰箱、抽屉柜），它们在玩家直觉里更接近箱子而不是桌面；女仆能把它们当容器用，
     * 见 {@code RefurbishedStorageChestType}。
     */
    private static void addRefurbishedFurniture(TagBuilder builder) {
        builder.addOptionalTag(Identifier.parse("refurbished_furniture:tuckable"));

        for (String wood : REFURBISHED_WOOD_TYPES) {
            builder.addOptionalElement(refurbished(wood + "_chair"));
        }
        for (DyeColor color : DyeColor.values()) {
            builder.addOptionalElement(refurbished(color.getSerializedName() + "_sofa"));
            builder.addOptionalElement(refurbished(color.getSerializedName() + "_stool"));
        }
        // 厨房台面：木种与颜色两套变体共用同一批后缀
        for (String counter : REFURBISHED_KITCHEN_COUNTERS) {
            for (String wood : REFURBISHED_WOOD_TYPES) {
                builder.addOptionalElement(refurbished(wood + "_" + counter));
            }
            for (DyeColor color : DyeColor.values()) {
                builder.addOptionalElement(refurbished(color.getSerializedName() + "_" + counter));
            }
        }
    }

    private static Identifier refurbished(String path) {
        return Identifier.fromNamespaceAndPath("refurbished_furniture", path);
    }
}
