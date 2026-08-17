package com.github.tartaricacid.touhoulittlemaid.datagen.tag;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.concurrent.CompletableFuture;

public class TagBlock extends FabricTagsProvider.BlockTagsProvider {
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
     * 所有颜色的女仆床方块
     */
    public static final TagKey<Block> MAID_BED = createTagKey("maid_bed");

    /**
     * CarryOn 黑名单标签，被此标签包含的方块将无法被 CarryOn 抱起
     */
    public static final TagKey<Block> CARRYON_BLOCK_BLACKLIST = createTagKey(Identifier.parse("carryon:block_blacklist"));

    public TagBlock(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    public static TagKey<Block> createTagKey(String name) {
        return TagKey.create(Registries.BLOCK, IdentifierUtil.modLoc(name));
    }

    public static TagKey<Block> createTagKey(Identifier resourceLocation) {
        return TagKey.create(Registries.BLOCK, resourceLocation);
    }

    public static ResourceKey<Block> createResourceKey(Identifier resourceLocation) {
        return ResourceKey.create(Registries.BLOCK, resourceLocation);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        valueLookupBuilder(MAID_BED)
                .add(InitBlocks.PINK_MAID_BED)
                .add(InitBlocks.WHITE_MAID_BED)
                .add(InitBlocks.BLACK_MAID_BED)
                .add(InitBlocks.YELLOW_MAID_BED)
                .add(InitBlocks.BLUE_MAID_BED)
                .add(InitBlocks.GREEN_MAID_BED)
                .add(InitBlocks.PURPLE_MAID_BED);

        valueLookupBuilder(MAID_JUMP_FORBIDDEN_BLOCK)
                .forceAddTag(BlockTags.DOORS)
                .forceAddTag(BlockTags.FENCES)
                .forceAddTag(BlockTags.CLIMBABLE);
        addKaleidoscopeFurniture(MAID_JUMP_FORBIDDEN_BLOCK);

        valueLookupBuilder(ALTAR_TORII).add(Blocks.RED_WOOL, Blocks.RED_CONCRETE);
        builder(ALTAR_TORII).addOptional(createResourceKey(Identifier.parse("biomesoplenty:redwood_planks")));
        valueLookupBuilder(ALTAR_PILLAR).forceAddTag(BlockTags.LOGS);

        var blacklist = valueLookupBuilder(CARRYON_BLOCK_BLACKLIST);
        BuiltInRegistries.BLOCK.keySet().stream().filter(id -> id.getNamespace().equals(TouhouLittleMaid.MOD_ID))
                .forEach(id -> blacklist.add(BuiltInRegistries.BLOCK.getValue(id)));

        valueLookupBuilder(MAID_SNACK_STAND_BLOCK)
                .add(InitBlocks.SNACK_CABINET)
                .addOptionalTag(createTagKey(Identifier.parse("kaleidoscope_cookery:table")));

        valueLookupBuilder(SNACK_CABINET_FULL)
                // 蛋糕全部是完整玻璃橱窗
                .add(Blocks.CAKE)
                .addOptionalTag(createTagKey(Identifier.parse("forge:cakes")))
                .addOptionalTag(createTagKey(Identifier.parse("c:cakes")))
                .addOptionalTag(createTagKey(Identifier.parse("jmc:cakes")));
        // 农夫乐事的盛宴
        builder(SNACK_CABINET_FULL)
                .addOptional(createResourceKey(Identifier.parse("farmersdelight:roast_chicken_block")))
                .addOptional(createResourceKey(Identifier.parse("farmersdelight:stuffed_pumpkin_block")))
                .addOptional(createResourceKey(Identifier.parse("farmersdelight:honey_glazed_ham_block")))
                .addOptional(createResourceKey(Identifier.parse("farmersdelight:shepherds_pie_block")))
                .addOptional(createResourceKey(Identifier.parse("farmersdelight:rice_roll_medley_block")));

        builder(SNACK_CABINET_HALF)
                // 农夫乐事的糕点
                .addOptional(createResourceKey(Identifier.parse("farmersdelight:apple_pie")))
                .addOptional(createResourceKey(Identifier.parse("farmersdelight:sweet_berry_cheesecake")))
                .addOptional(createResourceKey(Identifier.parse("farmersdelight:chocolate_pie")))
                // 森罗物语的方块菜，后续应该让森罗物语添加专门的 tag
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:dark_cuisine")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:suspicious_stir_fry")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:slime_ball_meal")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:fondant_pie")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:dongpo_pork")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:fondant_spider_eye")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:chorus_fried_egg")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:braised_fish")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:golden_salad")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:spicy_chicken")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:yakitori")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:pan_seared_knight_steak")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:stargazy_pie")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:sweet_and_sour_ender_pearls")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:crystal_lamb_chop")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:blaze_lamb_chop")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:frost_lamb_chop")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:nether_style_sashimi")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:end_style_sashimi")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:desert_style_sashimi")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:tundra_style_sashimi")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:cold_style_sashimi")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:shengjian_mantou")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:candied_potato")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:dough_drop_soup")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:stuffed_tiger_skin_pepper")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:spicy_rabbit_head")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:four_joy_meatball_soup")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:numbing_spicy_chicken")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:fried_caterpillar")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:fried_spring_roll")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:spicy_blood_stew")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:fruit_platter")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:braised_pork_ribs")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:cold_roasted_meat")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:oil_splashed_fish")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:brown_mushroom_pot_soup")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:red_mushroom_pot_soup")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:warped_fungus_pot_soup")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:crimson_fungus_pot_soup")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_cookery:buddha_jumps_over_the_wall")));

        addKaleidoscopeFurniture(MAID_AVOID_BLOCK);
        builder(MAID_AVOID_BLOCK)
                // 怎么能在吃饭的桌子上跳来跳去呢
                .addTag(MAID_SNACK_STAND_BLOCK)
                // 机械动力
                .addOptional(createResourceKey(Identifier.parse("create:mechanical_saw")))
                .addOptional(createResourceKey(Identifier.parse("create:crushing_wheel")))
                .addOptional(createResourceKey(Identifier.parse("create:crushing_wheel_controller")))
                // 黄蜂领域
                .addOptional(createResourceKey(Identifier.parse("the_bumblezone:heavy_air")))
                .addOptional(createResourceKey(Identifier.parse("the_bumblezone:windy_air")))
                // 农夫乐事
                .addOptional(createResourceKey(Identifier.parse("farmersdelight:stove")))
                // 暮色森林
                .addOptional(createResourceKey(Identifier.parse("twilightforest:hedge")))
                .addOptional(createResourceKey(Identifier.parse("twilightforest:fiery_block")))
                .addOptional(createResourceKey(Identifier.parse("twilightforest:knightmetal_block")))
                // Alex 的洞穴
                .addOptional(createResourceKey(Identifier.parse("alexscaves:primal_magma")))
                .addOptional(createResourceKey(Identifier.parse("alexscaves:primal_magma")))
                // MEK 反应堆的聚变堆和超临界移相器
                .addOptional(createResourceKey(Identifier.parse("mekanismgenerators:fusion_reactor_frame")))
                .addOptional(createResourceKey(Identifier.parse("mekanism:sps_casing")))
                // 机械动力附属的铁丝网
                .addOptional(createResourceKey(Identifier.parse("createaddition:barbed_wire")))
                // 沉浸工程的铁丝网
                .addOptional(createResourceKey(Identifier.parse("immersiveengineering:razor_wire")))
                // 铁魔法的两个火堆
                .addOptional(createResourceKey(Identifier.parse("irons_spellbooks:brazier")))
                .addOptional(createResourceKey(Identifier.parse("irons_spellbooks:brazier_soul")))
                // 刷怪塔实用设备的锥刺和研磨机
                .addOptional(createResourceKey(Identifier.parse("mob_grinding_utils:spikes")))
                .addOptional(createResourceKey(Identifier.parse("mob_grinding_utils:saw")));
    }

    /**
     * 森罗厨房与森罗酒馆的桌椅：女仆既不该站上去，也不该为了登上去而起跳。
     *
     * <p>优先用这两个模组**自己的语义标签**，这样它们新增木材/颜色变种时不用我们跟着改；
     * 全部走 optional，因此不构成硬依赖。酒馆没有暴露通用的桌子/家具标签，只能逐个列方块。</p>
     *
     * <p>注：{@code kaleidoscope_cookery:table} 经 {@code MAID_SNACK_STAND_BLOCK} 已被
     * {@code MAID_AVOID_BLOCK} 间接包含，此处直列是为了让两张标签各自独立成立，
     * 不依赖那条间接链——重复条目对生成结果无影响。</p>
     */
    private void addKaleidoscopeFurniture(TagKey<Block> tag) {
        valueLookupBuilder(tag)
                .addOptionalTag(createTagKey(Identifier.parse("kaleidoscope_cookery:table")))
                .addOptionalTag(createTagKey(Identifier.parse("kaleidoscope_cookery:sittable")))
                .addOptionalTag(createTagKey(Identifier.parse("kaleidoscope_tavern:sittable")));
        builder(tag)
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_tavern:table")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_tavern:bar_counter")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_tavern:bar_cabinet")))
                .addOptional(createResourceKey(Identifier.parse("kaleidoscope_tavern:glass_bar_cabinet")));
    }
}
