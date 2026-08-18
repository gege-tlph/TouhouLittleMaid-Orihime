package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidTargetingContext;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.targeting.MaidTargetingPolicy;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitAttribute;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Optional;
import java.util.function.Predicate;

import static com.github.tartaricacid.touhoulittlemaid.datagen.EnchantmentKeys.getEnchantmentLevel;

/**
 * MaidShootTargetAnyItemTask 的升级版本，不限制手持物品必须是 ProjectileWeaponItem
 */
public class MaidShootTargetAnyItemTask extends Behavior<EntityMaid> {
    private final int attackCooldown;
    private final int chargeDurationTick;
    private final Predicate<EntityMaid> weaponTest;
    private int attackTime = -1;
    private int seeTime;
    private int swingTime;

    public MaidShootTargetAnyItemTask(int attackCooldown, int chargeDurationTick, Predicate<EntityMaid> weaponTest) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), 1200);
        this.attackCooldown = attackCooldown;
        this.chargeDurationTick = chargeDurationTick;
        this.weaponTest = weaponTest;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, EntityMaid owner) {
        Optional<LivingEntity> memory = owner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (memory.isPresent()) {
            LivingEntity target = memory.get();
            return MaidTargetingPolicy.canContinueTargeting(owner, target, MaidTargetingContext.PLANNED_ATTACK)
                    && weaponTest.test(owner) && owner.canSee(target);
        }
        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        return entityIn.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && this.checkExtraStartConditions(worldIn, entityIn);
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        entityIn.setSwingingArms(true);
    }

    @Override
    protected void tick(ServerLevel worldIn, EntityMaid owner, long gameTime) {
        owner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent((target) -> {
            // 强行看见并朝向
            owner.getLookControl().setLookAt(target.getX(), target.getY(), target.getZ());
            boolean canSee = owner.canSee(target);
            boolean seeTimeMoreThanZero = this.seeTime > 0;

            // 如果两者不一致，重置看见时间
            if (canSee != seeTimeMoreThanZero) {
                this.seeTime = 0;
            }
            // 如果看见了对方，增加看见时间，否则减少
            if (canSee) {
                ++this.seeTime;
            } else {
                --this.seeTime;
            }

            // 依据是否可以 use 物品，分别判断
            boolean itemCanNotUse = owner.getMainHandItem().getUseDuration(owner) <= 0;
            if (itemCanNotUse) {
                tickItemCanNotUse(owner, target, canSee);
            } else {
                // 如果实体手部处于激活状态
                tickItemCanUse(owner, target, canSee);
            }
        });
    }

    private void tickItemCanUse(EntityMaid owner, LivingEntity target, boolean canSee) {
        if (owner.isUsingItem()) {
            // 如果看不见对方超时 60，重置激活状态
            if (!canSee && this.seeTime < -60) {
                owner.stopUsingItem();
            } else if (canSee) {
                // 否则开始进行远程攻击
                // ⚠️ 蓄力门槛必须读快速射击附魔，否则附魔弓与白板弓射速一样——附魔白附。
                // 与 MaidShootTargetTask 同式，只是把上游硬编码的 20 换成本任务的 chargeDurationTick
                // （应战接线传的正是 20，故对弓/弩逐字等价）。
                int ticksUsingItem = owner.getTicksUsingItem();
                int level = getEnchantmentLevel(owner.level.registryAccess(),
                        Enchantments.QUICK_CHARGE, owner.getMainHandItem());
                if (level > 4 || ticksUsingItem >= (this.chargeDurationTick - level * 5)) {
                    owner.stopUsingItem();
                    int powerTime = Math.max(ticksUsingItem, 20);
                    owner.performRangedAttack(target, BowItem.getPowerForTime(powerTime));
                    this.attackTime = resolveAttackCooldown(owner);
                }
            }
        } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
            // 非激活状态，但是时长合适，开始激活手部
            owner.startUsingItem(InteractionHand.MAIN_HAND);
        }
    }

    /**
     * 射击间隔按属性取值，与 {@link MaidShootTargetTask} 同式。
     *
     * <p>⚠️ {@link InitAttribute#MAID_SHOOT_COOLDOWN} <b>就是为调射速而存在的</b>；
     * 本任务此前恒用构造参数，等于让它在应战期整个失效。上游无属性时回落硬编码 2，
     * 这里回落到本任务的 attackCooldown——应战接线传的正是 2，故对弓/弩逐字等价。</p>
     */
    private int resolveAttackCooldown(EntityMaid owner) {
        AttributeInstance attributeInstance = owner.getAttribute(InitAttribute.MAID_SHOOT_COOLDOWN);
        return attributeInstance != null ? (int) attributeInstance.getValue() : this.attackCooldown;
    }

    private void tickItemCanNotUse(EntityMaid owner, LivingEntity target, boolean canSee) {
        if (owner.isSwingingArms()) {
            if (!canSee && this.seeTime < -60) {
                owner.setSwingingArms(false);
                swingTime = 0;
            } else if (canSee) {
                if (swingTime >= this.chargeDurationTick) {
                    int powerTime = Math.max(swingTime, 20);
                    owner.performRangedAttack(target, BowItem.getPowerForTime(powerTime));
                    this.attackTime = resolveAttackCooldown(owner);
                    owner.setSwingingArms(false);
                    swingTime = 0;
                }
            }
            swingTime++;
        } else if (--this.attackTime <= 0 && this.seeTime >= -60) {
            owner.setSwingingArms(true);
        }
    }

    @Override
    protected void stop(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        this.seeTime = 0;
        this.attackTime = -1;
        this.swingTime = 0;
        entityIn.stopUsingItem();
        // start() 置了 swingingArms，可用物品那一支（弓）从不清它——换武器后保持拉弓姿势
        entityIn.setSwingingArms(false);
    }
}