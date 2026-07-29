package com.github.tartaricacid.touhoulittlemaid.client.renderer.texture;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class FilePackTexture extends SizeTexture {
    private final Identifier texturePath;
    private final Path rootPath;
    private int width = 16;
    private int height = 16;

    public FilePackTexture(Path rootPath, Identifier texturePath) {
        super(texturePath);
        this.rootPath = rootPath;
        this.texturePath = texturePath;
    }

    @Override
    public boolean isExist() {
        File textureFile = rootPath.resolve("assets").resolve(texturePath.getNamespace()).resolve(texturePath.getPath()).toFile();
        return textureFile.isFile();
    }

    // 1.21.9+：load(ResourceManager) -> loadContents(ResourceManager) : TextureContents。
    // 线程调度与 GPU 上传均由 ReloadableTexture 负责，故 RenderSystem.isOnRenderThreadOrInit /
    // recordRenderCall / TextureUtil.prepareImage / NativeImage.upload 全部移除。
    @Override
    public TextureContents loadContents(ResourceManager manager) throws IOException {
        File textureFile = rootPath.resolve("assets").resolve(texturePath.getNamespace()).resolve(texturePath.getPath()).toFile();
        if (textureFile.isFile()) {
            try (InputStream stream = Files.newInputStream(textureFile.toPath())) {
                NativeImage imageIn = NativeImage.read(stream);
                width = imageIn.getWidth();
                height = imageIn.getHeight();
                return new TextureContents(imageIn, null);
            } catch (IOException e) {
                TouhouLittleMaid.LOGGER.error("Failed to load file texture {}", texturePath, e);
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
