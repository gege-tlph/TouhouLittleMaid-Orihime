package com.github.tartaricacid.touhoulittlemaid.geckolib3.core.processor;

/**
 * 第三方模型系统（目前是 YSM）向 TLM 描述一根骨骼局部位姿的最小契约。
 * <p>
 * 与 TLM 自己 gecko 层的 {@code GeoModelState} 不同——那边给的是预先算好的
 * <b>累积变换矩阵</b>；这里给的是<b>相对父级的局部 TRS（平移/旋转/缩放）+ pivot</b>，
 * 需要沿骨骼链逐级复合才能得到世界位姿，见 {@code LocationModelLocatorSource}。
 */
public interface ILocationBone {
    float getRotationX();

    float getRotationY();

    float getRotationZ();

    float getPositionX();

    float getPositionY();

    float getPositionZ();

    float getScaleX();

    float getScaleY();

    float getScaleZ();

    float getPivotX();

    float getPivotY();

    float getPivotZ();
}
