package com.github.tartaricacid.touhoulittlemaid.compat.rei.altar;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.event.ClientRecipeEvent;
import com.github.tartaricacid.touhoulittlemaid.compat.rei.MaidREIClientPlugin;
import com.github.tartaricacid.touhoulittlemaid.crafting.AltarRecipe;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 把祭坛配方灌进 REI。
 *
 * <p><b>与行为基准的有意分歧（重写非搬运）</b>：基准取配方走我们自己的
 * {@code ClientAltarRecipeCache}（审计 §3.K 的客户端同步，本分支未搬），
 * 宿主已有现成的 {@link ClientRecipeEvent#ALTAR_RECIPES}——配方同步事件到达时整表可用，
 * 且带真实的配方 id（基准缓存没有 id，只能用「序号 + 配方串清洗」拼显示 id；
 * 这里直接用 {@code RecipeHolder.id()}，天然稳定唯一）。宿主自己的 JEI 插件
 * {@code MaidJeiPlugin} 消费同一张表，是本改写的参照物。</p>
 *
 * <p>同步事件晚于插件初始化时表为空：直接返回即可——REI 在配方同步后会重载插件，
 * 届时再注册一遍（与 JEI 插件的兜底思路一致，但 REI 不需要主动拉取）。</p>
 */
public final class ReiAltarRecipeMaker {
    private ReiAltarRecipeMaker() {
    }

    public static void registerAltarRecipes(DisplayRegistry registry) {
        List<RecipeHolder<AltarRecipe>> recipes = ClientRecipeEvent.ALTAR_RECIPES;
        if (recipes.isEmpty()) {
            TouhouLittleMaid.LOGGER.info("Altar recipes not synchronized yet; REI displays register on plugin reload");
            return;
        }

        Set<Identifier> existingIds = new HashSet<>();
        registry.get(MaidREIClientPlugin.ALTAR).stream()
                .map(display -> display.getDisplayLocation().orElse(null))
                .filter(java.util.Objects::nonNull)
                .forEach(existingIds::add);

        int added = 0;
        for (RecipeHolder<AltarRecipe> holder : recipes) {
            Identifier recipeId = holder.id().identifier();
            Identifier displayId = Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID,
                    "rei/altar/" + recipeId.getNamespace() + "/" + recipeId.getPath());
            if (!existingIds.add(displayId)) {
                continue;
            }
            AltarRecipe recipe = holder.value();
            List<EntryIngredient> inputs = EntryIngredients.ofIngredients(recipe.getIngredients());
            List<EntryIngredient> outputs = List.of(EntryIngredients.of(recipe.getResult().create()));
            registry.add(new ReiAltarRecipeDisplay(displayId, inputs, outputs,
                    recipe.getPower(), recipe.getLangKey()));
            added++;
        }
        if (added > 0) {
            TouhouLittleMaid.LOGGER.info("Added {} synchronized altar recipes to REI ({} total)",
                    added, registry.get(MaidREIClientPlugin.ALTAR).size());
        }
    }
}
