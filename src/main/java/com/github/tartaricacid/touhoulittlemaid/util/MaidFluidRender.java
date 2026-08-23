package com.github.tartaricacid.touhoulittlemaid.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Optional;

/**
 * 把流体纹理画进 GUI（源自 JEI 的 FluidHelper），自行为基准搬入。
 *
 * <p>26.1.2：绘制入口从 {@code GuiGraphics} 改为 {@code GuiGraphicsExtractor}
 * （与棋局预览那刀同源的 GUI 抽取模型漂移），blitSprite/scissor/pose 家族签名
 * 经 javap 实查逐字一致，绘制逻辑不动。</p>
 *
 * From JEI: <a href="https://github.com/mezz/JustEnoughItems/blob/1.20/Fabric/src/main/java/mezz/jei/fabric/platform/FluidHelper.java">...</a>
 */
@Environment(EnvType.CLIENT)
public final class MaidFluidRender {
    private static final int TEXTURE_SIZE = 16;

    public static Component getFluidName(String fluidId, long amount) {
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(fluidId));
        if (amount <= 0 || fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return Component.translatable("tooltips.touhou_little_maid.tank_backpack.empty_fluid");
        }
        return FluidVariantAttributes.getName(FluidVariant.of(fluid));
    }

    public static void drawFluid(GuiGraphicsExtractor graphics, int x, int y, int width, int height, String fluidId, long amount, long capacity) {
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(fluidId));
        if (amount <= 0 || fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return;
        }
        FluidVariant fluidStack = FluidVariant.of(fluid);
        getStillFluidSprite(fluidStack).ifPresent(fluidStillSprite -> {
            int fluidColor = getColorTint(fluidStack);
            long scaledAmount = (amount * height) / capacity;
            if (scaledAmount < 1) {
                // 至少渲染一行像素，让人知道里面有东西
                scaledAmount = 1;
            }
            if (scaledAmount > height) {
                scaledAmount = height;
            }
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            drawTiledSprite(graphics, width, height, fluidColor, scaledAmount, fluidStillSprite);
            graphics.pose().popMatrix();
        });
    }

    public static int getColorTint(FluidVariant ingredient) {
        return FluidVariantRendering.getColor(ingredient);
    }

    public static Optional<TextureAtlasSprite> getStillFluidSprite(FluidVariant fluidStack) {
        // 26.1.2：Fabric 的 FluidVariantRendering.getSprite 已删（流体渲染并进原版 FluidModel），
        // still sprite 走 ModelManager 的 FluidStateModelSet——JEI 29.5 Fabric 版同款（字节码实查）
        TextureAtlasSprite fluidStill = net.minecraft.client.Minecraft.getInstance().getModelManager()
                .getFluidStateModelSet()
                .get(fluidStack.getFluid().defaultFluidState())
                .stillMaterial()
                .sprite();
        return Optional.ofNullable(fluidStill);
    }

    private static void drawTiledSprite(GuiGraphicsExtractor guiGraphics, final int tiledWidth, final int tiledHeight, int color, long scaledAmount, TextureAtlasSprite sprite) {
        final int xTileCount = tiledWidth / TEXTURE_SIZE;
        final int xRemainder = tiledWidth - (xTileCount * TEXTURE_SIZE);
        final long yTileCount = scaledAmount / TEXTURE_SIZE;
        final long yRemainder = scaledAmount - (yTileCount * TEXTURE_SIZE);

        for (int xTile = 0; xTile <= xTileCount; xTile++) {
            for (int yTile = 0; yTile <= yTileCount; yTile++) {
                int width = (xTile == xTileCount) ? xRemainder : TEXTURE_SIZE;
                long height = (yTile == yTileCount) ? yRemainder : TEXTURE_SIZE;
                int x = (xTile * TEXTURE_SIZE);
                int y = tiledHeight - ((yTile + 1) * TEXTURE_SIZE);
                if (width > 0 && height > 0) {
                    long maskTop = TEXTURE_SIZE - height;
                    int maskRight = TEXTURE_SIZE - width;
                    drawTextureWithMasking(guiGraphics, x, y, sprite, maskTop, maskRight, color);
                }
            }
        }
    }

    private static void drawTextureWithMasking(GuiGraphicsExtractor guiGraphics, int xCoord, int yCoord, TextureAtlasSprite textureSprite, long maskTop, long maskRight, int color) {
        // scissor + 整 sprite blit：把 16x16 sprite 画在 (x, y+maskTop)，裁掉右 maskRight 列与底部超界，
        // 与 origin 的 Tesselator 直绘逐像素等价（行为基准同款，见其注释）
        guiGraphics.enableScissor(xCoord, yCoord + (int) maskTop, xCoord + TEXTURE_SIZE - (int) maskRight, yCoord + TEXTURE_SIZE);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, textureSprite, xCoord, yCoord + (int) maskTop, TEXTURE_SIZE, TEXTURE_SIZE, color);
        guiGraphics.disableScissor();
    }
}
