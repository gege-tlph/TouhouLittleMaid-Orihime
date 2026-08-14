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

    // 26.1.2 与 1.21.9+ 同形（javap 实查）：可重载纹理实现 loadContents -> TextureContents，
    // 由引擎负责线程调度与 GPU 上传。TextureContents 是 Closeable 并接管 image 的所有权，
    // 因此原「上传后置 null 释放内存」的语义保持不变。
    // ⚠️ 注册必须走 TextureManager.registerAndLoad（register 只入表不调 loadContents，纹理永不上传）。
    // 资源重载（F3+T）会再次调用 loadContents，此时 imageIn 已空、返回 missing——
    // 与之配套的自愈机制：包重载会重填缓存队列，下次打开模型 GUI 即重新生成图标。
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
