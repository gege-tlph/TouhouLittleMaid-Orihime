package cn.sh1rocu.touhoulittlemaid.mixin.compat.tacz.client;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import mezz.jei.fabric.events.JeiLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * TaCZ R2 looks for the removed JEI 27-era AFTER_RECIPES_UPDATED field. On JEI 29,
 * invoking AFTER_RECIPE_SYNC restarts JEI's client lifecycle and re-registers plugins
 * against the freshly synchronized recipe data, so TaCZ need not reload every resource pack.
 */
@Mixin(targets = "com.tacz.guns.client.compat.RecipeViewerReloadBridge")
public abstract class RecipeViewerReloadBridgeMixin {
    @Inject(method = "refreshJei()Z", at = @At("HEAD"), cancellable = true)
    private static void tlm$refreshJei(CallbackInfoReturnable<Boolean> cir) {
        if (!FabricLoader.getInstance().isModLoaded("jei")) {
            return;
        }
        try {
            JeiLifecycleEvents.AFTER_RECIPE_SYNC.invoker().run();
            cir.setReturnValue(true);
        } catch (LinkageError | RuntimeException exception) {
            // Preserve TaCZ's original fallback if the optional JEI bridge is unavailable.
            TouhouLittleMaid.LOGGER.debug("TaCZ JEI lightweight refresh compatibility failed", exception);
        }
    }
}
