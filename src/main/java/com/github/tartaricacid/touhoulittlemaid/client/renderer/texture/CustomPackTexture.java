package com.github.tartaricacid.touhoulittlemaid.client.renderer.texture;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.client.resource.accessor.ResourceAccessor;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * 通过 {@link ResourceAccessor} 读取自定义模型包中的纹理，支持目录与压缩包等不同资源来源。
 */
public class CustomPackTexture extends SizeTexture {
    private final ResourceAccessor accessor;
    private final Identifier texturePath;
    private int width = 16;
    private int height = 16;

    public CustomPackTexture(ResourceAccessor accessor, Identifier texturePath) {
        super(texturePath);
        this.accessor = accessor;
        this.texturePath = texturePath;
    }

    private String assetPath() {
        return "assets/%s/%s".formatted(texturePath.getNamespace(), texturePath.getPath());
    }

    @Override
    public boolean isExist() {
        return accessor.exists(assetPath());
    }

    @Override
    public TextureContents loadContents(ResourceManager manager) throws IOException {
        String path = assetPath();
        if (accessor.exists(path)) {
            try (InputStream stream = accessor.open(path)) {
                NativeImage imageIn = NativeImage.read(stream);
                width = imageIn.getWidth();
                height = imageIn.getHeight();
                return new TextureContents(imageIn, null);
            } catch (IOException e) {
                TouhouLittleMaid.LOGGER.error("Failed to load custom pack texture {}", texturePath, e);
            }
        }
        return TextureContents.createMissing();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
