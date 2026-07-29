package com.github.tartaricacid.touhoulittlemaid.datagen.tag;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

import static com.github.tartaricacid.touhoulittlemaid.init.InitRecipes.ALTAR_RECIPE_SERIALIZER;

public class TagRecipeSerializer extends FabricTagProvider<RecipeSerializer<?>> {
    public static final TagKey<RecipeSerializer<?>> AUTOMATION_IGNORE = TagKey.create(Registries.RECIPE_SERIALIZER, Identifier.fromNamespaceAndPath("create", "automation_ignore"));

    public TagRecipeSerializer(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.RECIPE_SERIALIZER, lookupProvider);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        // 1.21.11: getOrCreateTagBuilder/reverseLookup 已移除 —— 用 getOrCreateRawBuilder(TagKey) + 序列化器注册 id
        //   作为可选元素写入（optional，兼容未安装 create 时的容错，与旧 addOptional(reverseLookup(..)) 等价）。
        Identifier serializerId = BuiltInRegistries.RECIPE_SERIALIZER.getKey(ALTAR_RECIPE_SERIALIZER);
        getOrCreateRawBuilder(AUTOMATION_IGNORE).addOptionalElement(serializerId);
    }
}
