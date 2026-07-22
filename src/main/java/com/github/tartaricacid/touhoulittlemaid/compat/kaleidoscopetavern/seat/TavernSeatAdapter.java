package com.github.tartaricacid.touhoulittlemaid.compat.kaleidoscopetavern.seat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.ysbbbbbb.kaleidoscopetavern.api.entity.ISittable;
import com.github.ysbbbbbb.kaleidoscopetavern.block.deco.SofaBlock;
import com.github.ysbbbbbb.kaleidoscopetavern.entity.SitEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.init.ModEntities;
import com.github.ysbbbbbb.kaleidoscopetavern.util.SitUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class TavernSeatAdapter {
    private TavernSeatAdapter() {
    }

    public static boolean isAvailable(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ISittable)) {
            return false;
        }
        SitEntity occupied = SitUtil.getSitEntity(level, pos);
        if (occupied != null && !occupied.isAlive()) {
            SitUtil.removeSitEntity(level, pos);
            occupied = null;
        }
        return occupied == null;
    }

    public static boolean sit(ServerLevel level, EntityMaid maid, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ISittable sittable) || !isAvailable(level, pos, state)) {
            return false;
        }

        SitEntity seat = ModEntities.SIT.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (seat == null) {
            return false;
        }
        seat.absSnapTo(pos.getX() + 0.5D, pos.getY() + sittable.getSitHeight(), pos.getZ() + 0.5D);
        alignToFurniture(maid, seat, state);
        if (!SitUtil.addSitEntity(level, pos, seat, maid.position())) {
            return false;
        }
        if (!level.addFreshEntity(seat)) {
            SitUtil.removeSitEntity(level, pos);
            return false;
        }

        if (!maid.startRiding(seat, true, true)) {
            seat.discard();
            return false;
        }
        return true;
    }

    public static boolean isTavernSeat(Entity vehicle) {
        return vehicle instanceof SitEntity;
    }

    public static void reconcile(EntityMaid maid) {
        if (!(maid.getVehicle() instanceof SitEntity seat)) {
            return;
        }
        BlockPos pos = seat.blockPosition();
        if (!(maid.level().getBlockState(pos).getBlock() instanceof ISittable)) {
            maid.stopRiding();
            seat.discard();
            return;
        }
        if (SitUtil.getSitEntity(maid.level(), pos) == null) {
            SitUtil.addSitEntity(maid.level(), pos, seat, maid.position());
        }
        alignToFurniture(maid, seat, maid.level().getBlockState(pos));
    }

    private static void alignToFurniture(EntityMaid maid, SitEntity seat, BlockState state) {
        // 沙发的正面是固定的。酒馆自己的玩家交互保留了玩家的视线方向，但AI选择的座位没有可继承的点击方向，因此将SitEntity保留在偏航0处可以将女仆放在靠背上。酒吧凳有意通过 Tavern 的块实体逻辑与乘客一起自由旋转。
        if (state.getBlock() instanceof SofaBlock && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            float yaw = state.getValue(HorizontalDirectionalBlock.FACING).toYRot();
            seat.setYRot(yaw);
            maid.setYRot(yaw);
            maid.setYBodyRot(yaw);
            maid.setYHeadRot(yaw);
        }
    }
}
