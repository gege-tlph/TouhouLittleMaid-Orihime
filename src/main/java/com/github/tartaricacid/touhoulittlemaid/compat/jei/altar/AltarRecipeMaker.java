package com.github.tartaricacid.touhoulittlemaid.compat.jei.altar;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.crafting.AltarRecipe;
import com.github.tartaricacid.touhoulittlemaid.init.InitRecipes;
import com.github.tartaricacid.touhoulittlemaid.network.client.ClientAltarRecipeCache;
import com.github.tartaricacid.touhoulittlemaid.util.JERIUtil;
import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import javax.annotation.Nullable;
import java.util.List;

public final class AltarRecipeMaker {
    @Nullable
    private final RecipeManager recipeManager;

    private AltarRecipeMaker() {
        // 1.21.11：客户端 Level 不再持有 RecipeManager（自定义 RecipeType 不随 vanilla 同步）。
        // 单人档从内置服务器读取；专用服务器下祭坛 JEI 列表暂缺，待专服 QA 轮补自定义同步。
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        this.recipeManager = server == null ? null : server.getRecipeManager();
    }

    public static AltarRecipeMaker getInstance() {
        return new AltarRecipeMaker();
    }

    @SuppressWarnings("unchecked")
    public List<AltarRecipeWrapper> getAltarRecipes() {
        if (recipeManager == null) {
            if (!ClientAltarRecipeCache.isReady()) {
                TouhouLittleMaid.LOGGER.warn("Altar JEI recipe summary has not arrived from the server yet");
                return List.of();
            }
            return ClientAltarRecipeCache.getRecipes().stream()
                    .map(recipe -> new AltarRecipeWrapper(recipe.inputs(), recipe.output().copy(),
                            recipe.powerCost(), recipe.langKey()))
                    .toList();
        }
        List<RecipeHolder<AltarRecipe>> altarRecipesMap = recipeManager.getRecipes().stream()
                .filter(holder -> holder.value().getType() == InitRecipes.ALTAR_CRAFTING)
                .map(holder -> (RecipeHolder<AltarRecipe>) holder)
                .toList();
        List<AltarRecipeWrapper> recipes = Lists.newArrayList();
        JERIUtil.recipeWarpHolder(altarRecipesMap, (recipeId, inputs, output, powerCost, langKey) -> {
            List<List<ItemStack>> inputs1 = inputs.stream()
                    .filter(ingredient -> !ingredient.isEmpty())
                    // 1.21.11：Ingredient.getItems() → items()（Stream<Holder<Item>>）
                    .map(ingredient -> ingredient.items().map(ItemStack::new).toList())
                    .toList();
            recipes.add(new AltarRecipeWrapper(inputs1, output, powerCost, langKey));
        });

        return recipes;
    }
}
