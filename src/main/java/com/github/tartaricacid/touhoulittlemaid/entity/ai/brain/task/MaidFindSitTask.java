package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.vehicle.boat.Boat;

public class MaidFindSitTask extends MaidCheckRateTask {
    private static final int MAX_DELAY_TIME = 12;
    private final float speedModifier;
    private Entity sitEntity = null;
    private long chatBubbleKey = -1;

    public MaidFindSitTask(float speedModifier) {
        super(ImmutableMap.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
        this.speedModifier = speedModifier;
        this.setMaxCheckRate(MAX_DELAY_TIME);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel world, EntityMaid maid) {
        return super.checkExtraStartConditions(world, maid) && maid.getVehicle() == null;
    }

    @Override
    protected void start(ServerLevel world, EntityMaid maid, long gameTime) {
        this.sitEntity = null;
        this.getEntities(maid).find(e -> filterEntity(maid, e)).findFirst().ifPresentOrElse(entity -> {
            this.sitEntity = entity;
            BehaviorUtils.setWalkAndLookTargetMemories(maid, this.sitEntity, this.speedModifier, 0);
        }, () -> {
            // 聊天气泡反馈通过聊天气泡子系统恢复；任务行为不受影响。
        });

        if (sitEntity != null && sitEntity.isAlive() && sitEntity.closerThan(maid, 2)) {
            if (sitEntity.getPassengers().isEmpty()) {
                maid.startRiding(this.sitEntity, true, true);
            }
            this.sitEntity = null;
        }
    }

    private boolean filterEntity(EntityMaid maid, Entity entity) {
        if (!entity.isAlive() || !maid.isWithinHome(entity.blockPosition()) || !entity.getPassengers().isEmpty()) {
            return false;
        }

        return entity instanceof EntityChair || entity instanceof Boat;
    }

    private NearestVisibleLivingEntities getEntities(EntityMaid maid) {
        return maid.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
                .orElse(NearestVisibleLivingEntities.empty());
    }
}
