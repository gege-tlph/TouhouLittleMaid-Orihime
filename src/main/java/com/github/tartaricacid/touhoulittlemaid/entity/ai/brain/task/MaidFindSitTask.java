package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityChair;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
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
    protected boolean checkExtraStartConditions(ServerLevel worldIn, EntityMaid owner) {
        return super.checkExtraStartConditions(worldIn, owner) && owner.getVehicle() == null;
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid maid, long gameTimeIn) {
        this.sitEntity = null;
        this.getEntities(maid)
                .find(e -> filterEntity(maid, e))
                .findFirst()
                .ifPresentOrElse(entity -> {
                    this.sitEntity = entity;
                    BehaviorUtils.setWalkAndLookTargetMemories(maid, this.sitEntity, this.speedModifier, 0);
                }, () -> {
                    String langKey = "chat_bubble.touhou_little_maid.inner.fishing.no_sit";
                    this.chatBubbleKey = maid.getChatBubbleManager().addTextChatBubbleIfTimeout(langKey, this.chatBubbleKey);
                    walkTowardsHomeIfStuck(maid);
                });

        if (sitEntity != null && sitEntity.isAlive() && sitEntity.closerThan(maid, 2)) {
            if (sitEntity.getPassengers().isEmpty()) {
                maid.startRiding(this.sitEntity, true, true);
            }
            this.sitEntity = null;
        }
    }

    /**
     * home 判定是三维球，而坐具搜索要求实体可见：休息区叠在工作区正上方时，
     * 女仆「在范围内」却永远看不见隔层的坐具，原地死锁（三树逐字同型的继承缺陷）。
     * 找不到可见坐具时朝工作中心走；视线打开后上方的 ifPresent 分支自然接管。
     * 到中心 2 格内不再走，保证收敛。
     */
    private void walkTowardsHomeIfStuck(EntityMaid maid) {
        if (!maid.hasHome()) {
            return;
        }
        BlockPos homePos = maid.getHomePosition();
        if (maid.blockPosition().closerThan(homePos, 2)) {
            return;
        }
        BehaviorUtils.setWalkAndLookTargetMemories(maid, homePos, this.speedModifier, 2);
    }

    private boolean filterEntity(EntityMaid maid, Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        if (!maid.isWithinHome(entity.blockPosition())) {
            return false;
        }
        if (!entity.getPassengers().isEmpty()) {
            return false;
        }
        return entity instanceof EntityChair || entity instanceof Boat;
    }

    private NearestVisibleLivingEntities getEntities(EntityMaid maid) {
        return maid.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).orElse(NearestVisibleLivingEntities.empty());
    }
}