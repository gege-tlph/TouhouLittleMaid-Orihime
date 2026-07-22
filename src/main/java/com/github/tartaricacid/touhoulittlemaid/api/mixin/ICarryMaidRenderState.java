package com.github.tartaricacid.touhoulittlemaid.api.mixin;

/**
 * 客户端渲染状态标志用于保留携带女仆的玩家手臂姿势。
 */
public interface ICarryMaidRenderState {
    boolean tlm$isCarryingMaid();

    void tlm$setCarryingMaid(boolean carryingMaid);
}
