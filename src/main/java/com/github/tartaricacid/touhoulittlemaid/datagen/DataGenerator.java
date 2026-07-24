package com.github.tartaricacid.touhoulittlemaid.datagen;

import com.github.tartaricacid.touhoulittlemaid.datagen.tag.*;
import com.github.tartaricacid.touhoulittlemaid.init.InitDamage;
import com.github.tartaricacid.touhoulittlemaid.init.InitPaintingVariants;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;

public class DataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        // 型号
        pack.addProvider(ItemModelGenerator::new);

        // 进步
        pack.addProvider(AdvancementDataGen::new);

        // 战利品表
        pack.addProvider(LootTableGenerator.ChestLootTables::new);
        pack.addProvider(LootTableGenerator.AdvancementLootTables::new);
        pack.addProvider(LootTableGenerator.EntityLootTables::new);
        pack.addProvider(LootTableGenerator.BlockLootTables::new);


        // 食谱
        pack.addProvider(RecipeGenerator::new);

        // 标签
        pack.addProvider(TagDamage::new);
        pack.addProvider(TagEntity::new);
        pack.addProvider(TagBlock::new);
        pack.addProvider(TagEnchantment::new);
        pack.addProvider(TagItem::new);
        pack.addProvider(TagRecipeSerializer::new);
        pack.addProvider(TagPaintingVariant::new);

        // 基于注册表的东西
        pack.addProvider(RegistryDataGenerator::new);
    }

    @Override
    public void buildRegistry(RegistrySetBuilder registryBuilder) {
        registryBuilder.add(Registries.ENCHANTMENT, EnchantmentKeys::bootstrap);
        registryBuilder.add(Registries.DAMAGE_TYPE, InitDamage::bootstrap);
        registryBuilder.add(Registries.PAINTING_VARIANT, InitPaintingVariants::bootstrap);
    }
}
