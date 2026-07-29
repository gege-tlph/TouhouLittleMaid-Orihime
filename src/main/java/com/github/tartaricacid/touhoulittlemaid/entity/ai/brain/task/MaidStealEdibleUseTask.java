package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import cn.sh1rocu.touhoulittlemaid.util.itemhandler.CombinedInvWrapper;
import com.github.tartaricacid.touhoulittlemaid.api.block.IMaidEdibleBlock;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.edible.MaidEdibleBlockAction;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.edible.MaidEdibleBlockManager;
import com.github.tartaricacid.touhoulittlemaid.entity.favorability.Type;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class MaidStealEdibleUseTask extends Behavior<EntityMaid> {
    private static final int MIN_FAVORABILITY_POINTS = 1;
    private static final int MAX_FAVORABILITY_POINTS = 3;

    private final double closeEnoughDist;

    /**
     * 女仆此刻是否允许偷吃已摆放的食物。开关关闭时不找也不吃；成功偷吃后共享同一个
     * {@link Type#STEAL_EDIBLE_BLOCK} 冷却，冷却期间不再消耗桌上食物。该冷却由
     * {@code FavorabilityManagerCounter} 随实体 NBT 持久化，重进世界或专服重启都不会重置。
     * 摆盘不读取本判据。
     */
    static boolean canSteal(EntityMaid maid) {
        return maid.getConfigManager().isTableFoodAllowed()
                && maid.getFavorabilityManager().canAdd(Type.STEAL_EDIBLE_BLOCK);
    }

    /** 单次成功偷吃的好感度奖励，恒为 {@code [1, 3]} 的均匀随机值。 */
    static int rollFavorabilityPoints(RandomSource random) {
        return MIN_FAVORABILITY_POINTS
                + random.nextInt(MAX_FAVORABILITY_POINTS - MIN_FAVORABILITY_POINTS + 1);
    }

    public MaidStealEdibleUseTask(double closeEnoughDist) {
        super(ImmutableMap.of(
                InitEntities.TARGET_POS, MemoryStatus.VALUE_PRESENT,
                InitEntities.MAID_EDIBLE_BLOCK_ACTION, MemoryStatus.VALUE_PRESENT
        ));
        this.closeEnoughDist = closeEnoughDist;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, EntityMaid owner) {
        Brain<EntityMaid> brain = owner.getBrain();
        // 持有超时：把目标让给优先级更高的工作行为。本判据每 tick 都会被调用（TARGET_POS 与
        // ACTION 都在时，本 Behavior 每 tick 都会被尝试启动），所以不需要额外的 ticking 结构。
        if (isTargetHoldExpired(worldIn, brain)) {
            releaseTargetHold(brain);
            return false;
        }
        MaidEdibleBlockAction action = brain.getMemory(InitEntities.MAID_EDIBLE_BLOCK_ACTION).orElse(null);
        if (action == MaidEdibleBlockAction.TRY_STEAL && !canSteal(owner)) {
            return false;
        }
        return brain.getMemory(InitEntities.TARGET_POS).map(targetPos -> {
            Vec3 targetV3d = targetPos.currentPosition();
            if (owner.distanceToSqr(targetV3d) > Math.pow(closeEnoughDist, 2)) {
                Optional<WalkTarget> walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET);
                if (walkTarget.isEmpty()
                        || walkTarget.get().getTarget().currentPosition().distanceToSqr(targetV3d)
                        > Math.pow(closeEnoughDist, 2)) {
                    brain.eraseMemory(InitEntities.TARGET_POS);
                    brain.eraseMemory(InitEntities.MAID_EDIBLE_HOLD_EXPIRY);
                }
                return false;
            }
            return true;
        }).orElse(false);
    }

    private static boolean isTargetHoldExpired(ServerLevel worldIn, Brain<EntityMaid> brain) {
        return brain.getMemory(InitEntities.MAID_EDIBLE_HOLD_EXPIRY)
                .map(expiry -> worldIn.getGameTime() >= expiry)
                .orElse(false);
    }

    /**
     * 释放偷吃/摆盘对目标的持有。
     *
     * <p>{@code WALK_TARGET} 必须一并清除：工作行为同样要求它为空，只清 {@code TARGET_POS}
     * 的话女仆会继续走向食物，工作依旧启动不了，等于没让位。</p>
     */
    private static void releaseTargetHold(Brain<EntityMaid> brain) {
        brain.eraseMemory(InitEntities.TARGET_POS);
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(InitEntities.MAID_EDIBLE_HOLD_EXPIRY);
    }

    @Override
    protected void start(ServerLevel world, EntityMaid maid, long gameTimeIn) {
        Brain<EntityMaid> brain = maid.getBrain();
        brain.getMemory(InitEntities.TARGET_POS)
                .ifPresent(posWrapper -> brain.getMemory(InitEntities.MAID_EDIBLE_BLOCK_ACTION)
                        .ifPresent(action -> handle(world, maid, posWrapper, action)));
    }

    private void handle(ServerLevel world, EntityMaid maid, PositionTracker posWrapper, MaidEdibleBlockAction action) {
        BlockPos blockPos = posWrapper.currentBlockPosition();
        BlockState blockState = world.getBlockState(blockPos);

        if (action == MaidEdibleBlockAction.TRY_STEAL) {
            for (IMaidEdibleBlock edibleBlock : MaidEdibleBlockManager.getEdibleBlocks()) {
                // 再进行一次方块确认
                if (edibleBlock.shouldMoveTo(maid, blockPos, blockState)) {
                    boolean result = edibleBlock.consume(maid, blockPos, blockState);
                    if (result) {
                        // 成功偷吃必定给 1~3 点好感度，与食物的饱食度、稀有度、价格、种类和剩余份数无关，
                        // 避免出现「用昂贵料理刷好感度」的最优解。上游改为按 IMaidEdibleBlock 固定给 1 点，
                        // 且三分钟内后续偷吃照吃不给奖励；这里是用户确认的玩法修正。
                        // apply 同时写入 STEAL_EDIBLE_BLOCK 冷却，因此它既是好感度冷却也是偷吃冷却。
                        maid.getFavorabilityManager()
                                .apply(Type.STEAL_EDIBLE_BLOCK, rollFavorabilityPoints(maid.getRandom()));
                        maid.swing(InteractionHand.MAIN_HAND);
                    }
                    releaseTargetHold(maid.getBrain());
                    return;
                }
            }
        } else {
            CombinedInvWrapper inv = maid.getAvailableInv(true);
            for (int i = 0; i < inv.getSlots(); i++) {
                ItemStack stack = inv.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                for (IMaidEdibleBlock edibleBlock : MaidEdibleBlockManager.getEdibleBlocks()) {
                    // 再进行一次方块确认
                    if (edibleBlock.canPlaceAsFood(maid, stack, i)) {
                        boolean result = edibleBlock.placeAsFood(maid, blockPos, stack, i);
                        if (result) {
                            maid.swing(InteractionHand.MAIN_HAND);
                        }
                        maid.getBrain().eraseMemory(InitEntities.TARGET_POS);
                        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                        return;
                    }
                }
            }
        }
    }
}
