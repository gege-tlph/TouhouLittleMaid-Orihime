package com.github.tartaricacid.touhoulittlemaid.util;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

import java.util.function.BiConsumer;

/**
 * Refer: https://github.com/CyclopsMC/IconExporter/
 * MIT license: https://github.com/CyclopsMC/IconExporter/blob/master-1.21/LICENSE.txt
 * <p>
 * ⚠️ 与 origin 的单绿幕等值抠像不同，本实现是<b>双背景差分抠像</b>（2026-08-15 用户批准的超基准改进）：
 * origin 只抠掉与纯绿逐位相等的像素，凡贴图带半透明像素的部件（琪露诺冰翅 α=128 等）
 * 与绿幕混合后逃过等值判定，图标上整片变绿——origin 固有伪影，实机截图证实。
 * 改为同一帧里并排绘制绿幕与品红幕两块、同一模型渲染两次（同帧 = 同动画时间戳），逐像素反解：
 * {@code P₁ = F·α + B₁·(1-α)}、{@code P₂ = F·α + B₂·(1-α)} →
 * {@code (1-α) = (P₂-P₁)/(B₂-B₁)}（三通道求均值抗读回舍入噪声）、{@code F = (P₁ - B₁·(1-α))/α}。
 * 不透明像素两张全同 → 原样保留（模型自身的绿色部件零误杀）；纯背景差满幅 → 全透明；
 * 半透明部件恢复出真实的 α 与前景色。
 */
public final class IconCache {
    /**
     * ARGB 纯绿：第一背景色。与品红逐通道互补，差分幅值恒为 255
     */
    public static final int BACKGROUND_GREEN = 0xFF_00_FF_00;
    /**
     * ARGB 纯品红：第二背景色
     */
    public static final int BACKGROUND_MAGENTA = 0xFF_FF_00_FF;

    /**
     * α 低于此值视为全透明（读回舍入噪声容差）
     */
    private static final float ALPHA_EPSILON = 2f / 255f;

    /**
     * 26.1.2：Screenshot.takeScreenshot(RenderTarget, Consumer&lt;NativeImage&gt;) 编码一条 GPU
     * copyTextureToBuffer 命令 + fence，回调经 RenderSystem.executePendingTasks 在 fence 就绪后
     * 于渲染线程执行（Minecraft.runTick 反编译实查）。
     * <p>
     * 一次截图裁出<b>同一帧里并排绘制的绿幕/品红幕两块</b>——两块出自同一动画时间戳，
     * 姿态逐位相同，差分不会把动画位移误读成透明度（gecko 模型实测教训：跨帧取两张时
     * 骨骼动画在帧间的位移全部变成鬼影）。本方法只裁剪不抠像，抠像在
     * {@link #combine(NativeImage, NativeImage)} 里完成。
     * NativeImage.pixels 字段在 26.1.2 已私有化，裁剪用公开的 getPixel/setPixel 逐像素拷贝。
     *
     * @param scaleImage       每块的边长（物理像素）
     * @param magentaXPhysical 品红块左上角的物理 x 偏移（GUI 坐标 × guiScale）
     */
    public static void capturePair(int scaleImage, int magentaXPhysical, BiConsumer<NativeImage, NativeImage> callback) {
        // 尝试全屏截图
        Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget(), imageFull -> {
            NativeImage greenShot = cropRegion(imageFull, 0, 0, scaleImage);
            NativeImage magentaShot = cropRegion(imageFull, magentaXPhysical, 0, scaleImage);
            // 关闭全屏截图的缓存
            imageFull.close();
            callback.accept(greenShot, magentaShot);
        });
    }

    /**
     * useCalloc=true 让越界兜底区域保持全零（窗口物理尺寸装不下两块时不越界读取；
     * 两块的全零区域在差分中恰好判为全透明）
     */
    private static NativeImage cropRegion(NativeImage full, int x0, int y0, int size) {
        NativeImage image = new NativeImage(size, size, true);
        int copyWidth = Math.clamp(full.getWidth() - x0, 0, size);
        int copyHeight = Math.clamp(full.getHeight() - y0, 0, size);
        for (int cy = 0; cy < copyHeight; cy++) {
            for (int cx = 0; cx < copyWidth; cx++) {
                image.setPixel(cx, cy, full.getPixel(x0 + cx, y0 + cy));
            }
        }
        return image;
    }

    /**
     * 双背景差分合成。入参两图不被本方法关闭（调用方负责），返回新图。
     */
    public static NativeImage combine(NativeImage greenShot, NativeImage magentaShot) {
        int width = Math.min(greenShot.getWidth(), magentaShot.getWidth());
        int height = Math.min(greenShot.getHeight(), magentaShot.getHeight());
        NativeImage result = new NativeImage(width, height, true);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.setPixel(x, y, combinePixel(greenShot.getPixel(x, y), magentaShot.getPixel(x, y)));
            }
        }
        return result;
    }

    /**
     * 单像素差分：入参为绿幕/品红幕下的 ARGB 读回值（读回 α 恒为 FF，忽略），返回带真实 α 的 ARGB。
     */
    public static int combinePixel(int greenArgb, int magentaArgb) {
        int gR = (greenArgb >> 16) & 0xFF;
        int gG = (greenArgb >> 8) & 0xFF;
        int gB = greenArgb & 0xFF;
        int mR = (magentaArgb >> 16) & 0xFF;
        int mG = (magentaArgb >> 8) & 0xFF;
        int mB = magentaArgb & 0xFF;

        // B₁=(0,255,0)、B₂=(255,0,255)：每通道独立解出 (1-α)，取均值抗舍入噪声
        float t = ((mR - gR) + (gG - mG) + (mB - gB)) / (3f * 255f);
        t = Math.clamp(t, 0f, 1f);
        float alpha = 1f - t;
        if (alpha <= ALPHA_EPSILON) {
            return 0x00_00_00_00;
        }

        // F = (P₁ - B₁·(1-α)) / α，B₁ 仅绿通道非零
        int r = Math.clamp(Math.round(gR / alpha), 0, 255);
        int g = Math.clamp(Math.round((gG - 255f * t) / alpha), 0, 255);
        int b = Math.clamp(Math.round(gB / alpha), 0, 255);
        int a = Math.clamp(Math.round(alpha * 255f), 0, 255);
        return a << 24 | r << 16 | g << 8 | b;
    }
}
