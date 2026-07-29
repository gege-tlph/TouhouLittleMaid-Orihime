package com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern.brain;

import com.github.tartaricacid.touhoulittlemaid.api.entity.ai.IExtraMaidBrain;
import com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern.crop.MaidTavernGrapeTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.ysbbbbbb.kaleidoscopetavern.block.plant.GrapevineTrellisBlock;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class TavernMaidBrain implements IExtraMaidBrain {
    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getIdleBehaviors() {
        return List.of(Pair.of(7, new MaidTavernSeatTask(0.6F, 2.0)));
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getRideIdleBehaviors() {
        return List.of(Pair.of(0, MaidTavernSeatRideTask.reconcile()));
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getRideWorkBehaviors() {
        return List.of(Pair.of(0, MaidTavernSeatRideTask.dismount()));
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getRideRestBehaviors() {
        return List.of(Pair.of(0, MaidTavernSeatRideTask.dismount()));
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> getWorkBehaviors() {
        return List.of(Pair.of(4, new MaidTavernGrapeTask(0.6F, 3.0)));
    }

    @Override
    public boolean canClimbBlock(EntityMaid maid, BlockPos pos, BlockState state) {
        // All three cultivated grape variants use this public upstream type.
        // They remain climbable for players and other mobs; only maid path
        // planning avoids using dense crop trellises as vertical shortcuts.
        return !(state.getBlock() instanceof GrapevineTrellisBlock);
    }
}
