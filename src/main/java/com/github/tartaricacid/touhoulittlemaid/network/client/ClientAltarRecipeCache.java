package com.github.tartaricacid.touhoulittlemaid.network.client;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.compat.jei.MaidPlugin;
import com.github.tartaricacid.touhoulittlemaid.init.InitCreativeTabs;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncAltarRecipesPackage.AltarRecipeSummary;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.CreativeModeTab;
import net.fabricmc.loader.api.FabricLoader;

import java.util.List;

@Environment(EnvType.CLIENT)
public final class ClientAltarRecipeCache {
    private static List<AltarRecipeSummary> recipes = List.of();
    private static boolean ready;

    private ClientAltarRecipeCache() {
    }

    public static void replace(List<AltarRecipeSummary> syncedRecipes) {
        recipes = syncedRecipes.stream().map(AltarRecipeSummary::copy).toList();
        ready = true;
        TouhouLittleMaid.LOGGER.info("Received {} synchronized altar recipe summaries", recipes.size());

        if (FabricLoader.getInstance().isModLoaded("jei")) {
            MaidPlugin.onSyncedAltarRecipesAvailable();
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null) {
            // The join packet can arrive while other mods' dynamic registry-backed item stacks are still resolving.
            // A global CreativeModeTabs rebuild is not transactional: if any foreign tab throws, vanilla has already
            // cached the parameters while Fabric has not yet assigned pages, so opening the inventory later crashes.
            // Only this tab depends on the synchronized altar summaries, so rebuild it without touching global state.
            InitCreativeTabs.MAIN_TAB.buildContents(new CreativeModeTab.ItemDisplayParameters(
                    minecraft.level.enabledFeatures(),
                    minecraft.player.canUseGameMasterBlocks(),
                    minecraft.level.registryAccess()));
            long placeholders = InitCreativeTabs.MAIN_TAB.getDisplayItems().stream()
                    .filter(stack -> stack.is(InitItems.ENTITY_PLACEHOLDER))
                    .count();
            TouhouLittleMaid.LOGGER.info("Creative tab rebuilt with {} altar entity placeholders", placeholders);
        }
    }

    public static List<AltarRecipeSummary> getRecipes() {
        return recipes;
    }

    public static boolean isReady() {
        return ready;
    }

    public static void clear() {
        recipes = List.of();
        ready = false;
    }
}
