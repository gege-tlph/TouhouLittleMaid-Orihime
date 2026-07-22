package com.github.tartaricacid.touhoulittlemaid.datagen.tag;

import com.github.tartaricacid.touhoulittlemaid.datagen.EnchantmentKeys;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.concurrent.CompletableFuture;

public class TagEnchantment extends FabricTagProvider<Enchantment> {
    public TagEnchantment(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> completableFuture) {
        super(output, Registries.ENCHANTMENT, completableFuture);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        builder(EnchantmentTags.NON_TREASURE).add(EnchantmentKeys.SPEEDY, EnchantmentKeys.IMPEDING);
        builder(EnchantmentTags.TREASURE).add(EnchantmentKeys.ENDERS_ENDER);
        builder(EnchantmentTags.TRADEABLE).add(EnchantmentKeys.ENDERS_ENDER);
    }
}
