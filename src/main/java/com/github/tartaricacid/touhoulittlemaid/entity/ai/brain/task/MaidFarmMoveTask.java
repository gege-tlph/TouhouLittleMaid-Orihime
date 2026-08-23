package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import cn.sh1rocu.touhoulittlemaid.util.transfer.CombinedResourceHandler;
import com.github.tartaricacid.touhoulittlemaid.api.task.IFarmTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidPathFindingBFS;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.Nullable;

public class MaidFarmMoveTask extends MaidMoveToBlockTask {
    private static final BoundingBox DEFAULT_STAND_SEARCH_RANGE = new BoundingBox(-1, 0, -1, 1, 1, 1);

    private final NonNullList<ItemStack> seeds = NonNullList.create();
    private final IFarmTask task;
    private @Nullable BlockPos reachableWalkTarget;

    public MaidFarmMoveTask(IFarmTask task, float movementSpeed) {
        super(movementSpeed, 2);
        this.task = task;
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        seeds.clear();
        CombinedResourceHandler<ItemVariant> inv = entityIn.getAvailableInv(true);
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getResource(i).toStack();
            if (task.isSeed(stack)) {
                seeds.add(stack);
            }
        }
        this.searchForDestination(worldIn, entityIn);
    }

    @Override
    protected boolean shouldMoveTo(ServerLevel worldIn, EntityMaid maid, BlockPos basePos) {
        if (task.checkCropPosAbove()) {
            BlockPos above2Pos = basePos.above(2);
            BlockState stateUp2 = worldIn.getBlockState(above2Pos);
            if (!stateUp2.getCollisionShape(worldIn, above2Pos).isEmpty()) {
                return false;
            }
        }

        BlockPos cropPos = basePos.above();
        BlockState cropState = worldIn.getBlockState(cropPos);
        if (task.canHarvest(maid, cropPos, cropState)) {
            return true;
        }

        BlockState baseState = worldIn.getBlockState(basePos);
        return seeds.stream().anyMatch(seed -> task.canPlant(maid, basePos, baseState, seed));
    }

    @Override
    protected boolean checkPathReach(EntityMaid maid, MaidPathFindingBFS pathFinding, BlockPos pos) {
        return findReachableWalkTarget(maid, pathFinding, pos, DEFAULT_STAND_SEARCH_RANGE);
    }

    protected final boolean findReachableWalkTarget(EntityMaid maid, MaidPathFindingBFS pathFinding,
                                                    BlockPos interactionBasePos, BoundingBox checkRange) {
        this.reachableWalkTarget = null;
        double nearestDistance = Double.MAX_VALUE;
        BlockPos interactionPos = interactionBasePos.above();
        double interactionDistanceSqr = task.getCloseEnoughDist() * task.getCloseEnoughDist();

        for (int x = checkRange.minX(); x <= checkRange.maxX(); x++) {
            for (int y = checkRange.minY(); y <= checkRange.maxY(); y++) {
                for (int z = checkRange.minZ(); z <= checkRange.maxZ(); z++) {
                    if (x == 0 && z == 0) {
                        continue;
                    }
                    BlockPos candidate = interactionBasePos.offset(x, y, z);
                    if (!maid.isWithinHome(candidate)
                        || candidate.distSqr(interactionPos) > interactionDistanceSqr
                        || !pathFinding.canPathReachExact(candidate)) {
                        continue;
                    }
                    double distance = candidate.distSqr(maid.blockPosition());
                    if (distance < nearestDistance) {
                        nearestDistance = distance;
                        this.reachableWalkTarget = candidate;
                    }
                }
            }
        }
        return this.reachableWalkTarget != null;
    }

    @Override
    protected BlockPos getWalkTargetPos(EntityMaid maid, BlockPos targetPos) {
        return this.reachableWalkTarget == null ? targetPos : this.reachableWalkTarget;
    }

    static boolean isWithinInteractionRange(BlockPos entityPos, BlockPos interactionBasePos, double closeEnoughDist) {
        return entityPos.distSqr(interactionBasePos.above()) <= closeEnoughDist * closeEnoughDist;
    }
}
