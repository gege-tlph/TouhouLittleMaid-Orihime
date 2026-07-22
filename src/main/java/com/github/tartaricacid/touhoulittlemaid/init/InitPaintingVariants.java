package com.github.tartaricacid.touhoulittlemaid.init;

import com.github.tartaricacid.touhoulittlemaid.util.ResourceLocationUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.decoration.painting.PaintingVariant;

import java.util.Optional;

public class InitPaintingVariants {
    public static final Identifier WINE_FOX_ID = ResourceLocationUtil.getResourceLocation("wine_fox");
    public static final ResourceKey<PaintingVariant> WINE_FOX = ResourceKey.create(Registries.PAINTING_VARIANT, WINE_FOX_ID);

    public static void bootstrap(BootstrapContext<PaintingVariant> context) {
        context.register(WINE_FOX, new PaintingVariant(2, 3, WINE_FOX_ID, Optional.empty(), Optional.empty()));
    }
}
