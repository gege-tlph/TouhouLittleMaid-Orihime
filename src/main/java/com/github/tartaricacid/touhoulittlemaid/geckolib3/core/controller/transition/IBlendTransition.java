package com.github.tartaricacid.touhoulittlemaid.geckolib3.core.controller.transition;

public interface IBlendTransition {
    float get(float tick);

    /**
     * 返回过渡持续的游戏刻数。
     */
    float length();

    default IBlendTransition startNew() {
        return this;
    }
}
