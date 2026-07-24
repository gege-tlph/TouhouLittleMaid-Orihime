package com.github.tartaricacid.touhoulittlemaid.client.renderer.item;

import com.github.tartaricacid.touhoulittlemaid.client.model.EntityPlaceholderModel;
import com.github.tartaricacid.touhoulittlemaid.item.ItemEntityPlaceholder;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * 根据祭坛配方动态选择图标的占位物品渲染器。
 */
public final class EntityPlaceholderItemRenderer implements SpecialModelRenderer<EntityPlaceholderItemRenderer.Icon> {
    public static final Identifier ID = IdentifierUtil.modLoc("entity_placeholder_item");
    private static final Identifier FALLBACK = IdentifierUtil.modLoc("textures/item/entity_placeholder.png");
    private static final EntityPlaceholderModel MODEL = new EntityPlaceholderModel();

    @Override
    public Icon extractArgument(ItemStack stack) {
        Identifier recipeId = ItemEntityPlaceholder.getRecipeId(stack);
        if (recipeId == null) {
            return new Icon(null, FALLBACK, 0L);
        }
        Path path = Paths.get(recipeId.getPath());
        String name = path.getFileName().toString();

        TextureAtlasSprite sprite = findItemSprite(
                Identifier.fromNamespaceAndPath(recipeId.getNamespace(), "item/" + name));
        if (sprite != null) {
            return new Icon(sprite, null, animationEpoch(sprite));
        }
        Identifier texture = Identifier.fromNamespaceAndPath(recipeId.getNamespace(),
                "textures/item/%s.png".formatted(name));
        ResourceManager resources = Minecraft.getInstance().getResourceManager();
        return new Icon(null, resources.getResource(texture).isPresent() ? texture : FALLBACK, 0L);
    }

    /**
     * GUI 会按模型标识缓存物品图标，而特殊模型无法自行把状态标记为逐帧动画。
     * 对动画精灵加入当前游戏刻，使缓存每刻失效，确保序列帧按正常速度更新。
     */
    private static long animationEpoch(TextureAtlasSprite sprite) {
        if (!sprite.contents().isAnimated()) {
            return 0L;
        }
        Level level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }

    /**
     * 从物品图集中查找已缝合的精灵；找不到时返回 {@code null}，由调用方改用独立纹理。
     */
    private static TextureAtlasSprite findItemSprite(Identifier spriteId) {
        // TextureAtlas.LOCATION_ITEMS 是图集纹理路径，并不是图集注册键，因此需通过 Material 查询。
        // 返回后再核对精灵名称，以区分目标精灵和缺失纹理占位符。
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
                .get(new Material(TextureAtlas.LOCATION_ITEMS, spriteId));
        return spriteId.equals(sprite.contents().name()) ? sprite : null;
    }

    @Override
    public void submit(Icon icon, ItemDisplayContext displayContext, PoseStack poseStack,
                       SubmitNodeCollector collector, int light, int overlay, boolean foil, int outlineColor) {
        // 与野餐篮图标使用相同的四边形几何；动画路径和回退路径只更换纹理来源。
        TextureAtlasSprite sprite = icon.sprite();
        RenderType renderType = RenderTypes.entityCutoutNoCull(
                sprite != null ? sprite.atlasLocation() : icon.texture());
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.ZN.rotationDegrees(180));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack geometryPose = new PoseStack();
            geometryPose.last().set(pose);

            VertexConsumer target = sprite != null ? new SpriteUv(buffer, sprite) : buffer;
            MODEL.renderToBuffer(geometryPose, target, light, overlay, -1);
        });
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(0, 0, 0));
        output.accept(new Vector3f(1, 1, 0));
    }

    /**
     * 图标数据。{@code sprite} 与 {@code texture} 只会设置其中一个：前者支持图集动画，后者用于回退纹理。
     */
    public record Icon(TextureAtlasSprite sprite, Identifier texture, long animationEpoch) {
    }

    /**
     * 可安全链式调用的 UV 重映射包装器。每个构建方法都返回自身，确保后续调用仍经过精灵坐标转换。
     */
    private record SpriteUv(VertexConsumer delegate, TextureAtlasSprite sprite) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.delegate.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            this.delegate.setColor(color);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.delegate.setUv(this.sprite.getU(u), this.sprite.getV(v));
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.delegate.setNormal(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            this.delegate.setLineWidth(width);
            return this;
        }
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);

        @Override
        public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext context) {
            return new EntityPlaceholderItemRenderer();
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
