package com.github.tartaricacid.touhoulittlemaid.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

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
     * 纯绿色 0xFF00FF00 在 RGBA/ABGR/ARGB 各序下数值相同；26.1.2 的 takeScreenshot 回读时
     * 对每个像素做 {@code | 0xFF000000}（其 setPixelABGR 调用处实查），绿幕像素不受影响，
     * 此常量与 BACKGROUND_COLOR 语义保持 origin 不变
     */
    public static final int BACKGROUND_COLOR_SHIFTED = 0xFF_00_FF_00;

    /**
     * 26.1.2：Screenshot.takeScreenshot(RenderTarget, Consumer&lt;NativeImage&gt;) 与 1.21.11 同形
     * （编码一条 GPU copyTextureToBuffer 命令 + fence，回调经 RenderSystem.executePendingTasks
     * 在 fence 就绪后于渲染线程执行——Minecraft.runTick 反编译实查）。
     * 裁剪与绿幕转透明逻辑与 origin 一致；NativeImage.pixels 字段在 26.1.2 已私有化，
     * 故裁剪由 MemoryUtil.memCopy 改为公开的 getPixel/setPixel 逐像素拷贝并顺带完成绿幕判定
     * （256×256 一次性成本，可忽略）
     */
    public static void exportImageFromScreenshot(int scaleImage, int backgroundColor, Consumer<NativeImage> callback) {
        // 尝试全屏截图
        Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget(), imageFull -> {
            // 从全屏截图中裁出左上角我们需要的那部分，同时把背景颜色转换为透明像素。
            // useCalloc=true 让越界兜底区域保持全透明（窗口物理尺寸小于 scaleImage 时不越界读取）
            NativeImage image = new NativeImage(scaleImage, scaleImage, true);
            int copyWidth = Math.min(scaleImage, imageFull.getWidth());
            int copyHeight = Math.min(scaleImage, imageFull.getHeight());
            for (int cy = 0; cy < copyHeight; cy++) {
                for (int cx = 0; cx < copyWidth; cx++) {
                    int color = imageFull.getPixel(cx, cy);
                    // 如果颜色等于背景色，那么直接设置为透明像素
                    image.setPixel(cx, cy, color == backgroundColor ? 0x00_00_00_00 : color);
                }
            }
            // 关闭全屏截图的缓存
            imageFull.close();
            callback.accept(image);
        });
    }
}
