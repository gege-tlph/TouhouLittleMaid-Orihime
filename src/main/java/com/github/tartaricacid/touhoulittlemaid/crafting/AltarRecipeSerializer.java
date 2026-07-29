package com.github.tartaricacid.touhoulittlemaid.crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AltarRecipeSerializer implements RecipeSerializer<AltarRecipe> {
    public static final MapCodec<AltarRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Codec.STRING.optionalFieldOf("group", StringUtils.EMPTY).forGetter(AltarRecipe::getGroup),
                    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(AltarRecipe::getCategory),
                    Ingredient.CODEC.listOf().fieldOf("ingredients").flatXmap(AltarRecipeSerializer::checkIngredients, DataResult::success).forGetter(AltarRecipe::getIngredients),
                    Codec.FLOAT.fieldOf("power").forGetter(AltarRecipe::getPower),
                    ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AltarRecipe::getResult),
                    Identifier.CODEC.fieldOf("entity").forGetter(AltarRecipe::getEntityType),
                    Codec.STRING.optionalFieldOf("lang", StringUtils.EMPTY).forGetter(AltarRecipe::getLangKey)
            ).apply(instance, AltarRecipe::new)
    );

    @NotNull
    private static DataResult<NonNullList<Ingredient>> checkIngredients(List<Ingredient> ingredientList) {
        if (ingredientList.isEmpty()) {
            return DataResult.error(() -> "No ingredients for shapeless recipe");
        }
        if (ingredientList.size() > 6) {
            return DataResult.error(() -> "Too many ingredients for shapeless recipe. The maximum is: 6");
        }
        // 1.21.11: Ingredient.EMPTY 移除 → createWithCapacity + addAll
        NonNullList<Ingredient> nonNullList = NonNullList.createWithCapacity(ingredientList.size());
        nonNullList.addAll(ingredientList);
        return DataResult.success(nonNullList);
    }

    private AltarRecipe fromNetwork(RegistryFriendlyByteBuf byteBuf) {
        String group = byteBuf.readUtf();
        CraftingBookCategory category = byteBuf.readEnum(CraftingBookCategory.class);
        // 1.21.11: Ingredient.EMPTY 移除 → createWithCapacity + 逐个 decode
        int size = byteBuf.readVarInt();
        NonNullList<Ingredient> ingredients = NonNullList.createWithCapacity(size);
        for (int i = 0; i < size; i++) {
            ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(byteBuf));
        }
        float power = byteBuf.readFloat();
        ItemStack result = ItemStack.STREAM_CODEC.decode(byteBuf);
        // 1.21.11: readResourceLocation 移除 → Identifier.STREAM_CODEC
        Identifier entityType = Identifier.STREAM_CODEC.decode(byteBuf);
        String langKey = byteBuf.readUtf();
        return new AltarRecipe(group, category, ingredients, power, result, entityType, langKey);
    }

    private void toNetwork(RegistryFriendlyByteBuf friendlyByteBuf, AltarRecipe altarRecipe) {
        friendlyByteBuf.writeUtf(altarRecipe.getGroup());
        friendlyByteBuf.writeEnum(altarRecipe.getCategory());
        friendlyByteBuf.writeVarInt(altarRecipe.getIngredients().size());
        for (Ingredient ingredient : altarRecipe.getIngredients()) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(friendlyByteBuf, ingredient);
        }
        friendlyByteBuf.writeFloat(altarRecipe.getPower());
        ItemStack.STREAM_CODEC.encode(friendlyByteBuf, altarRecipe.getResult());
        // 1.21.11: writeResourceLocation 移除 → Identifier.STREAM_CODEC
        Identifier.STREAM_CODEC.encode(friendlyByteBuf, altarRecipe.getEntityType());
        friendlyByteBuf.writeUtf(altarRecipe.getLangKey());
    }

    @Override
    public MapCodec<AltarRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, AltarRecipe> streamCodec() {
        return StreamCodec.of(this::toNetwork, this::fromNetwork);
    }
}
