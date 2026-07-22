package cn.sh1rocu.touhoulittlemaid.api.extension;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.special.SpecialModelRenderer;

public interface IItemRenderer {
    @Environment(EnvType.CLIENT)
    SpecialModelRenderer<?> getCustomRenderer();
}
