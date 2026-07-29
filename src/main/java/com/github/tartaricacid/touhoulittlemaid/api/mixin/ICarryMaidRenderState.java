package com.github.tartaricacid.touhoulittlemaid.api.mixin;

/** Client render-state flag used to preserve the carried-maid player arm pose. */
public interface ICarryMaidRenderState {
    boolean tlm$isCarryingMaid();

    void tlm$setCarryingMaid(boolean carryingMaid);
}
