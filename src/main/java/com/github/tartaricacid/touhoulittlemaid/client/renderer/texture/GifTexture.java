package com.github.tartaricacid.touhoulittlemaid.client.renderer.texture;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.client.decoder.GifDecoder;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.renderer.texture.TickableTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * 1.21.9+ 迁移说明：
 * <ul>
 *   <li>{@code Tickable} 并非被移除，而是**更名为 {@link TickableTexture}**
 *       （{@code TextureManager.tick()} 仍遍历 {@code Set<TickableTexture>} 逐个 tick）。
 *       此前的 TODO 误判为「接口不存在」并注释掉了 implements，导致 {@code tick()} 不再覆盖任何东西、
 *       永不被调用 —— GIF 动画因此已经静默失效。现已恢复。</li>
 *   <li>加载改为 {@code loadContents -> TextureContents}，首帧由引擎上传。</li>
 *   <li>逐帧切换改用 {@code RenderSystem.getDevice().createCommandEncoder().writeToTexture(...)}
 *       （旧的 {@code TextureUtil.prepareImage(getId(),…)} + {@code NativeImage.upload(…)} 已随
 *       {@code AbstractTexture.getId()} 一同移除）。</li>
 *   <li>{@code setPixelRGBA} → {@code setPixelABGR}：旧名是误称，其入参本就是 ABGR 打包值，
 *       与本类 {@code (a<<24)|(b<<16)|(g<<8)|r} 的打包顺序一致，故为精确对应。</li>
 * </ul>
 */
public class GifTexture extends SizeTexture implements TickableTexture {
    private final Identifier texturePath;
    private NativeImage[] frames;
    private int[] frameDelays;
    private int currentFrame = 0;
    private int currentFrameDelay = 0;
    private int width = 16;
    private int height = 16;

    public GifTexture(Identifier texturePath) {
        super(texturePath);
        this.texturePath = texturePath;
    }

    @Override
    public TextureContents loadContents(ResourceManager manager) throws IOException {
        try (InputStream stream = manager.open(this.texturePath)) {
            GifDecoder decoder = new GifDecoder();
            decoder.read(stream);

            int totalFrames = decoder.getFrameCount();
            Dimension frameSize = decoder.getFrameSize();
            this.frames = new NativeImage[totalFrames];
            this.frameDelays = new int[totalFrames];
            this.width = frameSize.width;
            this.height = frameSize.height;

            // 让图片学习原版序列帧竖向排列
            for (int i = 0; i < totalFrames; i++) {
                NativeImage nativeImage = new NativeImage(this.width, this.height, true);
                BufferedImage image = decoder.getFrame(i);
                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int argb = image.getRGB(x, y);
                        int a = (argb >> 24) & 0xFF;
                        int r = (argb >> 16) & 0xFF;
                        int g = (argb >> 8) & 0xFF;
                        int b = argb & 0xFF;

                        nativeImage.setPixelABGR(x, y, (a << 24) | (b << 16) | (g << 8) | r);
                    }
                }
                this.frames[i] = nativeImage;
                this.frameDelays[i] = Math.max(decoder.getDelay(i) / 50, 1);
            }

            if (totalFrames > 0) {
                // 首帧交给引擎上传。frames[] 由本类持有用于后续 tick 切换，
                // 故此处传入副本，避免 TextureContents（Closeable）关闭我们仍需复用的帧。
                return new TextureContents(copyOf(this.frames[0]), null);
            }
        } catch (Exception e) {
            TouhouLittleMaid.LOGGER.error("Failed to load gif texture: {}", this.texturePath, e);
        }
        return TextureContents.createMissing();
    }

    private static NativeImage copyOf(NativeImage src) {
        NativeImage dst = new NativeImage(src.getWidth(), src.getHeight(), true);
        dst.copyFrom(src);
        return dst;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public boolean isExist() {
        return true;
    }

    @Override
    public void tick() {
        if (frames == null || frames.length == 0 || this.texture == null) {
            return;
        }
        currentFrameDelay++;
        if (currentFrameDelay >= frameDelays[currentFrame]) {
            currentFrameDelay = 0;
            currentFrame = (currentFrame + 1) % frames.length;
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToTexture(this.texture, frames[currentFrame]);
        }
    }
}
