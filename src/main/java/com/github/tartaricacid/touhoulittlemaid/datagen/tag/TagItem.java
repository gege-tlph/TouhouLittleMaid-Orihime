package com.github.tartaricacid.touhoulittlemaid.datagen.tag;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

// B4_DATAGEN_RESTORE 已恢复（Phase 2 datagen 解冻）：既是**运行时 TagKey 常量持有类**（6 个物品 TagKey 被
//   gameplay 广泛引用），又恢复其 datagen provider 职责（extends FabricTagProvider.ItemTagProvider）。
//   1.21.11 API 迁移：getOrCreateTagBuilder → valueLookupBuilder（对象/TagKey）+ getOrCreateRawBuilder（跨模组
//   Identifier 可选引用，因新 TagAppender 不再接受 Identifier 形 add/addOptionalTag）。
public class TagItem extends FabricTagProvider.ItemTagProvider {
    public static final TagKey<Item> GOHEI_ENCHANTABLE = createTagKey("gohei_enchantable");
    public static final TagKey<Item> MAID_PLANTABLE_SEEDS = createTagKey("maid_plantable_seeds");

    /**
     * 能够驯服女仆的物品
     * <p>
     * 默认已经把所有带有 <code>#forge:cakes</code> 和 <code>#c:cakes</code> 标签的物品加入其中了
     */
    public static final TagKey<Item> MAID_TAMED_ITEM = createTagKey("maid_tamed_item");

    /**
     * 物品拥有经验修补后，女仆在吸收经验或者 P 点时能够进行修复；
     * <p>
     * 但是部分物品不能这么做，可将其加入此 tag 下
     */
    public static final TagKey<Item> MAID_MENDING_BLOCKLIST_ITEM = createTagKey("maid_mending_blocklist_item");


    /**
     * 女仆和玩家类似，在穿戴拥有消失诅咒附魔的装备（或者饰品）后死亡，其对应的物品会直接消失；
     * <p>
     * 但是部分物品不能这么做，可将其加入此 tag 下
     */
    public static final TagKey<Item> MAID_VANISHING_BLOCKLIST_ITEM = createTagKey("maid_vanishing_blocklist_item");

    /**
     * /**
     * 女仆进食黑名单，与配置文件协同作用，方便拓展兼容
     * <p>
     * 全局的，适用于工作餐、回血餐和家庭餐
     */
    public static final TagKey<Item> MAID_EAT_BLOCKLIST_ITEM = createTagKey("maid_eat_blocklist_item");

    public TagItem(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, completableFuture);
    }

    public static TagKey<Item> createTagKey(String name) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, name));
    }

    public static TagKey<Item> createTagKey(Identifier resourceLocation) {
        return TagKey.create(Registries.ITEM, resourceLocation);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        valueLookupBuilder(GOHEI_ENCHANTABLE)
                .add(InitItems.HAKUREI_GOHEI)
                .add(InitItems.SANAE_GOHEI);

        valueLookupBuilder(ItemTags.DURABILITY_ENCHANTABLE).add(InitItems.HAKUREI_GOHEI)
                .add(InitItems.SANAE_GOHEI)
                .add(InitItems.ULTRAMARINE_ORB_ELIXIR)
                .add(InitItems.EXPLOSION_PROTECT_BAUBLE)
                .add(InitItems.FIRE_PROTECT_BAUBLE)
                .add(InitItems.PROJECTILE_PROTECT_BAUBLE)
                .add(InitItems.MAGIC_PROTECT_BAUBLE)
                .add(InitItems.FALL_PROTECT_BAUBLE)
                .add(InitItems.DROWN_PROTECT_BAUBLE)
                .add(InitItems.NIMBLE_FABRIC);

        valueLookupBuilder(MAID_PLANTABLE_SEEDS)
                .forceAddTag(ItemTags.VILLAGER_PLANTABLE_SEEDS)
                .forceAddTag(ConventionalItemTags.SEEDS)
                .add(Items.NETHER_WART);
        getOrCreateRawBuilder(MAID_PLANTABLE_SEEDS)
                .addOptionalTag(Identifier.parse("kaleidoscope_cookery:cookery_mod_seeds"));

        valueLookupBuilder(MAID_TAMED_ITEM).add(Items.CAKE);
        TagBuilder tamed = getOrCreateRawBuilder(MAID_TAMED_ITEM);
        tamed.addOptionalTag(Identifier.parse("forge:cakes"));
        tamed.addOptionalTag(Identifier.parse("c:cakes"));
        tamed.addOptionalTag(Identifier.parse("jmc:cakes"));
        tamed.addOptionalElement(Identifier.parse("kawaiidishes:cheese_cake"));
        tamed.addOptionalElement(Identifier.parse("kawaiidishes:honey_cheese_cake"));
        tamed.addOptionalElement(Identifier.parse("kawaiidishes:chocolate_cheese_cake"));
        tamed.addOptionalElement(Identifier.parse("kawaiidishes:piece_of_cake"));
        tamed.addOptionalElement(Identifier.parse("kawaiidishes:piece_of_cheesecake"));
        tamed.addOptionalElement(Identifier.parse("kawaiidishes:piece_of_chocolate_cheesecake"));
        tamed.addOptionalElement(Identifier.parse("kawaiidishes:piece_of_honey_cheesecake"));

        valueLookupBuilder(MAID_MENDING_BLOCKLIST_ITEM).add(InitItems.ULTRAMARINE_ORB_ELIXIR);
        valueLookupBuilder(MAID_VANISHING_BLOCKLIST_ITEM).add(InitItems.ULTRAMARINE_ORB_ELIXIR);

        // 森罗物语辣椒
        TagBuilder eat = getOrCreateRawBuilder(MAID_EAT_BLOCKLIST_ITEM);
        eat.addOptionalElement(Identifier.parse("kaleidoscope_cookery:red_chili"));
        eat.addOptionalElement(Identifier.parse("kaleidoscope_cookery:green_chili"));
    }
}
