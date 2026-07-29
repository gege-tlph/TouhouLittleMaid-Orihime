package com.github.tartaricacid.touhoulittlemaid.entity.passive;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.util.TeleportHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;

public final class SchedulePos {
    private static final int MAX_TELEPORT_ATTEMPTS_TIMES = 10;

    private BlockPos workPos;
    private BlockPos idlePos;
    private BlockPos sleepPos;
    private Identifier dimension;
    private boolean configured = false;

    public static final StreamCodec<RegistryFriendlyByteBuf, SchedulePos> SCHEDULE_POS_STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SchedulePos::getWorkPos,
            BlockPos.STREAM_CODEC, SchedulePos::getIdlePos,
            BlockPos.STREAM_CODEC, SchedulePos::getSleepPos,
            Identifier.STREAM_CODEC, SchedulePos::getDimension,
            SchedulePos::new
    );

    public SchedulePos(BlockPos workPos, BlockPos idlePos, BlockPos sleepPos, Identifier dimension) {
        this.workPos = workPos;
        this.idlePos = idlePos;
        this.sleepPos = sleepPos;
        this.dimension = dimension;
    }

    public SchedulePos(BlockPos workPos, BlockPos idlePos, Identifier dimension) {
        this(workPos, idlePos, idlePos, dimension);
    }

    public SchedulePos(BlockPos workPos, Identifier dimension) {
        this(workPos, workPos, dimension);
    }

    public void setWorkPos(BlockPos workPos) {
        this.workPos = workPos;
    }

    public void setIdlePos(BlockPos idlePos) {
        this.idlePos = idlePos;
    }

    public void setSleepPos(BlockPos sleepPos) {
        this.sleepPos = sleepPos;
    }

    public void setDimension(Identifier dimension) {
        this.dimension = dimension;
    }

    public void tick(EntityMaid maid) {
        if (maid.tickCount % 40 == 0) {
            this.setHomeTo(maid);
            if (maid.isWithinHome()) {
                return;
            }
            if (!maid.canBrainMoving()) {
                return;
            }
            double distanceSqr = maid.getHomePosition().distSqr(maid.blockPosition());
            int minTeleportDistance = maid.getHomeRadius() + 4;
            if (distanceSqr > (minTeleportDistance * minTeleportDistance) && !this.sameWithRestrictCenter(maid)) {
                teleport(maid);
            } else {
                BehaviorUtils.setWalkAndLookTargetMemories(maid, maid.getHomePosition(), 0.7f, 3);
            }
        }
    }

    public void save(ValueOutput output) {
        // 逐字节对齐 origin/1.21.1：嵌套在 "MaidSchedulePos" 子 compound（此前被移植期扁平化到 maid 根 →
        //   跨版本升级世界重置 home/work/idle/sleep + 污染实体根命名，见 CLIENT_AUDIT §I.A/S2）。
        //   位置用 int-array [x,y,z] == HEAD 的 NbtUtils.writeBlockPos（逐字节等价）。
        ValueOutput data = output.child("MaidSchedulePos");
        data.putIntArray("Work", new int[]{this.workPos.getX(), this.workPos.getY(), this.workPos.getZ()});
        data.putIntArray("Idle", new int[]{this.idlePos.getX(), this.idlePos.getY(), this.idlePos.getZ()});
        data.putIntArray("Sleep", new int[]{this.sleepPos.getX(), this.sleepPos.getY(), this.sleepPos.getZ()});
        data.putString("Dimension", this.dimension.toString());
        data.putBoolean("Configured", this.configured);
    }

    public void load(ValueInput input, EntityMaid maid) {
        input.child("MaidSchedulePos").ifPresent(data -> {
            data.getIntArray("Work").ifPresent(arr -> { if (arr.length == 3) this.workPos = new BlockPos(arr[0], arr[1], arr[2]); });
            data.getIntArray("Idle").ifPresent(arr -> { if (arr.length == 3) this.idlePos = new BlockPos(arr[0], arr[1], arr[2]); });
            data.getIntArray("Sleep").ifPresent(arr -> { if (arr.length == 3) this.sleepPos = new BlockPos(arr[0], arr[1], arr[2]); });
            this.dimension = Identifier.parse(data.getStringOr("Dimension", ""));
            this.configured = data.getBooleanOr("Configured", false);
        });
        this.setHomeTo(maid);
    }

    public void setHomeTo(EntityMaid maid) {
        if (!maid.isHomeModeEnable()) {
            return;
        }
        Activity activity = maid.getScheduleDetail();
        if (activity == Activity.WORK) {
            maid.setHomeTo(this.workPos, ServerRuleConfig.get(MaidConfig.MAID_WORK_RANGE));
            return;
        }
        if (activity == Activity.IDLE) {
            maid.setHomeTo(this.idlePos, ServerRuleConfig.get(MaidConfig.MAID_IDLE_RANGE));
            return;
        }
        if (activity == Activity.REST) {
            maid.setHomeTo(this.sleepPos, ServerRuleConfig.get(MaidConfig.MAID_SLEEP_RANGE));
        }
    }

    public void setConfigured(boolean configured) {
        this.configured = configured;
    }

    public BlockPos getWorkPos() {
        return workPos;
    }

    public BlockPos getIdlePos() {
        return idlePos;
    }

    public BlockPos getSleepPos() {
        return sleepPos;
    }

    public boolean isConfigured() {
        return configured;
    }

    public Identifier getDimension() {
        return dimension;
    }

    public void clear(EntityMaid maid) {
        this.idlePos = this.workPos;
        this.sleepPos = this.workPos;
        this.configured = false;
        // B8 修复: ResourceKey.location() → identifier()（javap 确认）。移植期误用 .toString()（="ResourceKey[...]"，Identifier.parse 拒绝→崩溃）。还原 HEAD 语义。
        this.dimension = maid.level.dimension().identifier();
        this.setHomeTo(maid);
    }

    public void setHomeModeEnable(EntityMaid maid, BlockPos pos) {
        if (!this.configured) {
            this.workPos = pos;
            this.idlePos = pos;
            this.sleepPos = pos;
            // 1.21.11: ResourceKey.location() -> identifier(). Do not parse
            // ResourceKey#toString(): it is the diagnostic "ResourceKey[...]" form.
            this.dimension = maid.level.dimension().identifier();
        }
        this.setHomeTo(maid);
    }

    @Nullable
    public BlockPos getNearestPos(EntityMaid maid) {
        if (this.configured) {
            BlockPos pos = this.workPos;
            double workDistance = maid.blockPosition().distSqr(this.workPos);
            double idleDistance = maid.blockPosition().distSqr(this.idlePos);
            double sleepDistance = maid.blockPosition().distSqr(this.sleepPos);
            if (workDistance > idleDistance) {
                pos = this.idlePos;
                workDistance = idleDistance;
            }
            if (workDistance > sleepDistance) {
                pos = this.sleepPos;
            }
            return pos;
        }
        return null;
    }

    private boolean sameWithRestrictCenter(EntityMaid maid) {
        BlockPos restrictCenter = maid.getHomePosition();
        return maid.getBrain().getMemory(MemoryModuleType.WALK_TARGET)
                .filter(walkTarget -> walkTarget.getTarget().currentBlockPosition().equals(restrictCenter))
                .isPresent();
    }

    private void teleport(EntityMaid maid) {
        for (int i = 0; i < MAX_TELEPORT_ATTEMPTS_TIMES; ++i) {
            if (TeleportHelper.teleportToRestrictCenter(maid)) {
                return;
            }
        }
    }
}
