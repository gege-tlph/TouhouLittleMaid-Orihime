package com.github.tartaricacid.touhoulittlemaid.datagen.tag;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

import java.util.concurrent.CompletableFuture;

// B4_DATAGEN_RESTORE 已恢复（Phase 2 datagen 解冻）：既是**运行时 TagKey 常量持有类**（妖精目标/骑乘 tag 被
//   gameplay 引用），又恢复其 datagen provider 职责（extends FabricTagProvider.EntityTypeTagProvider）。
//   1.21.11 迁移：getOrCreateTagBuilder → valueLookupBuilder（对象/TagKey）+ getOrCreateRawBuilder（跨模组 Identifier 可选引用）。
public class TagEntity extends FabricTagProvider.EntityTypeTagProvider {
    /**
     * 女仆妖精的攻击目标，默认仅攻击铁傀儡和玩家
     */
    public static TagKey<EntityType<?>> MAID_FAIRY_ATTACK_GOAL = createTagKey("maid_fairy_attack_goal");

    /**
     * 女仆在骑乘时，为了朝向一致，会强制同步女仆朝向和当前骑乘实体朝向；
     * <p>
     * 但是部分模组（如机械动力）这么做反而会导致女仆异常旋转，故添加此标签
     */
    public static TagKey<EntityType<?>> MAID_VEHICLE_ROTATE_BLOCKLIST = createTagKey("maid_vehicle_rotate_blocklist");

    public static TagKey<EntityType<?>> CARRYON_ENTITY_BLACKLIST = TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath("carryon", "entity_blacklist"));

    /**
     * 冰与火的石化效果免疫标签
     */
    public static final TagKey<EntityType<?>> IMMUNE_TO_GORGON_STONE = createTagKey(
            Identifier.parse("iceandfire:immune_to_gorgon_stone")
    );

    public TagEntity(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    private static TagKey<EntityType<?>> createTagKey(String name) {
        return TagKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, name));
    }

    private static TagKey<EntityType<?>> createTagKey(Identifier id) {
        return TagKey.create(Registries.ENTITY_TYPE, id);
    }

    @Override
    protected void addTags(HolderLookup.Provider lookupProvider) {
        valueLookupBuilder(EntityTypeTags.IMPACT_PROJECTILES).add(InitEntities.DANMAKU);
        valueLookupBuilder(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS).add(InitEntities.FAIRY);
        valueLookupBuilder(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES).add(InitEntities.FAIRY);
        valueLookupBuilder(EntityTypeTags.FALL_DAMAGE_IMMUNE).add(InitEntities.FAIRY);

        valueLookupBuilder(MAID_FAIRY_ATTACK_GOAL).add(EntityType.IRON_GOLEM);
        TagBuilder attackGoal = getOrCreateRawBuilder(MAID_FAIRY_ATTACK_GOAL);
        attackGoal.addOptionalElement(Identifier.parse("guardvillagers:guard"));
        attackGoal.addOptionalElement(Identifier.parse("earthtojavamobs:furnace_golem"));
        attackGoal.addOptionalElement(Identifier.parse("earthmobsmod:furnace_golem"));
        attackGoal.addOptionalElement(Identifier.parse("mutantmonsters:mutant_snow_golem"));
        attackGoal.addOptionalElement(Identifier.parse("alexscaves:gingerbread_man"));
        attackGoal.addOptionalElement(Identifier.parse("alexsmobs:bunfungus"));

        TagBuilder rotateBlocklist = getOrCreateRawBuilder(MAID_VEHICLE_ROTATE_BLOCKLIST);
        rotateBlocklist.addOptionalElement(Identifier.parse("create:carriage_contraption"));
        rotateBlocklist.addOptionalElement(Identifier.parse("create:seat"));

        valueLookupBuilder(CARRYON_ENTITY_BLACKLIST)
                .add(InitEntities.TOMBSTONE)
                .add(InitEntities.SIT)
                .add(InitEntities.BROOM);

        // 让女仆免疫冰与火的石化效果，避免石化带来的各种问题
        valueLookupBuilder(IMMUNE_TO_GORGON_STONE).add(InitEntities.MAID);
    }
}
