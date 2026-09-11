package com.github.tartaricacid.touhoulittlemaid.geckolib3.geo;

import com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.render.built.GeoLocatorType;
import com.mojang.blaze3d.vertex.PoseStack;

import java.util.function.Consumer;

/**
 * 挂件 layer（{@link GeoLayerRenderer} 的各实现）定位骨骼所需要的最小接口。
 * <p>
 * 抽出这个接口是为了让 {@link GeckoRenderData#modelState} 既能是 TLM 自己的
 * {@link com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.animated.GeoModelState}
 * （gecko 模型走位姿动画时），也能是外部（如 YSM）提供的定位骨骼数据源——
 * 五个挂件 layer（拿持物/头饰/背包/背部物品/旗帜）全部只通过
 * {@link #visitLocatorGroup}/{@link #locatorGroupSize} 读取骨骼位置，
 * 从未直接触碰 {@code GeoModelState} 的其它字段，所以这次抽取不需要改动任何一个 layer 文件。
 */
public interface IGeoLocatorSource {
    /**
     * 对指定定位骨骼组下的每一根骨骼，把 {@code poseStack} 变换到该骨骼的世界位姿后调用 visitor。
     */
    void visitLocatorGroup(GeoLocatorType type, PoseStack poseStack, Consumer<PoseStack> visitor);

    /**
     * 指定定位骨骼组当前有多少根骨骼命中（模型没有声明该组定位骨骼时为 0）。
     */
    int locatorGroupSize(GeoLocatorType type);
}
