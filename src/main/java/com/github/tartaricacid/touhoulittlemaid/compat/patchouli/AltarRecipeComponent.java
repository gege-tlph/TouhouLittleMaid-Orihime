package com.github.tartaricacid.touhoulittlemaid.compat.patchouli;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.network.client.ClientAltarRecipeCache;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncAltarRecipesPackage.AltarRecipeSummary;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import vazkii.patchouli.api.IComponentProcessor;
import vazkii.patchouli.api.IVariable;
import vazkii.patchouli.api.IVariableProvider;

import java.util.List;

public final class AltarRecipeComponent implements IComponentProcessor {
    private static final String RECIPE_ID = "recipe_id";
    private static final String INPUT = "input";
    private static final String POWER_COST = "power_cost";
    private static final String OUTPUT_ITEM = "output_item";
    private static final String OUTPUT_ENTITY = "output_entity";
    private static final String OUTPUT_DESC = "output_desc";
    private static final String ALTAR_RECIPE_PATH = "altar_recipe/";

    private @Nullable AltarRecipeSummary recipe;

    @Override
    public void setup(Level level, IVariableProvider variables) {
        Identifier recipeId = Identifier.parse(variables.get(RECIPE_ID, level.registryAccess()).asString());
        String path = recipeId.getPath();
        String recipeString = path.startsWith(ALTAR_RECIPE_PATH)
                ? path.substring(ALTAR_RECIPE_PATH.length()) : path;
        recipe = ClientAltarRecipeCache.getRecipes().stream()
                .filter(summary -> summary.recipeString().equals(recipeString))
                .findFirst()
                .orElse(null);
        if (recipe == null) {
            TouhouLittleMaid.LOGGER.error("Altar recipe summary not found for Patchouli page: {}", recipeId);
        }
    }

    @Override
    public @Nullable IVariable process(Level level, String key) {
        if (recipe == null) {
            return emptyValue(level, key);
        }
        if (key.startsWith(INPUT)) {
            int index = Integer.parseInt(key.substring(INPUT.length())) - 1;
            if (index < 0 || index >= recipe.inputs().size()) {
                return IVariable.from(ItemStack.EMPTY, level.registryAccess());
            }
            List<String> itemIds = recipe.inputs().get(index).stream()
                    .map(ItemStack::getItem)
                    .map(BuiltInRegistries.ITEM::getKey)
                    .map(Identifier::toString)
                    .toList();
            return IVariable.wrap(StringUtils.join(itemIds, ","), level.registryAccess());
        }
        return switch (key) {
            case POWER_COST -> IVariable.wrap(String.format("x%.2f", recipe.powerCost()), level.registryAccess());
            case OUTPUT_ITEM -> isItemCraft(recipe)
                    ? IVariable.from(recipe.output(), level.registryAccess())
                    : IVariable.from(ItemStack.EMPTY, level.registryAccess());
            case OUTPUT_DESC -> IVariable.wrap(I18n.get(recipe.langKey()), level.registryAccess());
            case OUTPUT_ENTITY -> IVariable.wrap(displayEntityType(recipe.entityType()), level.registryAccess());
            default -> null;
        };
    }

    private static boolean isItemCraft(AltarRecipeSummary summary) {
        return "minecraft:item".equals(summary.entityType());
    }

    private static String displayEntityType(String entityType) {
        return "touhou_little_maid:box".equals(entityType) ? "touhou_little_maid:maid" : entityType;
    }

    private static @Nullable IVariable emptyValue(Level level, String key) {
        return switch (key) {
            case POWER_COST, OUTPUT_DESC -> IVariable.wrap("", level.registryAccess());
            case OUTPUT_ENTITY -> IVariable.wrap("minecraft:item", level.registryAccess());
            case OUTPUT_ITEM -> IVariable.from(ItemStack.EMPTY, level.registryAccess());
            default -> key.startsWith(INPUT) ? IVariable.from(ItemStack.EMPTY, level.registryAccess()) : null;
        };
    }
}
