package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import cn.sh1rocu.touhoulittlemaid.util.itemhandler.CombinedInvWrapper;
import com.github.tartaricacid.touhoulittlemaid.api.block.IMaidEdibleBlock;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.edible.MaidEdibleBlockAction;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.edible.MaidEdibleBlockManager;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidPathFindingBFS;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import javax.annotation.Nullable;
import java.util.Optional;

public class MaidStealEdibleMoveBlockTask extends MaidMoveToBlockTask {
    /**
     * 女仆偷吃方块的水平搜索范围比较小
     */
    private static final int HORIZONTAL_SEARCH_RANGE = 5;
    /**
     * 当搜索成功后，女仆下一次偷吃的检查间隔
     */
    private static final int NEXT_CHECK_TICK_COUNT = 45 * 20;
    /**
     * 检查方块可达性的范围，默认检查寻路点周围 3x3x3 范围内的方块的可达性
     */
    private static final BoundingBox CHECK_RANGE = new BoundingBox(-1, -1, -1, 1, 1, 1);
    /**
     * 仅供开发调试，用来缩短女仆偷吃方块的冷却时间
     */
    @VisibleForDebug
    public static boolean DEBUG = false;
    /**
     * 偷吃/摆盘最多持有 {@link InitEntities#TARGET_POS} 的时长。
     *
     * <p>偷吃两个 Behavior 注册在优先级 8，而工作行为在 5~6 且同样要求 {@code TARGET_POS} 为空——
     * 一旦偷吃先拿到该内存，工作行为连判据都不会被调用，无法在工作侧抢占。这里给持有加上时限：
     * 超时即释放，让工作行为在下一 tick 按优先级正常取得目标。</p>
     *
     * <p>水平搜索范围只有 {@value #HORIZONTAL_SEARCH_RANGE} 格，正常走到食物远快于该时限，
     * 所以它只会截断「走不到 / 被卡住」这类本就该放弃的持有，不会打断正常偷吃。
     * 与智能应战参数同属待实机手感冻结的暂定值。</p>
     */
    private static final int MAX_TARGET_HOLD_TICKS = 200;

    private final MemoryModuleType<MaidEdibleBlockAction> action;

    private @Nullable ItemStack placedStack;
    private @Nullable BlockPos reachableWalkTarget;

    public MaidStealEdibleMoveBlockTask(float movementSpeed) {
        super(movementSpeed, 2);
        this.setMaxCheckRate(NEXT_CHECK_TICK_COUNT);
        this.action = InitEntities.MAID_EDIBLE_BLOCK_ACTION;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, EntityMaid owner) {
        if (DEBUG) {
            return true;
        }
        return super.checkExtraStartConditions(worldIn, owner);
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid maid, long gameTimeIn) {
        Optional<MaidEdibleBlockAction> memory = maid.getBrain().getMemory(this.action);

        if (memory.isPresent() && memory.get() == MaidEdibleBlockAction.TRY_STEAL) {
            // 检查背包内有可放置食物么，有就切放置状态
            CombinedInvWrapper inv = maid.getAvailableInv(true);
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                for (IMaidEdibleBlock edibleBlock : MaidEdibleBlockManager.getEdibleBlocks()) {
                    if (edibleBlock.canPlaceAsFood(maid, stack, i)) {
                        this.placedStack = stack;
                        maid.getBrain().setMemory(this.action, MaidEdibleBlockAction.TRY_PLACE);
                        this.searchForDestination(worldIn, maid);
                        armTargetHold(worldIn, maid);
                        return;
                    }
                }
            }

            // 如果没有，那么切偷吃状态
            maid.getBrain().setMemory(this.action, MaidEdibleBlockAction.TRY_STEAL);
        } else {
            // 初始状态为偷吃
            maid.getBrain().setMemory(this.action, MaidEdibleBlockAction.TRY_STEAL);
        }

        // 摆盘和偷吃是两套玩家可感知的行为。关闭偷吃或仍处于偷吃冷却时，仍允许上面的
        // 背包食物扫描与摆盘，只跳过对世界中现成食物的搜索。
        if (!MaidStealEdibleUseTask.canSteal(maid)) {
            return;
        }

        // 尝试搜索目标位置
        this.searchForDestination(worldIn, maid);
        armTargetHold(worldIn, maid);
    }

    /**
     * 搜索成功时给本次持有打上到期时刻；搜索失败则什么也不做。
     *
     * <p>每次取得目标都重新计时，因此不存在「上一轮遗留的到期时刻立刻掐掉这一轮」的问题。</p>
     */
    private static void armTargetHold(ServerLevel worldIn, EntityMaid maid) {
        if (maid.getBrain().hasMemoryValue(InitEntities.TARGET_POS)) {
            maid.getBrain().setMemory(InitEntities.MAID_EDIBLE_HOLD_EXPIRY,
                    worldIn.getGameTime() + MAX_TARGET_HOLD_TICKS);
        }
    }

    @Override
    protected boolean shouldMoveTo(ServerLevel worldIn, EntityMaid maid, BlockPos pos) {
        BlockState blockState = worldIn.getBlockState(pos);
        MaidEdibleBlockAction memory = maid.getBrain().getMemory(this.action).orElse(MaidEdibleBlockAction.TRY_STEAL);
        for (IMaidEdibleBlock edibleBlock : MaidEdibleBlockManager.getEdibleBlocks()) {
            if (memory == MaidEdibleBlockAction.TRY_PLACE) {
                if (this.placedStack == null) {
                    return false;
                }
                if (edibleBlock.shouldPlaceTo(maid, pos, blockState, this.placedStack)) {
                    return true;
                }
            } else {
                if (edibleBlock.shouldMoveTo(maid, pos, blockState)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected boolean checkPathReach(EntityMaid maid, MaidPathFindingBFS pathFinding, BlockPos pos) {
        this.reachableWalkTarget = null;
        double nearestDistance = Double.MAX_VALUE;
        for (int x = CHECK_RANGE.minX(); x <= CHECK_RANGE.maxX(); x++) {
            for (int y = CHECK_RANGE.minY(); y <= CHECK_RANGE.maxY(); y++) {
                for (int z = CHECK_RANGE.minZ(); z <= CHECK_RANGE.maxZ(); z++) {
                    BlockPos candidate = pos.offset(x, y, z);
                    if (pathFinding.canPathReach(candidate)) {
                        double distance = candidate.distSqr(maid.blockPosition());
                        if (distance < nearestDistance) {
                            nearestDistance = distance;
                            this.reachableWalkTarget = candidate;
                        }
                    }
                }
            }
        }
        return this.reachableWalkTarget != null;
    }

    @Override
    protected int getHorizontalSearchRange(EntityMaid maid) {
        int defaultRange = super.getHorizontalSearchRange(maid);
        return Math.min(defaultRange, HORIZONTAL_SEARCH_RANGE);
    }

    @Override
    protected BlockPos getWalkTargetPos(EntityMaid maid, BlockPos targetPos) {
        return this.reachableWalkTarget == null ? targetPos : this.reachableWalkTarget;
    }

    @Override
    protected void setNextCheckTickCount(int nextCheckTickCount) {
        super.setNextCheckTickCount(NEXT_CHECK_TICK_COUNT);
    }
}
