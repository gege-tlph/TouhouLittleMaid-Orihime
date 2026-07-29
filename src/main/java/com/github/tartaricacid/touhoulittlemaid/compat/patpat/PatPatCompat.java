package com.github.tartaricacid.touhoulittlemaid.compat.patpat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

public class PatPatCompat {
    private static final String PATPAT_ID = "patpat";
    private static boolean isLoaded = false;

    public static void init() {
        isLoaded = FabricLoader.getInstance().isModLoaded(PATPAT_ID);
    }

    /**
     * PatPat injects into vanilla LivingEntityRenderer itself. TLM only calls
     * this bridge for renderers that bypass that vanilla path (our vendored
     * Gecko renderer), matching the origin/1.21.1 behavior without double scale.
     */
    public static void renderPat(@Nullable LivingEntity livingEntity, PoseStack matrixStack, float tickDelta) {
        if (isLoaded && livingEntity != null) {
            PatPatRenderer.scaleEntityIfPatted(livingEntity, matrixStack, tickDelta);
        }
    }
}
