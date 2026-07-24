package com.github.tartaricacid.simplebedrockmodel.client.bedrock.model;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * 扩展 {@link ModelPart} 以支持自发光、镜像等基岩版模型特性。
 * 所需成员通过 {@code touhou_little_maid.accesswidener} 开放访问。
 */
public class BedrockPart extends ModelPart {
    private static final int MAX_LIGHT_TEXTURE = Mth.square(15);
    private static final Vector3f[] NORMALS = Util.make(new Vector3f[6], array -> {
        for (int i = 0; i < array.length; i++) {
            array[i] = new Vector3f();
        }
    });

    public float offsetX = 0, offsetY = 0, offsetZ = 0;
    public @Nullable BedrockPart parent = null;
    public boolean illuminated = false;
    public boolean mirror = false;

    public BedrockPart() {
        super(Lists.newArrayList(), new Object2ObjectArrayMap<>());
    }

    @Override
    public void resetPose() {
        super.resetPose();
        this.offsetX = 0;
        this.offsetY = 0;
        this.offsetZ = 0;
    }


    @Override
    public void render(PoseStack poseStack, VertexConsumer consumer, int lightmap, int overlay, int color) {
        this.renderBedrock(poseStack, consumer, lightmap, overlay, color);
    }

    public void renderBedrock(PoseStack poseStack, VertexConsumer consumer, int lightmap, int overlay, int color) {
        int cubePackedLight = illuminated ? MAX_LIGHT_TEXTURE : lightmap;
        if (this.visible) {
            boolean xNearZero = -1E-5F < xScale && xScale < 1E-5F;
            boolean yNearZero = -1E-5F < yScale && yScale < 1E-5F;
            boolean zNearZero = -1E-5F < zScale && zScale < 1E-5F;
            if ((xNearZero && yNearZero) || (xNearZero && zNearZero) || (yNearZero && zNearZero)) {
                return;
            }

            if (!this.cubes.isEmpty() || !this.children.isEmpty()) {
                poseStack.pushPose();
                this.translateAndRotateBedrock(poseStack);
                if (!this.skipDraw) {
                    this.compileBedrock(poseStack.last(), consumer, cubePackedLight, overlay, color);
                }

                for (var part : this.children.values()) {
                    if (part instanceof BedrockPart bedrockPart) {
                        bedrockPart.render(poseStack, consumer, cubePackedLight, overlay, color);
                    }
                }

                poseStack.popPose();
            }
        }
    }

    public void translateAndRotateBedrock(PoseStack poseStack) {
        poseStack.translate((this.x / 16.0F) + this.offsetX, (this.y / 16.0F) + this.offsetY, (this.z / 16.0F) + this.offsetZ);
        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
            poseStack.mulPose(new Quaternionf().rotationZYX(this.zRot, this.yRot, this.xRot));
        }

        if (this.xScale != 1.0F || this.yScale != 1.0F || this.zScale != 1.0F) {
            poseStack.scale(this.xScale, this.yScale, this.zScale);
        }
    }

    /**
     * 返回欧拉旋转后的部分局部平移位置。
     */
    public Vector3f getTranslateAndRotateVector3f() {
        Quaternionf rotation = new Quaternionf();
        if (this.xRot != 0.0F || this.yRot != 0.0F || this.zRot != 0.0F) {
            rotation.rotateZYX(this.zRot, this.yRot, this.xRot);
        }
        Vector3f translation = new Vector3f(
                (this.x / 16.0F) + this.offsetX,
                (this.y / 16.0F) + this.offsetY,
                (this.z / 16.0F) + this.offsetZ);
        return rotation.transform(translation);
    }

    public void compileBedrock(PoseStack.Pose pose, VertexConsumer consumer, int lightmap, int overlay, int color) {
        Matrix3f normal = pose.normal();
        NORMALS[0].set(-normal.m10, -normal.m11, -normal.m12);
        NORMALS[1].set(normal.m10, normal.m11, normal.m12);
        NORMALS[2].set(-normal.m20, -normal.m21, -normal.m22);
        NORMALS[3].set(normal.m20, normal.m21, normal.m22);
        NORMALS[4].set(-normal.m00, -normal.m01, -normal.m02);
        NORMALS[5].set(normal.m00, normal.m01, normal.m02);
        for (var cube : this.cubes) {
            if (cube instanceof BedrockCube bedrockCube) {
                bedrockCube.compile(pose, NORMALS, consumer, lightmap, overlay, color);
            }
        }
    }

    public void addChild(String name, BedrockPart model) {
        this.children.put(name, model);
        model.parent = this;
    }

    public void addCube(BedrockCube cube) {
        if (cube instanceof ModelPart.Cube c) {
            this.cubes.add(c);
        }
    }

    @Nullable
    public BedrockPart getParent() {
        return parent;
    }

    public float getInitRotX() {
        return this.initialPose.xRot();
    }

    public float getInitRotY() {
        return this.initialPose.yRot();
    }

    public float getInitRotZ() {
        return this.initialPose.zRot();
    }
}
