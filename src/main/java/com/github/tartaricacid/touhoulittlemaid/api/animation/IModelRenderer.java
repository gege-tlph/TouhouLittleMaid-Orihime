package com.github.tartaricacid.touhoulittlemaid.api.animation;


import com.github.tartaricacid.simplebedrockmodel.client.bedrock.model.BedrockPart;

public interface IModelRenderer {
    /**
     * 获取模型渲染器对应的 BedrockPart。
     *
     * @return BedrockPart
     */
    BedrockPart getModelRenderer();

    /**
     * 获取模型的 x 轴旋转角度。
     *
     * @return 旋转角度
     */
    float getRotateAngleX();

    /**
     * 设置模型的 x 轴旋转角度。
     *
     * @param xRot x 旋转角度
     */
    void setRotateAngleX(float xRot);

    /**
     * 获取模型初始的 x 轴旋转角度。
     *
     * @return 初始旋转角度
     */
    float getInitRotateAngleX();

    /**
     * 获取模型的 y 轴旋转角度。
     *
     * @return 旋转角度
     */
    float getRotateAngleY();

    /**
     * 设置模型的 y 轴旋转角度。
     *
     * @param yRot y 轴旋转角度
     */
    void setRotateAngleY(float yRot);

    /**
     * 获取模型初始的 y 轴旋转角度。
     *
     * @return 初始旋转角度
     */
    float getInitRotateAngleY();

    /**
     * 获取模型的 z 轴旋转角度。
     *
     * @return 旋转角度
     */
    float getRotateAngleZ();

    /**
     * 设置模型的 z 轴旋转角度。
     *
     * @param zRot z 旋转角度
     */
    void setRotateAngleZ(float zRot);

    /**
     * 获取模型初始的 z 轴旋转角度。
     *
     * @return 初始旋转角度
     */
    float getInitRotateAngleZ();

    /**
     * 获取 ModelRenderer 的 x 偏移量
     *
     * @return x 轴偏移量
     */
    float getOffsetX();

    /**
     * 设置ModelRenderer的x偏移
     *
     * @param offsetX x 偏移量
     */
    void setOffsetX(float offsetX);

    /**
     * 获取 ModelRenderer 的 y 偏移量
     *
     * @return y 轴偏移量
     */
    float getOffsetY();

    /**
     * 设置ModelRenderer的y偏移
     *
     * @param offsetY y 轴偏移
     */
    void setOffsetY(float offsetY);

    /**
     * 获取 ModelRenderer 的 z 偏移
     *
     * @return z 轴偏移量
     */
    float getOffsetZ();

    /**
     * 设置 ModelRenderer 的 z 偏移
     *
     * @param offsetZ z 轴偏移
     */
    void setOffsetZ(float offsetZ);

    /**
     * 获取ModelRenderer的x旋转点
     *
     * @return x 轴旋转中心
     */
    float getRotationPointX();

    /**
     * 获取ModelRenderer的y旋转点
     *
     * @return y 轴旋转中心
     */
    float getRotationPointY();

    /**
     * 获取ModelRenderer的z旋转点
     *
     * @return z 轴旋转中心
     */
    float getRotationPointZ();

    /**
     * 判断模型部件是否隐藏。
     *
     * @return 布尔值
     */
    boolean isHidden();

    /**
     * 设置模型部件是否隐藏。
     *
     * @param hidden 布尔值
     */
    void setHidden(boolean hidden);
}
