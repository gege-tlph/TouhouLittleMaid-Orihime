package cn.sh1rocu.touhoulittlemaid.mixin.accessor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(EnvType.CLIENT)
@Mixin(SingleQuadParticle.class)
public interface TextureSheetParticleAccessor {
    @Invoker("setSprite")
    void tlm$setSprite(TextureAtlasSprite sprite);
}
