package com.github.tartaricacid.touhoulittlemaid.world.data;

import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityTombstone;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;

public class MaidWorldData extends SavedData {
    private static final Codec<List<MaidInfo>> MAID_INFO_CODEC_LIST = MaidInfo.MAID_INFO_CODEC.listOf().xmap(Lists::newArrayList, Function.identity());
    private static final Codec<List<MaidInfo>> TOMBS_STONE_CODEC_LIST = MaidInfo.TOMBS_STONE_CODEC.listOf().xmap(Lists::newArrayList, Function.identity());

    public static final Codec<MaidWorldData> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, MAID_INFO_CODEC_LIST).xmap(Maps::newHashMap, Function.identity())
                    .fieldOf("MaidInfos").forGetter(o -> (HashMap<UUID, List<MaidInfo>>) o.infos),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, TOMBS_STONE_CODEC_LIST).xmap(Maps::newHashMap, Function.identity())
                    .fieldOf("MaidTombstones").forGetter(o -> (HashMap<UUID, List<MaidInfo>>) o.tombstones),
            // 1.21.1 saves predate the codec migration and do not contain this
            // runtime-only flag. Accept the port-era field without requiring it.
            Codec.BOOL.optionalFieldOf("dirty", false).forGetter(SavedData::isDirty)
    ).apply(ins, (MaidWorldData::new)));

    // 1.21.11 SavedDataType 的 id 直接当存档文件名
    // （data/<id>.dat）。移植时误用 Identifier.toString() = "touhou_little_maid:world_data"——
    // ① 冒号在 Windows 是非法路径字符 → InvalidPathException，世界数据永远无法读/写；
    // ② 文件名偏离 origin → 破坏 1.21.1 存档兼容。origin/1.21.1（及上游 release tag）逐字
    // 为 "touhou_little_maid_world_data"，此处必须原样还原。
    private static final String IDENTIFIER = "touhou_little_maid_world_data";
    private final Map<UUID, List<MaidInfo>> infos;
    private final Map<UUID, List<MaidInfo>> tombstones;

    private MaidWorldData(Map<UUID, List<MaidInfo>> infos, Map<UUID, List<MaidInfo>> tombstones) {
        this.infos = infos;
        this.tombstones = tombstones;
    }

    private MaidWorldData(Map<UUID, List<MaidInfo>> infos, Map<UUID, List<MaidInfo>> tombstones, boolean dirty) {
        this.infos = infos;
        this.tombstones = tombstones;
        this.setDirty(dirty);
    }

    private MaidWorldData() {
        this.infos = Maps.newHashMap();
        this.tombstones = Maps.newHashMap();
    }

    public static SavedDataType<@NotNull MaidWorldData> factory() {
        return new SavedDataType<>(IDENTIFIER, MaidWorldData::new, CODEC, DataFixTypes.ENTITY_CHUNK);
    }

    @Nullable
    public static MaidWorldData get(Level level) {
        if (level instanceof ServerLevel) {
            ServerLevel overWorld = level.getServer().getLevel(Level.OVERWORLD);
            if (overWorld == null) {
                return null;
            }
            DimensionDataStorage storage = overWorld.getDataStorage();
            MaidWorldData data = storage.computeIfAbsent(MaidWorldData.factory());
            data.setDirty();
            return data;
        }
        return null;
    }

    public void addInfo(MaidInfo info) {
        UUID ownerId = info.ownerId();
        List<MaidInfo> maidInfos = this.infos.computeIfAbsent(ownerId, uuid -> Lists.newArrayList());
        maidInfos.add(info);
        this.setDirty();
    }

    public void removeInfo(UUID ownerId, UUID maidId) {
        if (this.infos.containsKey(ownerId)) {
            this.infos.get(ownerId).removeIf(info -> info.entityId().equals(maidId));
            this.setDirty();
        }
    }

    // 1.21.11: TamableAnimal.getOwnerUUID() 已移除，owner 改为 EntityReference<LivingEntity>（可为 null）。
    //   语义保持：无主人时返回 null（与原 getOwnerUUID() 一致）。参照 EntityDanmaku 既有迁移。
    @Nullable
    private static UUID ownerUuid(EntityMaid maid) {
        EntityReference<LivingEntity> ref = maid.getOwnerReference();
        return ref == null ? null : ref.getUUID();
    }

    public void addInfo(EntityMaid maid) {
        String dimension = maid.level().dimension().identifier().toString();
        BlockPos chunkPos = maid.blockPosition();
        UUID ownerId = ownerUuid(maid);
        UUID maidId = maid.getUUID();
        long timestamp = System.currentTimeMillis();
        Component name = maid.getDisplayName();
        this.addInfo(new MaidInfo(dimension, chunkPos, ownerId, maidId, timestamp, name));
    }

    public void removeInfo(EntityMaid maid) {
        this.removeInfo(ownerUuid(maid), maid.getUUID());
    }

    public void addTombstones(EntityMaid maid, EntityTombstone tombstone) {
        String dimension = maid.level().dimension().identifier().toString();
        BlockPos chunkPos = tombstone.blockPosition();
        UUID ownerId = ownerUuid(maid);
        UUID tombstoneId = tombstone.getUUID();
        long timestamp = System.currentTimeMillis();
        Component name = maid.getDisplayName();
        this.addTombstone(new MaidInfo(dimension, chunkPos, ownerId, tombstoneId, timestamp, name));
    }

    public void removeTombstones(EntityTombstone tombstone) {
        this.removeTombstone(tombstone.getOwnerId(), tombstone.getUUID());
    }

    @Nullable
    public List<MaidInfo> getInfos(UUID owner) {
        return infos.get(owner);
    }

    @Nullable
    public List<MaidInfo> getPlayerMaidInfos(Player player) {
        return this.infos.get(player.getUUID());
    }

    public void addTombstone(MaidInfo info) {
        UUID ownerId = info.ownerId();
        List<MaidInfo> tombstoneInfos = this.tombstones.computeIfAbsent(ownerId, uuid -> Lists.newArrayList());
        tombstoneInfos.add(info);
        this.setDirty();
    }

    public void removeTombstone(UUID ownerId, UUID tombstoneId) {
        if (this.tombstones.containsKey(ownerId)) {
            this.tombstones.get(ownerId).removeIf(info -> info.entityId().equals(tombstoneId));
            this.setDirty();
        }
    }

    @Nullable
    public List<MaidInfo> getTombstones(UUID owner) {
        return tombstones.get(owner);
    }

    @Nullable
    public List<MaidInfo> getPlayerMaidTombstones(Player player) {
        return this.tombstones.get(player.getUUID());
    }
}
