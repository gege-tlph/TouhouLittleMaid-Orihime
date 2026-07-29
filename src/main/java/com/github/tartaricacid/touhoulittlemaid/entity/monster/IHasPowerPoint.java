package com.github.tartaricacid.touhoulittlemaid.entity.monster;

import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityPowerPoint;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.gamerules.GameRules;

public interface IHasPowerPoint {
    /**
     * 获取 Power 点数值
     *
     * @return Power 点数值，与玩家 Power 数是 100 倍的关系
     */
    int getPowerPoint();

    /**
     * 掉落 P 点的方法
     *
     * @param entity 掉落该 P 点的实体
     */
    default void dropPowerPoint(LivingEntity entity) {
        // 需要考虑 doMobLoot 规则
        // SWEEP R9-4：1.21.11 doMobLoot 更名 mob_drops（GameRuleRegistryFix 证），且 getGameRules()
        // 移到 ServerLevel（同树 AbstractEntityFromItem:84 同款）——还原 origin 检查；客户端侧
        // 本就被下方 !isClientSide 拦截，行为不变
        if (entity.level instanceof ServerLevel serverLevel && !serverLevel.getGameRules().get(GameRules.MOB_DROPS)) {
            return;
        }
        int dropTime = 20;
        if (entity.deathTime == dropTime && !entity.level.isClientSide()) {
            int totalPowerPoint = getPowerPoint();
            while (totalPowerPoint > 0) {
                int powerSplit = EntityPowerPoint.getPowerValue(totalPowerPoint);
                totalPowerPoint -= powerSplit;
                EntityPowerPoint powerPoint = new EntityPowerPoint(entity.level, entity.getX(), entity.getY(), entity.getZ(), powerSplit);
                entity.level.addFreshEntity(powerPoint);
            }
        }
    }
}
