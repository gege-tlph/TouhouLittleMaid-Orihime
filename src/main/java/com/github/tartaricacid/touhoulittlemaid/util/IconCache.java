package com.github.tartaricacid.touhoulittlemaid.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.lwjgl.system.MemoryUtil;

import java.util.function.Consumer;

/**
 * Refer: https://github.com/CyclopsMC/IconExporter/
 * MIT license: https://github.com/CyclopsMC/IconExporter/blob/master-1.21/LICENSE.txt
 */
public final class IconCache {
    /**
     * ARGB
     */
    public static final int BACKGROUND_COLOR = 0xFF_00_FF_00;
    /**
     * 不知何故，Minecraft 的顺序是 ABGR
     * <p>
     * 1.21.11：NativeImage 公开像素接口改为 ARGB 序（getPixel/setPixel）；纯绿色 0xFF00FF00 在
     * RGBA/ABGR/ARGB 各序下数值相同，此常量与 BACKGROUND_COLOR 语义保持 origin 不变
     */
    public static final int BACKGROUND_COLOR_SHIFTED = 0xFF_00_FF_00;

    /**
     * 1.21.11：Screenshot.takeScreenshot(RenderTarget) 同步返回版已移除，改为
     * takeScreenshot(RenderTarget, Consumer&lt;NativeImage&gt;)（GPU 异步回读），
     * 故本方法由同步返回改为回调风格；裁剪与绿幕转透明逻辑与 origin 完全一致
     */
    public static void exportImageFromScreenshot(int scaleImage, int backgroundColor, Consumer<NativeImage> callback) {
        // 尝试全屏截图
        Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget(), imageFull -> {
            // 从全屏截图中获取我们需要的那部分
            NativeImage image = getSubImage(imageFull, scaleImage, scaleImage);
            // 关闭全屏截图的缓存
            imageFull.close();

            // 将背景颜色转换为透明像素
            for (int cx = 0; cx < image.getWidth(); cx++) {
                for (int cy = 0; cy < image.getHeight(); cy++) {
                    // 1.21.11: getPixelRGBA/setPixelRGBA（ABGR 序）已私有化，公开替代 getPixel/setPixel（ARGB 序）；
                    // 背景绿色在两序下同值，判定与 origin 等价
                    int color = image.getPixel(cx, cy);
                    // 如果颜色等于背景色，那么直接设置为透明像素
                    if (color == backgroundColor) {
                        image.setPixel(cx, cy, 0x00_00_00_00);
                    }
                }
            }

            callback.accept(image);
        });
    }

    private static NativeImage getSubImage(NativeImage image, int width, int height) {
        NativeImage imageSub = new NativeImage(width, height, false);
        // 只截取其中一部分贴图
        for (int y = 0; y < imageSub.getHeight(); y++) {
            int pointerOffset = y * image.getWidth() * image.format().components();
            int pointerOffsetSub = y * imageSub.getWidth() * imageSub.format().components();
            MemoryUtil.memCopy(image.pixels + (long) pointerOffset, imageSub.pixels + (long) pointerOffsetSub, (long) imageSub.getWidth() * image.format().components());
        }
        return imageSub;
    }
}
