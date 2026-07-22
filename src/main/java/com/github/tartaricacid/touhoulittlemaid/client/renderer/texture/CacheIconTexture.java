package com.github.tartaricacid.touhoulittlemaid.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;

public class CacheIconTexture extends ReloadableTexture {
    private final Identifier modelId;
    private NativeImage imageIn;

    public CacheIconTexture(Identifier modelId, NativeImage imageIn) {
        super(modelId);
        this.modelId = modelId;
        this.imageIn = imageIn;
    }

    @Override
    public TextureContents loadContents(ResourceManager manager) throws IOException {
        if (imageIn == null) {
            return TextureContents.createMissing();
        }
        NativeImage image = this.imageIn;
        this.imageIn = null;
        return new TextureContents(image, null);
    }

    public Identifier getModelId() {
        return modelId;
    }
}
