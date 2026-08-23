package com.github.tartaricacid.touhoulittlemaid.api.backpack;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;

/**
 * 带持久状态的背包（熔炉/液体）的数据对象，与行为基准 {@code port/1.21.11-fabric} 同一接口。
 *
 * <p>生命周期（本分支形态）：由 {@code MaidBackpackManager} 经
 * {@code BackpackStateData} 附件持有——穿上背包时经 {@link IMaidBackpack#getBackpackData}
 * 创建，读档后首次访问时惰性创建并 {@link #load}，实体存档时由附件的 codec 拉取 {@link #save}，
 * 服务端每 tick 走 {@link #serverTick}。</p>
 *
 * <p>{@link #getDataAccess} 供容器 {@code addDataSlots} 用——GUI 进度条的同步走的是
 * 原版菜单 data slot 通道（26.1.2 javap 证 {@code addDataSlots(ContainerData)} 仍在），
 * **不是**附件同步；所以状态附件本身不需要 {@code syncWith}。</p>
 */
public interface IBackpackData {
    ContainerData getDataAccess();

    void load(CompoundTag tag, EntityMaid maid);

    void save(CompoundTag tag, EntityMaid maid);

    void serverTick(EntityMaid maid);
}
