package com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern.brain;

import com.github.tartaricacid.touhoulittlemaid.advancements.maid.TriggerType;
import com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern.seat.TavernSeatAdapter;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidCheckRateTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidPathFindingBFS;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.init.InitTrigger;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public final class MaidTavernSeatTask extends MaidCheckRateTask {
    private static final int MAX_SCAN_DELAY = 120;
    private final float speed;
    private final double closeEnoughDistance;
    @Nullable
    private BlockPos targetSeat;

    public MaidTavernSeatTask(float speed, double closeEnoughDistance) {
        super(ImmutableMap.of(
                MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                InitEntities.TARGET_POS, MemoryStatus.VALUE_ABSENT));
        this.speed = speed;
        this.closeEnoughDistance = closeEnoughDistance;
        this.setMaxCheckRate(MAX_SCAN_DELAY);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        if (!super.checkExtraStartConditions(level, maid) || !maid.canBrainMoving() || maid.isPassenger()) {
            return false;
        }
        targetSeat = findSeat(level, maid);
        if (targetSeat == null) {
            return false;
        }
        if (targetSeat.distToCenterSqr(maid.position()) <= closeEnoughDistance * closeEnoughDistance) {
            return true;
        }
        BehaviorUtils.setWalkAndLookTargetMemories(maid, targetSeat, speed, 1);
        this.setNextCheckTickCount(5);
        targetSeat = null;
        return false;
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        if (targetSeat != null && TavernSeatAdapter.sit(level, maid, targetSeat)
                && maid.getOwner() instanceof ServerPlayer serverPlayer) {
            InitTrigger.MAID_EVENT.trigger(serverPlayer, TriggerType.MAID_SIT_JOY);
        }
        targetSeat = null;
        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        maid.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Nullable
    private BlockPos findSeat(ServerLevel level, EntityMaid maid) {
        BlockPos center = maid.getBrainSearchPos();
        int range = (int) maid.getHomeRadius();
        double bestDistance = Double.MAX_VALUE;
        BlockPos best = null;
        MaidPathFindingBFS pathFinding = new MaidPathFindingBFS(maid.getNavigation().getNodeEvaluator(), level, maid);
        try {
            for (BlockPos mutablePos : BlockPos.betweenClosed(center.offset(-range, -2, -range), center.offset(range, 2, range))) {
                BlockPos pos = mutablePos.immutable();
                BlockState state = level.getBlockState(pos);
                if (!maid.isWithinHome(pos) || !ownerAllows(maid, pos)
                        || !TavernSeatAdapter.isAvailable(level, pos, state)
                        || !hasReachableSide(pathFinding, pos)) {
                    continue;
                }
                double distance = pos.distToCenterSqr(maid.position());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = pos;
                }
            }
        } finally {
            pathFinding.finish();
        }
        return best;
    }

    private static boolean hasReachableSide(MaidPathFindingBFS pathFinding, BlockPos seatPos) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (pathFinding.canPathReach(seatPos.relative(direction))) {
                return true;
            }
        }
        return false;
    }

    private static boolean ownerAllows(EntityMaid maid, BlockPos pos) {
        if (maid.isHomeModeEnable()) {
            return true;
        }
        return maid.getOwner() != null && pos.closerToCenterThan(maid.getOwner().position(), 8.0D);
    }
}
