package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import cn.sh1rocu.touhoulittlemaid.util.itemhandler.CombinedInvWrapper;
import com.github.tartaricacid.touhoulittlemaid.advancements.maid.TriggerType;
import com.github.tartaricacid.touhoulittlemaid.api.task.IFarmTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.init.InitTrigger;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

public class MaidFarmPlantTask extends Behavior<EntityMaid> {
    private final IFarmTask task;

    public MaidFarmPlantTask(IFarmTask task) {
        super(ImmutableMap.of(InitEntities.TARGET_POS, MemoryStatus.VALUE_PRESENT));
        this.task = task;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, EntityMaid owner) {
        Brain<EntityMaid> brain = owner.getBrain();
        return brain.getMemory(InitEntities.TARGET_POS).map(targetPos -> {
            BlockPos interactionBasePos = targetPos.currentBlockPosition();
            if (!isCloseEnoughToInteract(owner, interactionBasePos)) {
                Optional<WalkTarget> walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET);
                if (walkTarget.isEmpty() || !MaidFarmMoveTask.isWithinInteractionRange(
                        walkTarget.get().getTarget().currentBlockPosition(), interactionBasePos,
                        task.getCloseEnoughDist())) {
                    brain.eraseMemory(InitEntities.TARGET_POS);
                }
                return false;
            }
            return true;
        }).orElse(false);
    }

    /**
     * Harvest as soon as the maid is actually near the crop. The move task parks the maid on a
     * reachable node BESIDE the crop (on 1.21.11 it often cannot path into the crop column), so
     * gate on the maid's real position with a tolerance that covers an adjacent stand node. This
     * matches the origin/1.21.1 baseline, which harvested from within the crop column via a
     * forgiving Vec3 distance check. The prior integer block-coordinate check was too strict: the
     * maid frequently settles one block off the exact stand node and would never satisfy it, so it
     * kept re-pathing to the crop without ever harvesting (only occasionally landing exactly right).
     */
    private boolean isCloseEnoughToInteract(EntityMaid owner, BlockPos interactionBasePos) {
        double reach = task.getCloseEnoughDist() + 1.0D;
        return owner.distanceToSqr(Vec3.atCenterOf(interactionBasePos.above())) <= reach * reach;
    }

    @Override
    protected void start(ServerLevel world, EntityMaid maid, long gameTimeIn) {
        maid.getBrain().getMemory(InitEntities.TARGET_POS).ifPresent(posWrapper -> {
            BlockPos basePos = posWrapper.currentBlockPosition();
            BlockPos cropPos = basePos.above();
            BlockState cropState = world.getBlockState(cropPos);
            if (maid.canDestroyBlock(cropPos) && task.canHarvest(maid, cropPos, cropState)) {
                task.harvest(maid, cropPos, cropState);
                maid.swing(InteractionHand.MAIN_HAND);
                maid.getBrain().eraseMemory(InitEntities.TARGET_POS);
                maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                if (maid.getOwner() instanceof ServerPlayer serverPlayer) {
                    InitTrigger.MAID_EVENT.trigger(serverPlayer, TriggerType.MAID_FARM);
                }
            }

            CombinedInvWrapper availableInv = maid.getAvailableInv(true);
            List<Integer> slots = ItemsUtil.getFilterStackSlots(availableInv, task::isSeed);
            if (!slots.isEmpty()) {
                for (int slot : slots) {
                    ItemStack seed = availableInv.getStackInSlot(slot);
                    BlockState baseState = world.getBlockState(basePos);
                    if (task.canPlant(maid, basePos, baseState, seed)) {
                        ItemStack remain = task.plant(maid, basePos, baseState, seed);
                        availableInv.setStackInSlot(slot, remain);
                        maid.swing(InteractionHand.MAIN_HAND);
                        maid.getBrain().eraseMemory(InitEntities.TARGET_POS);
                        maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                        if (maid.getOwner() instanceof ServerPlayer serverPlayer) {
                            InitTrigger.MAID_EVENT.trigger(serverPlayer, TriggerType.MAID_FARM);
                        }
                        return;
                    }
                }
            }
        });
    }
}
