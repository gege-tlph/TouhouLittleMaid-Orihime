package com.github.tartaricacid.touhoulittlemaid.compat.rei.altar;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.compat.rei.MaidREIClientPlugin;
import com.github.tartaricacid.touhoulittlemaid.network.client.ClientAltarRecipeCache;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class ReiAltarRecipeMaker {
    private ReiAltarRecipeMaker() {
    }

    public static void registerAltarRecipes(DisplayRegistry registry) {
        if (!ClientAltarRecipeCache.isReady()) {
            return;
        }

        Set<Identifier> existingIds = new HashSet<>();
        registry.get(MaidREIClientPlugin.ALTAR).stream()
                .map(display -> display.getDisplayLocation().orElse(null))
                .filter(java.util.Objects::nonNull)
                .forEach(existingIds::add);

        int added = 0;
        var recipes = ClientAltarRecipeCache.getRecipes();
        for (int index = 0; index < recipes.size(); index++) {
            var recipe = recipes.get(index);
            Identifier id = displayId(index, recipe.recipeString());
            if (!existingIds.add(id)) {
                continue;
            }
            var inputs = recipe.inputs().stream()
                    .map(items -> EntryIngredient.of(items.stream().map(EntryStacks::of).toList()))
                    .toList();
            var outputs = java.util.List.of(EntryIngredient.of(EntryStacks.of(recipe.output().copy())));
            registry.add(new ReiAltarRecipeDisplay(id, inputs, outputs,
                    recipe.powerCost(), recipe.langKey()));
            added++;
        }
        if (added > 0) {
            TouhouLittleMaid.LOGGER.info("Added {} synchronized altar recipes to REI ({} total)",
                    added, registry.get(MaidREIClientPlugin.ALTAR).size());
        }
    }

    private static Identifier displayId(int index, String recipeString) {
        String safeName = recipeString.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
        if (safeName.isBlank()) {
            safeName = "recipe";
        }
        return Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID,
                "rei/altar/" + index + "_" + safeName);
    }
}
