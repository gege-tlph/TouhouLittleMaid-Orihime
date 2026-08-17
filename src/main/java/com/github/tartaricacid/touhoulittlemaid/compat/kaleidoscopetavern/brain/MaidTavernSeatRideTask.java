package com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern.brain;

import com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern.seat.TavernSeatAdapter;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidCheckRateTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;

public final class MaidTavernSeatRideTask extends MaidCheckRateTask {
    private final boolean shouldDismount;

    private MaidTavernSeatRideTask(boolean shouldDismount) {
        super(ImmutableMap.of(), shouldDismount ? DEFAULT_DURATION : Integer.MAX_VALUE);
        this.shouldDismount = shouldDismount;
        this.setMaxCheckRate(1);
    }

    public static MaidTavernSeatRideTask reconcile() {
        return new MaidTavernSeatRideTask(false);
    }

    public static MaidTavernSeatRideTask dismount() {
        return new MaidTavernSeatRideTask(true);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        return super.checkExtraStartConditions(level, maid) && TavernSeatAdapter.isTavernSeat(maid.getVehicle());
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        if (shouldDismount) {
            maid.stopRiding();
        } else {
            TavernSeatAdapter.reconcile(maid);
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        return !shouldDismount && TavernSeatAdapter.isTavernSeat(maid.getVehicle());
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        if (!shouldDismount) {
            TavernSeatAdapter.reconcile(maid);
        }
    }
}
