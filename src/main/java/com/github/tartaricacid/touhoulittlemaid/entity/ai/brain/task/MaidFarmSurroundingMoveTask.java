package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IFarmTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidPathFindingBFS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * 农田寻路的升级版本，能够检测周围的方块是否可达，而不是单点位置可达
 */
public class MaidFarmSurroundingMoveTask extends MaidFarmMoveTask {
    private final BoundingBox checkRange;

    public MaidFarmSurroundingMoveTask(IFarmTask task, BoundingBox checkRange, float movementSpeed) {
        super(task, movementSpeed);
        this.checkRange = checkRange;
    }

    public MaidFarmSurroundingMoveTask(IFarmTask task, float movementSpeed) {
        this(task, new BoundingBox(-1, 0, -1, 1, 1, 1), movementSpeed);
    }

    @Override
    protected boolean checkPathReach(EntityMaid maid, MaidPathFindingBFS pathFinding, BlockPos pos) {
        return findReachableWalkTarget(maid, pathFinding, pos, checkRange);
    }
}
