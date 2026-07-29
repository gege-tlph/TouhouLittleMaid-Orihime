package com.github.tartaricacid.touhoulittlemaid.world.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;

import java.util.UUID;

public record MaidInfo(String dimension, BlockPos chunkPos, UUID ownerId, UUID entityId, long timestamp,
                       Component name) {
    private static final String RESOURCE_KEY_PREFIX = "ResourceKey[";
    private static final String RESOURCE_KEY_SEPARATOR = " / ";

    public MaidInfo {
        dimension = normalizeDimension(dimension);
    }

    /** Repairs dimension strings written by the early 1.21.11 port via ResourceKey#toString(). */
    private static String normalizeDimension(String dimension) {
        if (dimension.startsWith(RESOURCE_KEY_PREFIX) && dimension.endsWith("]")) {
            int separator = dimension.lastIndexOf(RESOURCE_KEY_SEPARATOR);
            if (separator >= RESOURCE_KEY_PREFIX.length()) {
                return dimension.substring(separator + RESOURCE_KEY_SEPARATOR.length(), dimension.length() - 1);
            }
        }
        return dimension;
    }

    public static final Codec<MaidInfo> MAID_INFO_CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("Dimension").forGetter(o -> o.dimension),
            BlockPos.CODEC.fieldOf("ChunkPos").forGetter(o -> o.chunkPos),
            UUIDUtil.CODEC.fieldOf("OwnerId").forGetter(o -> o.ownerId),
            UUIDUtil.CODEC.fieldOf("MaidId").forGetter(o -> o.entityId),
            Codec.LONG.fieldOf("Timestamp").forGetter(o -> o.timestamp),
            ComponentSerialization.CODEC.fieldOf("Name").forGetter(o -> o.name)
    ).apply(ins, MaidInfo::new));

    public static final Codec<MaidInfo> TOMBS_STONE_CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.STRING.fieldOf("Dimension").forGetter(o -> o.dimension),
            BlockPos.CODEC.fieldOf("ChunkPos").forGetter(o -> o.chunkPos),
            UUIDUtil.CODEC.fieldOf("OwnerId").forGetter(o -> o.ownerId),
            UUIDUtil.CODEC.fieldOf("TombstoneId").forGetter(o -> o.entityId),
            Codec.LONG.fieldOf("Timestamp").forGetter(o -> o.timestamp),
            ComponentSerialization.CODEC.fieldOf("Name").forGetter(o -> o.name)
    ).apply(ins, MaidInfo::new));
}
