package com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoLocatorType;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.function.Consumer;

/**
 * 「把 poseStack 送进某个定位组的坐标系」这件事的来源，与模型系统解耦。
 *
 * <p><b>为什么需要这一层</b>：女仆有两套模型系统，两者提供定位信息的形态完全不同——
 * TLM 自带的 gecko 给的是 {@link GeoModelState} 里预先算好的**累积变换矩阵**，
 * 而 YSM 模型给的是 {@link ILocationModel} 的**骨骼链**（每根只有局部 TRS，需逐级复合）。
 * 挂件 layer（手持物 / 头顶方块 / 背包 / 背部物品 / 背旗）本身与模型系统无关，
 * 它们只想问一句「定位组在哪」，所以那句话应该问到这个接口上，而不是问到某一套模型状态上。</p>
 *
 * <p>{@code origin/1.21.1} 的 layer 走的正是 {@link ILocationModel}（见
 * {@code client/renderer/entity/geckolayer/}），移植到 1.21.11 时改成直接持有 {@code GeoModelState}，
 * {@code ILocationModel} 就此成为零消费者接口——YSM 模型的女仆不再渲染任何挂件。
 * 本接口把两条来源重新收敛到一处，**layer 只有一条路径**，日后再来第三种模型系统也只改这一层。</p>
 */
public interface IGeoLocatorSource {
    /**
     * 对该定位组的**每一个**定位点，把 poseStack 送进它的坐标系并回调一次。
     *
     * <p>实现方负责 push/pop 与「不该渲染的定位点直接跳过」，因此 visitor 收到的一定是
     * 一个可以直接开画的坐标系——调用方不必再判空、判缩放。</p>
     */
    void visitLocatorGroup(GeoLocatorType type, PoseStack poseStack, Consumer<PoseStack> visitor);

    /**
     * 该定位组有几个定位点。0 表示这个模型没有该定位组，调用方据此走回落画法
     * （{@code GeckoLayerMaidBackItem} 就是这么用的：没有背包定位组时直接画在实体坐标系里）。
     */
    int locatorGroupSize(GeoLocatorType type);
}
