package com.github.tartaricacid.touhoulittlemaid.util;

import com.github.tartaricacid.touhoulittlemaid.crafting.AltarRecipe;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.item.ItemEntityPlaceholder;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

public final class JERIUtil {
    public static void recipeWarpHolder(List<RecipeHolder<AltarRecipe>> altarRecipes, AltarRecipeMaker maker) {
        for (RecipeHolder<AltarRecipe> altarRecipeHolder : altarRecipes) {
            AltarRecipe altarRecipe = altarRecipeHolder.value();
            recipeMaker(maker, altarRecipe);
        }
    }

    public static void recipeWarp(List<AltarRecipe> altarRecipes, AltarRecipeMaker maker) {
        for (AltarRecipe altarRecipeHolder : altarRecipes) {
            recipeMaker(maker, altarRecipeHolder);
        }
    }

    private static void recipeMaker(AltarRecipeMaker maker, AltarRecipe altarRecipe) {
        // 1.21.11：ClientLevel 已无 RecipeManager（origin 此处的枚举调用本就是无消费的死代码，删）；
        // getResultItem(registryAccess) → 仓库 AltarRecipe 适配后的 getResult()
        Identifier recipeId = altarRecipe.getId();
        ItemStack output = altarRecipe.getResult();
        if (!altarRecipe.isItemCraft()) {
            output = InitItems.ENTITY_PLACEHOLDER.getDefaultInstance();
            ItemEntityPlaceholder.setRecipeId(output, altarRecipe.getRecipeString());
        }
        maker.accept(recipeId, altarRecipe.getIngredients(), output, altarRecipe.getPower(), altarRecipe.getLangKey());
    }

    public interface AltarRecipeMaker {
        void accept(Identifier recipeId, NonNullList<Ingredient> inputs, ItemStack output, float powerCost, String langKey);
    }
}