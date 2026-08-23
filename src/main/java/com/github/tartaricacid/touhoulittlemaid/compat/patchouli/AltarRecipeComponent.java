package com.github.tartaricacid.touhoulittlemaid.compat.patchouli;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.event.ClientRecipeEvent;
import com.github.tartaricacid.touhoulittlemaid.crafting.AltarRecipe;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class AltarRecipeComponent implements IComponentProcessor {
    private static final String RECIPE_ID = "recipe_id";
    private static final String INPUT = "input";
    private static final String POWER_COST = "power_cost";
    private static final String OUTPUT_ITEM = "output_item";
    private static final String OUTPUT_ENTITY = "output_entity";
    private static final String OUTPUT_DESC = "output_desc";

    private @Nullable AltarRecipe recipe;

    @Override
    public void setup(Level level, IVariableProvider variables) {
        Identifier recipeId = Identifier.parse(variables.get(RECIPE_ID, level.registryAccess()).asString());
        recipe = ClientRecipeEvent.ALTAR_RECIPES.stream()
                .filter(holder -> holder.id().identifier().equals(recipeId))
                .map(RecipeHolder::value)
                .findFirst()
                .orElse(null);
        if (recipe == null) {
            TouhouLittleMaid.LOGGER.error("Altar recipe not found for Patchouli page: {}", recipeId);
        }
    }

    @Override
    public @Nullable IVariable process(Level level, String key) {
        if (recipe == null) {
            return emptyValue(level, key);
        }
        if (key.startsWith(INPUT)) {
            int index;
            try {
                index = Integer.parseInt(key.substring(INPUT.length())) - 1;
            } catch (NumberFormatException ignored) {
                return IVariable.from(ItemStack.EMPTY, level.registryAccess());
            }
            List<Ingredient> ingredients = recipe.getIngredients();
            if (index < 0 || index >= ingredients.size()) {
                return IVariable.from(ItemStack.EMPTY, level.registryAccess());
            }
            String itemIds = ingredients.get(index).items()
                    .map(holder -> BuiltInRegistries.ITEM.getKey(holder.value()))
                    .filter(Objects::nonNull)
                    .map(Identifier::toString)
                    .collect(Collectors.joining(","));
            return IVariable.wrap(itemIds, level.registryAccess());
        }
        return switch (key) {
            case POWER_COST -> IVariable.wrap(String.format("x%.2f", recipe.getPower()), level.registryAccess());
            case OUTPUT_ITEM -> isItemCraft(recipe)
                    ? IVariable.from(recipe.getResult().create(), level.registryAccess())
                    : IVariable.from(ItemStack.EMPTY, level.registryAccess());
            case OUTPUT_DESC -> IVariable.wrap(I18n.get(recipe.getLangKey()), level.registryAccess());
            case OUTPUT_ENTITY -> IVariable.wrap(displayEntityType(recipe.getEntityType()), level.registryAccess());
            default -> null;
        };
    }

    private static boolean isItemCraft(AltarRecipe recipe) {
        return "minecraft:item".equals(recipe.getEntityType().toString());
    }

    private static String displayEntityType(Identifier entityType) {
        return "touhou_little_maid:box".equals(entityType.toString())
                ? "touhou_little_maid:maid" : entityType.toString();
    }

    private static @Nullable IVariable emptyValue(Level level, String key) {
        return switch (key) {
            case POWER_COST, OUTPUT_DESC -> IVariable.wrap("", level.registryAccess());
            case OUTPUT_ENTITY -> IVariable.wrap("minecraft:item", level.registryAccess());
            case OUTPUT_ITEM -> IVariable.from(ItemStack.EMPTY, level.registryAccess());
            default -> key.startsWith(INPUT)
                    ? IVariable.from(ItemStack.EMPTY, level.registryAccess()) : null;
        };
    }
}
