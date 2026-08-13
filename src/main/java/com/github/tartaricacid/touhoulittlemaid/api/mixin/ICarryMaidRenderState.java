package com.github.tartaricacid.touhoulittlemaid.api.mixin;

/**
 * 「这个玩家正扛着女仆」这一位，挂在客户端渲染状态上。
 *
 * <p>为什么需要它：渲染状态重构之后，{@code HumanoidModel.setupAnim} 只拿得到
 * {@code HumanoidRenderState}，**拿不到实体**，没法再像 {@code origin/1.21.1} 那样直接问
 * {@code player.getFirstPassenger() instanceof EntityMaid}。所以在抽取渲染状态那一步把结论存进来，
 * 摆姿势那一步再读出去。</p>
 */
public interface ICarryMaidRenderState {
    boolean tlm$isCarryingMaid();

    void tlm$setCarryingMaid(boolean carryingMaid);
}
