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

    // 1.21.9+：AbstractTexture 已无 getId()/load(ResourceManager)（改为持有 GpuTexture）。
    // 可重载纹理改为实现 loadContents -> TextureContents，由引擎负责线程调度与 GPU 上传，
    // 故原先的 RenderSystem.recordRenderCall / TextureUtil.prepareImage / NativeImage.upload 全部移除。
    // TextureContents 是 Closeable 并接管 image 的所有权，因此原「上传后置 null 释放内存」的语义保持不变。
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
