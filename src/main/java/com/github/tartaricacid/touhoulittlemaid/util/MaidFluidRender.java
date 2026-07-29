package com.github.tartaricacid.touhoulittlemaid.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Optional;


/**
 * From JEI: <a href="https://github.com/mezz/JustEnoughItems/blob/1.20/Fabric/src/main/java/mezz/jei/fabric/platform/FluidHelper.java">...</a>
 */
@Environment(EnvType.CLIENT)
public final class MaidFluidRender {
    private static final int TEXTURE_SIZE = 16;

    public static Component getFluidName(String fluidId, long amount) {
        // 1.21.11: Registry.get(Identifier) 返回 Optional<Reference<T>> → getValue 保留 1.21.1 defaulted 语义（在树先例 SetTankCountFunction）
        Fluid fluid = BuiltInRegistries.FLUID.getValue(Identifier.parse(fluidId));
        if (amount <= 0 || fluid == null || fluid.isSame(Fluids.EMPTY)) {
            return Component.translatable("tooltips.touhou_little_maid.tank_backpack.empty_fluid");
        }
        //return fluid.getFluidType().getDescription();
        return FluidVariantAttributes.getName(FluidVariant.of(fluid));
    }

    public static void drawFluid(GuiGraphics graphics, int x, int y, int width, int height, String fluidId, long amount, long capacity) {
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
            // 1.21.11: pose() 为 Matrix3x2fStack（GUI 2D 化），pushPose/popPose → pushMatrix/popMatrix，translate 去 z
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            drawTiledSprite(graphics, width, height, fluidColor, scaledAmount, fluidStillSprite);
            graphics.pose().popMatrix();
        });
    }

    public static int getColorTint(FluidVariant ingredient) {
        //Fluid fluid = ingredient.getFluid();
        //IClientFluidTypeExtensions renderProperties = IClientFluidTypeExtensions.of(fluid);
        //return renderProperties.getTintColor(ingredient);
        return FluidVariantRendering.getColor(ingredient);
    }

    public static Optional<TextureAtlasSprite> getStillFluidSprite(FluidVariant fluidStack) {
        //Fluid fluid = fluidStack.getFluid();
        //IClientFluidTypeExtensions renderProperties = IClientFluidTypeExtensions.of(fluid);
        //Identifier fluidStill = renderProperties.getStillTexture(fluidStack);
        TextureAtlasSprite fluidStill = FluidVariantRendering.getSprite(fluidStack);
        return Optional.ofNullable(fluidStill)
/*                .map(f -> Minecraft.getInstance()
                        .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                        .apply(f)
                )
                .filter(s -> s.atlasLocation() != MissingTextureAtlasSprite.getLocation())*/;
    }

    private static void drawTiledSprite(GuiGraphics guiGraphics, final int tiledWidth, final int tiledHeight, int color, long scaledAmount, TextureAtlasSprite sprite) {
        // 1.21.11: setShaderTexture/setShaderColor + Tesselator 直绘已移除；
        // 贴图由 sprite.atlasLocation() 经 blitSprite 提供，染色经 blitSprite 的 color 参数（ARGB 乘算，等价原 setGLColorFromInt）
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

    private static void drawTextureWithMasking(GuiGraphics guiGraphics, int xCoord, int yCoord, TextureAtlasSprite textureSprite, long maskTop, long maskRight, int color) {
        // 1.21.11: GUI Tesselator/BufferUploader 直绘路径已移除 → scissor + 整 sprite blit 重derive。
        // 与 origin 逐像素等价：origin 把 sprite 左上 (16-maskRight)x(16-maskTop) 子区域按原比例绘制于
        // (x, y+maskTop)..(x+16-maskRight, y+16)；此处将整个 16x16 sprite 绘制于 (x, y+maskTop) 并用
        // scissor 裁掉右侧 maskRight 列与底部超界部分，像素映射相同（enableScissor 经 transformAxisAligned
        // 随 pose 变换，上层 translate 安全）。zLevel(100) 随 GUI 2D 化丢弃。
        guiGraphics.enableScissor(xCoord, yCoord + (int) maskTop, xCoord + TEXTURE_SIZE - (int) maskRight, yCoord + TEXTURE_SIZE);
        guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, textureSprite, xCoord, yCoord + (int) maskTop, TEXTURE_SIZE, TEXTURE_SIZE, color);
        guiGraphics.disableScissor();
    }
}
