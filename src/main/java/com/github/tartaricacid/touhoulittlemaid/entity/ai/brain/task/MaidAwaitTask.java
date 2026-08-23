package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class MaidAwaitTask extends Behavior<EntityMaid> {
    public MaidAwaitTask() {
        super(ImmutableMap.of());
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid maid, long gameTimeIn) {
        // 应战期间不得擦掉应战行为设置的 WALK_TARGET
        if (maid.isEmergencyCombatActive()) {
            return;
        }
        Brain<?> brain = maid.getBrain();
        if (brain.hasMemoryValue(MemoryModuleType.WALK_TARGET)) {
            boolean result = brain.getMemory(MemoryModuleType.WALK_TARGET)
                    .filter(walkTarget -> maid.isWithinHome(walkTarget.getTarget().currentBlockPosition()))
                    .isPresent();
            if (!result) {
                brain.eraseMemory(MemoryModuleType.PATH);
                brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            }
        }
    }
}
