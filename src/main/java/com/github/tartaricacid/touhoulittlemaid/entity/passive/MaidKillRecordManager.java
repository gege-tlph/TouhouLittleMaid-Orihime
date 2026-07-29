package com.github.tartaricacid.touhoulittlemaid.entity.passive;

import com.github.tartaricacid.touhoulittlemaid.advancements.maid.TriggerType;
import com.github.tartaricacid.touhoulittlemaid.init.InitTrigger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Slime;

import javax.annotation.Nullable;

public final class MaidKillRecordManager {
    private static final String KILL_RECORD = "KillRecord";
    private static final String TOTAL_COUNT = "TotalCount";
    private static final String SLIME_COUNT = "Slime";
    private static final String WITHER_COUNT = "Wither";
    private static final String ENDER_DRAGON_COUNT = "EnderDragon";

    private int totalCount;
    private int slimeCount;
    private int witherCount;
    private int enderDragonCount;

    // 1.21.11: 迁移到 ValueOutput/ValueInput，用 COMPOUND_TAG_CODEC 桥接 "KillRecord" 子 compound，
    // 与 HEAD 的 compound.put(KILL_RECORD, killRecord) 逐字节同格式（此前的移植版把 4 个 int 平铺到根
    // = 破坏与 1.21.1 存档的兼容，已还原 HEAD 布局）。
    // ⚠️ 保留 HEAD 既有的键不对称：写时 totalCount 存入子键 "KillRecord"（=KILL_RECORD），读时却从 "TotalCount"
    //    (=TOTAL_COUNT) 取 → totalCount 实际从不回存（重载后归 0）。这是 HEAD 的原有行为，此处忠实保留；
    //    修它属独立的 gameplay 决策，非移植任务。
    void addAdditionalSaveData(ValueOutput output) {
        CompoundTag killRecord = new CompoundTag();
        killRecord.putInt(KILL_RECORD, totalCount);
        killRecord.putInt(SLIME_COUNT, slimeCount);
        killRecord.putInt(WITHER_COUNT, witherCount);
        killRecord.putInt(ENDER_DRAGON_COUNT, enderDragonCount);
        output.store(KILL_RECORD, CustomData.COMPOUND_TAG_CODEC, killRecord);
    }

    void readAdditionalSaveData(ValueInput input) {
        input.read(KILL_RECORD, CustomData.COMPOUND_TAG_CODEC).ifPresent(killRecord -> {
            totalCount = killRecord.getIntOr(TOTAL_COUNT, 0);
            slimeCount = killRecord.getIntOr(SLIME_COUNT, 0);
            witherCount = killRecord.getIntOr(WITHER_COUNT, 0);
            enderDragonCount = killRecord.getIntOr(ENDER_DRAGON_COUNT, 0);
        });
    }

    public void onTargetDeath(EntityMaid maid, LivingEntity target) {
        LivingEntity owner = maid.getOwner();
        this.totalCount++;
        triggerKill(owner, TriggerType.MAID_KILL_MOB);
        if (this.totalCount >= 100) {
            triggerKill(owner, TriggerType.KILL_100);
        }
        if (target instanceof Slime) {
            this.slimeCount++;
            if (slimeCount >= 300) {
                triggerKill(owner, TriggerType.KILL_SLIME_300);
            }
        }
        if (target instanceof WitherBoss) {
            this.witherCount++;
            triggerKill(owner, TriggerType.KILL_WITHER);
        }
        if (target instanceof EnderDragon) {
            this.enderDragonCount++;
            triggerKill(owner, TriggerType.KILL_DRAGON);
        }
    }

    private void triggerKill(@Nullable LivingEntity owner, String eventName) {
        if (owner instanceof ServerPlayer serverPlayer) {
            InitTrigger.MAID_EVENT.trigger(serverPlayer, eventName);
        }
    }
}
