package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain;


import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.entity.schedule.Activity;

import java.util.function.IntFunction;

public enum MaidSchedule {
    // 日程表的模式
    DAY, NIGHT, ALL;

    // 1.21.11: Schedule 移除 → 每个模式映射到一个 EnvironmentAttribute<Activity>（InitEntities 注册，datapack timeline 供关键帧）
    public EnvironmentAttribute<Activity> getEnvironmentAttribute() {
        return switch (this) {
            case DAY -> InitEntities.MAID_DAY_SHIFT_ACTIVITY;
            case NIGHT -> InitEntities.MAID_NIGHT_SHIFT_ACTIVITY;
            case ALL -> InitEntities.MAID_ALL_DAY_ACTIVITY;
        };
    }

    public static final IntFunction<MaidSchedule> BY_ID =
            ByIdMap.continuous(
                    MaidSchedule::ordinal,
                    MaidSchedule.values(),
                    ByIdMap.OutOfBoundsStrategy.ZERO
            );
    public static final StreamCodec<ByteBuf, MaidSchedule> STREAM_CODEC = ByteBufCodecs.idMapper(MaidSchedule.BY_ID, MaidSchedule::ordinal);

    public static final EntityDataSerializer<MaidSchedule> DATA = new EntityDataSerializer<>() {

        @Override
        public StreamCodec<? super RegistryFriendlyByteBuf, MaidSchedule> codec() {
            return STREAM_CODEC;
        }

        @Override
        public MaidSchedule copy(MaidSchedule value) {
            return value;
        }
    };
}
