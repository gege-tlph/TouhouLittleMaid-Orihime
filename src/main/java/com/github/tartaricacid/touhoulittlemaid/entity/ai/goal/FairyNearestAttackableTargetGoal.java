package com.github.tartaricacid.touhoulittlemaid.entity.ai.goal;

import com.github.tartaricacid.touhoulittlemaid.datagen.tag.TagEntity;
import com.github.tartaricacid.touhoulittlemaid.entity.monster.EntityFairy;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;

import java.util.Comparator;
import java.util.List;

public class FairyNearestAttackableTargetGoal<T extends LivingEntity> extends TargetGoal {
    private static final int DEFAULT_RANDOM_INTERVAL = 5;
    private final TargetingConditions targetConditions;
    private @Nullable LivingEntity target;

    public FairyNearestAttackableTargetGoal(EntityFairy fairy) {
        super(fairy, true, false);
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        this.targetConditions = TargetingConditions.forCombat().range(this.getFollowDistance());
    }

    @Override
    public boolean canUse() {
        if (this.mob.getRandom().nextInt(DEFAULT_RANDOM_INTERVAL) != 0) {
            return false;
        } else {
            this.findTarget();
            return this.target != null;
        }
    }

    private AABB getTargetSearchArea(double distance) {
        return this.mob.getBoundingBox().inflate(distance, 4, distance);
    }

    private void findTarget() {

        if (this.mob.level instanceof ServerLevel serverLevel) {
            AABB searchArea = this.getTargetSearchArea(this.getFollowDistance());
            List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, searchArea,
                    e -> e.getType().is(TagEntity.MAID_FAIRY_ATTACK_GOAL) && this.targetConditions.test(serverLevel, this.mob, e));
            this.target = entities.stream().min(Comparator.comparingDouble(this.mob::distanceToSqr)).orElse(null);
        } else {
            this.target = null;
        }
    }

    @Override
    public void start() {
        this.mob.setTarget(this.target);
        super.start();
    }
}