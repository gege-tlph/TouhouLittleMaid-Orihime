package com.github.tartaricacid.touhoulittlemaid.api.entity.targeting;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.LivingEntity;

/**
 * Optional hostility bridge for conditional vanilla behavior and third-party
 * entities. Implementations must not hard-link optional mod classes unless the
 * owning mod is present.
 */
@FunctionalInterface
public interface IMaidHostilityAdapter {
    MaidHostilityDecision evaluate(EntityMaid maid, LivingEntity target, MaidTargetingContext context);
}
