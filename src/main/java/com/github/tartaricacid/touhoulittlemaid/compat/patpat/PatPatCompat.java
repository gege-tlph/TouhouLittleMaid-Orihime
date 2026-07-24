package com.github.tartaricacid.touhoulittlemaid.compat.patpat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/**
 * PatPat 渲染桥。仅补偿绕过原版 LivingEntityRenderer 的 Gecko 渲染路径，避免重复缩放。
 */
public class PatPatCompat {
    private static final String PATPAT_ID = "patpat";
    private static boolean isLoaded = false;

    public static void init() {
        isLoaded = FabricLoader.getInstance().isModLoaded(PATPAT_ID);
    }


    public static void renderPat(@Nullable LivingEntity livingEntity, PoseStack matrixStack, float tickDelta) {
        if (isLoaded && livingEntity != null) {
            PatPatRenderer.scaleEntityIfPatted(livingEntity, matrixStack, tickDelta);
        }
    }
}
