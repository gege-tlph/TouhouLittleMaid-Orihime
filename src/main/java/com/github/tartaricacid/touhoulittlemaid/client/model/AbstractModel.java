package com.github.tartaricacid.touhoulittlemaid.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * origin/1.21.1：{@code AbstractModel<E extends Entity> extends EntityModel<E>}，无根骨骼构造。
 * <p>
 * 1.21.11 EntityModel 泛型改为 RenderState 系，且构造器强制传入根 {@link ModelPart}
 * （几何经 root().render() 渲染，renderToBuffer 已 final），故构造器增加 root 参数；
 * 默认 RenderType 仍为 origin 的 entityCutoutNoCull。
 */
public abstract class AbstractModel<E extends EntityRenderState> extends EntityModel<E> {
    public AbstractModel(ModelPart root, Function<Identifier, RenderType> pRenderType) {
        super(root, pRenderType);
    }

    protected AbstractModel(ModelPart root) {
        this(root, RenderTypes::entityCutoutNoCull);
    }

    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer buffer, int light, int overlay, float r, float g, float b, float a) {
        this.renderToBuffer(poseStack, buffer, light, overlay);
    }
}
