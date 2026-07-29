package com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor.ILocationBone;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoLocatorType;
import com.github.tartaricacid.touhoulittlemaid.geckolib3.util.RenderUtils;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.List;
import java.util.function.Consumer;

/**
 * 把 {@link ILocationModel} 的骨骼链包装成 {@link IGeoLocatorSource}——第三方模型系统
 * （目前是 YSM）给女仆挂 TLM 物件的通道。
 *
 * <p><b>每一条语义都照抄 {@code origin/1.21.1} 的 layer 实现</b>
 * （{@code client/renderer/entity/geckolayer/GeckoLayerMaid{Backpack,Held,BipedHead,Banner}}），
 * 不是照着「应该怎么写」推导的：</p>
 *
 * <ul>
 *   <li><b>就位方式</b>：{@code pushPose} → {@link RenderUtils#prepMatrixForLocator} → 回调 → {@code popPose}。
 *       它对链上前 n−1 级做完整的骨骼变换，对最后一级做「移到骨骼 → 移到 pivot → 绕 pivot 旋转 → 缩放」。
 *       pivot 两侧都除以 16，是同一套 Blockbench 单位，<b>不需要换算</b>。
 *       <p>⚠️ 这与 {@link GeoModelState#visitLocatorGroup} 的「{@code mulPose(累积矩阵)} +
 *       {@code translate(pivot/16)}」<b>不是同一个公式</b>，也没有人证明过两者等价——它们是两套模型系统
 *       各自的约定。本类的正确性判据不是「与 gecko 那条路等价」，而是<b>「与 {@code origin/1.21.1}
 *       在 ILocationModel 这条路上的行为逐字一致」</b>。改动本类时请回去比对基准，不要拿 gecko 那条路当参照。</p></li>
 *   <li><b>手部 = 主链 + 每条 extra 链</b>：基准的 {@code renderArmWithItem} 先按主链画一次，
 *       再对 {@code extraLeftHandBones()} / {@code extraRightHandBones()} 里的每条链各画一次。
 *       所以一个定位组可以对应多个定位点，与 TLM gecko 那侧「一个定位组可含多根骨骼」同构。</li>
 *   <li><b>缩放全为 0 则跳过</b>：基准用 {@code prepMatrixForLocator} 的返回值当门禁
 *       （模型作者靠把骨骼缩放归零来隐藏挂点）。<b>注意两条路径的判据本就不同</b>：
 *       TLM gecko 那侧在 {@code GeoModelStateExtractor} 里用的是「任意<b>两个</b>分量为 0」，
 *       这里是「<b>三个</b>分量全为 0」。不是笔误，各自照抄各自的基准。</li>
 * </ul>
 *
 * <p>⚠️ {@code GeoLocatorType} 有意不是枚举（见其类注释），故这里用 {@code ==} 比较静态常量，
 * 不能写 {@code switch}。</p>
 */
public final class LocationModelLocatorSource implements IGeoLocatorSource {
    private final ILocationModel model;

    public LocationModelLocatorSource(ILocationModel model) {
        this.model = model;
    }

    @Override
    public void visitLocatorGroup(GeoLocatorType type, PoseStack poseStack, Consumer<PoseStack> visitor) {
        if (type == GeoLocatorType.BACKPACK) {
            visitChain(model.backpackBones(), poseStack, visitor);
        } else if (type == GeoLocatorType.HEAD) {
            visitChain(model.headBones(), poseStack, visitor);
        } else if (type == GeoLocatorType.RIGHT_HAND) {
            visitChain(model.rightHandBones(), poseStack, visitor);
            visitExtraChains(model.extraRightHandBones(), poseStack, visitor);
        } else if (type == GeoLocatorType.LEFT_HAND) {
            visitChain(model.leftHandBones(), poseStack, visitor);
            visitExtraChains(model.extraLeftHandBones(), poseStack, visitor);
        }
    }

    /**
     * <b>这是「过滤前」的定位点数</b>，与基准里 {@code !model.backpackBones().isEmpty()} 的判据一致。
     *
     * <p>唯一的消费者是 {@code GeckoLayerMaidBackItem}，它拿这个数只为回答「这个模型有没有背包定位组」，
     * 有则画在定位组上、没有则回落到实体坐标系。缩放归零的挂点在 {@link #visitLocatorGroup} 里跳过，
     * 那时已经画不出东西，不影响这个判断。</p>
     */
    @Override
    public int locatorGroupSize(GeoLocatorType type) {
        if (type == GeoLocatorType.BACKPACK) {
            return model.backpackBones().isEmpty() ? 0 : 1;
        }
        if (type == GeoLocatorType.HEAD) {
            return model.headBones().isEmpty() ? 0 : 1;
        }
        if (type == GeoLocatorType.RIGHT_HAND) {
            return (model.rightHandBones().isEmpty() ? 0 : 1) + model.extraRightHandBones().size();
        }
        if (type == GeoLocatorType.LEFT_HAND) {
            return (model.leftHandBones().isEmpty() ? 0 : 1) + model.extraLeftHandBones().size();
        }
        return 0;
    }

    private static void visitExtraChains(List<List<? extends ILocationBone>> chains, PoseStack poseStack,
                                         Consumer<PoseStack> visitor) {
        for (List<? extends ILocationBone> chain : chains) {
            visitChain(chain, poseStack, visitor);
        }
    }

    private static void visitChain(List<? extends ILocationBone> chain, PoseStack poseStack,
                                   Consumer<PoseStack> visitor) {
        if (chain.isEmpty()) {
            // 基准每处都先判 isEmpty：prepMatrixForLocator 会取 size()-1 下标，空链直接越界
            return;
        }
        poseStack.pushPose();
        boolean scaleAllIsZero = RenderUtils.prepMatrixForLocator(poseStack, chain);
        if (!scaleAllIsZero) {
            visitor.accept(poseStack);
        }
        poseStack.popPose();
    }
}
