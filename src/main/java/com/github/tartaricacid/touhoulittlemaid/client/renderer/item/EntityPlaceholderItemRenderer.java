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
 * [Codex] Dynamic altar-result icon renderer. It preserves origin's
 * recipe-id-to-texture fallback without relying on the removed BEWLR API.
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
        // origin rendered the baked item model of ns:item/<name>, so the icon animated with
        // the atlas. A directly bound texture never does: .mcmeta frames are ticked on the
        // stitched sprite. Resolving that sprite is the faithful 1.21.11 equivalent.
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
     * The GUI caches a rendered icon under {@code TrackingItemStackRenderState.getModelIdentity}
     * and only redraws every frame once the state is marked animated. Only BlockModelWrapper does
     * that; SpecialModelWrapper marks a state animated for enchantment glint alone, and a special
     * renderer cannot reach the state to mark it itself. So for an animated sprite we fold the
     * game time into the argument instead: the identity then changes each tick, the cache misses,
     * and the frames advance at their real rate rather than whenever the cache happens to drop.
     */
    private static long animationEpoch(TextureAtlasSprite sprite) {
        if (!sprite.contents().isAnimated()) {
            return 0L;
        }
        Level level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }

    /**
     * @return the stitched item-atlas sprite, or null when the id is not on the atlas, which
     * mirrors origin falling back to a flat texture when the recipe had no baked model.
     */
    private static TextureAtlasSprite findItemSprite(Identifier spriteId) {
        // Look the sprite up through Material, NOT getAtlasOrThrow: the latter keys on the
        // atlas id while TextureAtlas.LOCATION_ITEMS is the atlas texture path, so it threw
        // "Invalid atlas id". AtlasManager.get(Material) keys on exactly that texture path
        // and answers with the atlas' missing sprite when the id was never stitched.
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
                .get(new Material(TextureAtlas.LOCATION_ITEMS, spriteId));
        return spriteId.equals(sprite.contents().name()) ? sprite : null;
    }

    @Override
    public void submit(Icon icon, ItemDisplayContext displayContext, PoseStack poseStack,
                       SubmitNodeCollector collector, int light, int overlay, boolean foil, int outlineColor) {
        // Geometry is deliberately untouched: this placement is shared with
        // PicnicBasketItemRenderer, which renders correctly, so only the texture source
        // differs between the animated and the flat fallback path.
        TextureAtlasSprite sprite = icon.sprite();
        RenderType renderType = RenderTypes.entityCutoutNoCull(
                sprite != null ? sprite.atlasLocation() : icon.texture());
        poseStack.pushPose();
        poseStack.translate(0.5, 1.5, 0.5);
        poseStack.mulPose(Axis.ZN.rotationDegrees(180));
        collector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            PoseStack geometryPose = new PoseStack();
            geometryPose.last().set(pose);
            // NOT sprite.wrap(): SpriteCoordinateExpander.addVertex returns the delegate
            // rather than itself, so the chained setColor().setUv() that BedrockCubeBox emits
            // lands on the raw buffer and the remap is skipped, leaving the model sampling the
            // whole 1024x512 item atlas (renders as colour noise). This wrapper returns itself.
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
     * Exactly one of the two is set: an item-atlas sprite when the recipe has one (animated),
     * otherwise the flat texture origin fell back to.
     */
    public record Icon(TextureAtlasSprite sprite, Identifier texture, long animationEpoch) {
    }

    /**
     * Chain-safe replacement for {@link TextureAtlasSprite#wrap}: every builder method returns
     * {@code this}, so a model that emits {@code addVertex(..).setColor(..).setUv(..)} keeps
     * going through the remap instead of falling back to the raw buffer.
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
