package com.github.tartaricacid.touhoulittlemaid.geckolib3.util;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

import java.util.List;

/**
 * {@link ILocationBone} 骨骼链 → {@link PoseStack} 位姿的复合工具。
 * <p>
 * 只收 {@code LocationModelLocatorSource} 实际用到的方法——这不是照抄一整个通用几何库，
 * 是照抄 {@code origin/1.21.1} 那套挂件 layer（{@code GeckoLayerMaid{Backpack,Held,BipedHead,Banner}}）
 * 里逐字一致的位姿复合公式。改动前请回去比对基准，不要凭直觉"优化"。
 */
public final class RenderUtils {
    private RenderUtils() {
    }

    public static void translateMatrixToBone(PoseStack poseStack, ILocationBone bone) {
        poseStack.translate(-bone.getPositionX() / 16f, bone.getPositionY() / 16f, bone.getPositionZ() / 16f);
    }

    public static void rotateMatrixAroundBone(PoseStack poseStack, ILocationBone bone) {
        if (bone.getRotationZ() != 0.0F || bone.getRotationY() != 0.0F || bone.getRotationX() != 0.0F) {
            poseStack.mulPose(new Quaternionf().rotateZYX(bone.getRotationZ(), bone.getRotationY(), bone.getRotationX()));
        }
    }

    /**
     * 缩放三个分量全为 0 时返回 true。
     * <p>
     * ⚠️ 判据是"三个分量全为 0"，不是 TLM gecko 那侧 {@code GeoModelStateExtractor} 用的
     * "任意两个分量为 0"——两套模型系统各自的隐藏骨骼约定，不是笔误，不要统一。
     */
    public static boolean scaleMatrixForBone(PoseStack poseStack, ILocationBone bone) {
        float scaleX = bone.getScaleX();
        float scaleY = bone.getScaleY();
        float scaleZ = bone.getScaleZ();
        poseStack.scale(scaleX, scaleY, scaleZ);
        return scaleX == 0 && scaleY == 0 && scaleZ == 0;
    }

    public static void translateToPivotPoint(PoseStack poseStack, ILocationBone bone) {
        poseStack.translate(bone.getPivotX() / 16f, bone.getPivotY() / 16f, bone.getPivotZ() / 16f);
    }

    public static void translateAwayFromPivotPoint(PoseStack poseStack, ILocationBone bone) {
        poseStack.translate(-bone.getPivotX() / 16f, -bone.getPivotY() / 16f, -bone.getPivotZ() / 16f);
    }

    /**
     * 链上非末级骨骼的完整变换：移到骨骼 → 移到 pivot → 绕 pivot 旋转 → 缩放 → 移回。
     * 缩放全为 0 时返回 true（供调用方决定是否跳过整条链）。
     */
    public static boolean prepMatrixForBone(PoseStack poseStack, ILocationBone bone) {
        translateMatrixToBone(poseStack, bone);
        translateToPivotPoint(poseStack, bone);
        rotateMatrixAroundBone(poseStack, bone);
        boolean scaleAllIsZero = scaleMatrixForBone(poseStack, bone);
        translateAwayFromPivotPoint(poseStack, bone);
        return scaleAllIsZero;
    }

    /**
     * 把 poseStack 送进一条骨骼链末端定位点的坐标系：对前 n-1 级做完整变换（
     * {@link #prepMatrixForBone}），对最后一级只做「移到骨骼 → 移到 pivot → 绕 pivot 旋转 → 缩放」
     * （不移回 pivot，好让调用方直接在 pivot 处开画）。
     * <p>
     * ⚠️ 返回值<b>只看链上前 n-1 级（祖先骨骼）的缩放是否全零</b>，最后一级（定位点本身）的
     * 缩放会被应用但<b>不计入判据</b>——照抄基准逐字如此，不是遗漏，不要"顺手"把它也 OR 进去。
     *
     * @return 链上任意一根<b>祖先</b>骨骼缩放全为 0 时为 true（挂点被模型作者隐藏）
     */
    public static boolean prepMatrixForLocator(PoseStack poseStack, List<? extends ILocationBone> locatorHierarchy) {
        boolean scaleCheck = false;
        for (int i = 0; i < locatorHierarchy.size() - 1; i++) {
            boolean result = RenderUtils.prepMatrixForBone(poseStack, locatorHierarchy.get(i));
            if (result) {
                scaleCheck = true;
            }
        }
        ILocationBone lastBone = locatorHierarchy.get(locatorHierarchy.size() - 1);
        RenderUtils.translateMatrixToBone(poseStack, lastBone);
        RenderUtils.translateToPivotPoint(poseStack, lastBone);
        RenderUtils.rotateMatrixAroundBone(poseStack, lastBone);
        RenderUtils.scaleMatrixForBone(poseStack, lastBone);
        return scaleCheck;
    }
}
