package com.github.tartaricacid.touhoulittlemaid.client.renderer.entity.state;

public enum ModelType {
    NONE,
    SIMPLE_BEDROCK,
    GECKO,
    /**
     * 本体渲染整体交给第三方模型系统（目前是 YSM）接管——TLM 自己不画身体，
     * 只在接管方画完之后补挂件 layer（见 {@code YsmMaidLayerBridge}）。
     */
    YSM,
}
