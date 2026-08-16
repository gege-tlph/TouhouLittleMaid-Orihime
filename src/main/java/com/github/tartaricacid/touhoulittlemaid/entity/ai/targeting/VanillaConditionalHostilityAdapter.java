package com.github.tartaricacid.touhoulittlemaid.entity.ai.targeting;

import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.IMaidHostilityAdapter;
import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidHostilityDecision;
import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidTargetingContext;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.spider.Spider;

/**
 * Vanilla mobs whose hostility is represented by their actual attack target
 * rather than the persistent-anger contract exposed by {@code NeutralMob}.
 */
final class VanillaConditionalHostilityAdapter implements IMaidHostilityAdapter {
    @Override
    public MaidHostilityDecision evaluate(EntityMaid maid, LivingEntity target, MaidTargetingContext context) {
        if (!(target instanceof AbstractPiglin || target instanceof Spider
                || target instanceof Goat || target instanceof Llama)) {
            return MaidHostilityDecision.PASS;
        }

        LivingEntity actualTarget = ((Mob) target).getTarget();
        if (actualTarget == maid || actualTarget != null && maid.isOwnedBy(actualTarget)) {
            return MaidHostilityDecision.HOSTILE;
        }
        return MaidHostilityDecision.FRIENDLY;
    }
}
