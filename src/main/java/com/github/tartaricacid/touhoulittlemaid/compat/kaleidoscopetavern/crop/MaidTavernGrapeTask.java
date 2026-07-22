package com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern.crop;

import com.github.tartaricacid.touhoulittlemaid.advancements.maid.TriggerType;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidCheckRateTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidPathFindingBFS;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskNormalFarm;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.init.InitTrigger;
import com.github.ysbbbbbb.kaleidoscopetavern.block.plant.GrapeCropBlock;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class MaidTavernGrapeTask extends MaidCheckRateTask {
    private static final int MAX_SCAN_DELAY = 120;
    private static final int CONTINUOUS_SCAN_DELAY = 5;
    private final float speed;
    private final double closeEnoughDistance;
    @Nullable
    private Target target;

    public MaidTavernGrapeTask(float speed, double closeEnoughDistance) {
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                InitEntities.TARGET_POS, MemoryStatus.VALUE_ABSENT));
        this.speed = speed;
        this.closeEnoughDistance = closeEnoughDistance;
        this.setMaxCheckRate(MAX_SCAN_DELAY);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        if (!super.checkExtraStartConditions(level, maid)
                || !(maid.getTask() instanceof TaskNormalFarm)
                || !maid.canBrainMoving()) {
            return false;
        }
        target = findGrape(level, maid);
        if (target == null) {
            return false;
        }
        if (target.cropPos.distToCenterSqr(maid.position()) <= closeEnoughDistance * closeEnoughDistance) {
            return true;
        }
        BehaviorUtils.setWalkAndLookTargetMemories(maid, target.approachPos, speed, 0);
        this.setNextCheckTickCount(CONTINUOUS_SCAN_DELAY);
        target = null;
        return false;
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        if (target != null) {
            BlockState state = level.getBlockState(target.cropPos);
            if (maid.canDestroyBlock(target.cropPos)
                    && state.getBlock() instanceof GrapeCropBlock crop
                    && crop.isMaxAge(state)
                    && maid.destroyBlock(target.cropPos)) {
                maid.swing(InteractionHand.MAIN_HAND);
                if (maid.getOwner() instanceof ServerPlayer serverPlayer) {
                    InitTrigger.MAID_EVENT.trigger(serverPlayer, TriggerType.MAID_FARM);
                }
                // 匹配正常的耕作任务：生产性工作结束后，几乎立即寻找附近的下一种作物，而不是等待 6-12 秒。
                this.setNextCheckTickCount(CONTINUOUS_SCAN_DELAY);
            }
        }
        target = null;
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Nullable
    private Target findGrape(ServerLevel level, EntityMaid maid) {
        BlockPos center = maid.getBrainSearchPos();
        int range = (int) maid.getHomeRadius();
        double bestDistance = Double.MAX_VALUE;
        Target best = null;
        MaidPathFindingBFS pathFinding = new MaidPathFindingBFS(maid.getNavigation().getNodeEvaluator(), level, maid);
        try {
            for (BlockPos mutablePos : BlockPos.betweenClosed(center.offset(-range, -2, -range), center.offset(range, 3, range))) {
                BlockPos cropPos = mutablePos.immutable();
                BlockState state = level.getBlockState(cropPos);
                if (!maid.isWithinHome(cropPos)
                        || !(state.getBlock() instanceof GrapeCropBlock crop)
                        || !crop.isMaxAge(state)) {
                    continue;
                }
                BlockPos approachPos = findApproach(pathFinding, cropPos);
                if (approachPos == null) {
                    continue;
                }
                double distance = cropPos.distToCenterSqr(maid.position());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = new Target(cropPos, approachPos);
                }
            }
        } finally {
            pathFinding.finish();
        }
        return best;
    }

    @Nullable
    private static BlockPos findApproach(MaidPathFindingBFS pathFinding, BlockPos cropPos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos sameLevel = cropPos.relative(direction);
            if (pathFinding.canPathReach(sameLevel)) {
                return sameLevel;
            }
            BlockPos oneBelow = sameLevel.below();
            if (pathFinding.canPathReach(oneBelow)) {
                return oneBelow;
            }
        }
        return null;
    }

    private record Target(BlockPos cropPos, BlockPos approachPos) {
    }
}
