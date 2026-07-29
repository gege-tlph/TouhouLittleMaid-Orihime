package com.github.tartaricacid.touhoulittlemaid.client.renderer.texture;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipPackTexture extends SizeTexture {
    private final Identifier texturePath;
    private final Path zipFilePath;
    private int width = 16;
    private int height = 16;

    public ZipPackTexture(String zipFilePath, Identifier texturePath) {
        super(texturePath);
        this.zipFilePath = Paths.get(zipFilePath);
        this.texturePath = texturePath;
    }

    @Override
    public boolean isExist() {
        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            ZipEntry entry = zipFile.getEntry(String.format("assets/%s/%s", texturePath.getNamespace(), texturePath.getPath()));
            return entry != null;
        } catch (IOException e) {
            TouhouLittleMaid.LOGGER.error("Failed to inspect zip texture {}", texturePath, e);
        }
        return false;
    }

    // 1.21.9+：load(ResourceManager) -> loadContents(ResourceManager) : TextureContents。
    // 线程调度与 GPU 上传由 ReloadableTexture 负责。
    @Override
    public TextureContents loadContents(ResourceManager manager) throws IOException {
        try (ZipFile zipFile = new ZipFile(zipFilePath.toFile())) {
            ZipEntry entry = zipFile.getEntry(String.format("assets/%s/%s", texturePath.getNamespace(), texturePath.getPath()));
            if (entry == null) {
                return TextureContents.createMissing();
            }
            try (InputStream stream = zipFile.getInputStream(entry)) {
                NativeImage imageIn = NativeImage.read(stream);
                width = imageIn.getWidth();
                height = imageIn.getHeight();
                return new TextureContents(imageIn, null);
            } catch (IOException e) {
                TouhouLittleMaid.LOGGER.error("Failed to load zip texture {}", texturePath, e);
            }
        } catch (IOException e) {
            TouhouLittleMaid.LOGGER.error("Failed to open zip texture {}", texturePath, e);
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
