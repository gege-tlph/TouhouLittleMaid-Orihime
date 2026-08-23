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
        // 三种葡萄变体共用这个公开类型。它们对玩家和其它生物照旧可攀爬，
        // 只是女仆的寻路不再把密集的葡萄架当成垂直捷径。
        return !(state.getBlock() instanceof GrapevineTrellisBlock);
    }
}
