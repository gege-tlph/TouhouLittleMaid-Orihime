package com.github.tartaricacid.touhoulittlemaid.client.renderer.texture;

import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.resources.Identifier;

/**
 * 1.21.9+ 纹理重写：{@code AbstractTexture} 已无 {@code getId()} / {@code load(ResourceManager)}，
 * 改为持有 {@code GpuTexture}。可重载纹理须继承 {@link ReloadableTexture} 并实现
 * {@code loadContents(ResourceManager) -> TextureContents}，由引擎负责 GPU 上传——
 * 子类不再手动调用 TextureUtil.prepareImage / NativeImage.upload / RenderSystem.recordRenderCall。
 */
public abstract class SizeTexture extends ReloadableTexture {
    public SizeTexture(Identifier resourceId) {
        super(resourceId);
    }

    /**
     * 获取材质宽度
     *
     * @return 宽度（像素）
     */
    abstract public int getWidth();

    /**
     * 获取材质高度
     *
     * @return 高度（像素）
     */
    abstract public int getHeight();

    /**
     * 给定路径的文件是否存在
     *
     * @return 存在与否
     */
    abstract public boolean isExist();
}
