package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import com.github.tartaricacid.touhoulittlemaid.api.entity.targeting.MaidTargetingContext;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.targeting.MaidTargetingPolicy;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ProjectileWeaponItem;

import java.util.function.Predicate;

/**
 * MaidAttackStrafingTask 的升级版本，不限制手持物品必须是 ProjectileWeaponItem
 */
public class MaidAttackStrafingAnyItemTask extends Behavior<EntityMaid> {
    private final Predicate<EntityMaid> weaponTest;
    private final float projectileRange;
    private final float strafeSpeed;
    private boolean strafingClockwise;
    private boolean strafingBackwards;
    private int strafingTime = -1;

    public MaidAttackStrafingAnyItemTask(Predicate<EntityMaid> weaponTest, float projectileRange, float strafeSpeed) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT,
                        MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                        MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT,
                        MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT),
                1200);
        this.weaponTest = weaponTest;
        this.projectileRange = projectileRange;
        this.strafeSpeed = strafeSpeed;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, EntityMaid owner) {
        return weaponTest.test(owner) &&
                owner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET)
                        .filter(target -> MaidTargetingPolicy.canContinueTargeting(
                                owner, target, MaidTargetingContext.PLANNED_ATTACK))
                        .isPresent();
    }

    @Override
    protected void tick(ServerLevel worldIn, EntityMaid owner, long gameTime) {
        if (!weaponTest.test(owner)) {
            return;
        }
        owner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent((target) -> {
            double distance = owner.distanceTo(target);

            // 如果在最大攻击距离之内，而且看见的时长足够长
            if (distance < owner.searchRadius()) {
                ++this.strafingTime;
            } else {
                this.strafingTime = -1;
            }

            // 如果攻击时间也足够长，随机对走位方向和前后走位进行反转
            if (this.strafingTime >= 20) {
                if (owner.getRandom().nextFloat() < 0.3) {
                    this.strafingClockwise = !this.strafingClockwise;
                }
                if (owner.getRandom().nextFloat() < 0.3) {
                    this.strafingBackwards = !this.strafingBackwards;
                }
                this.strafingTime = 0;
            }

            // 如果攻击时间大于 -1
            if (this.strafingTime > -1) {
                // 依据距离远近决定是否前后走位。
                // ⚠️ 阈值必须按**这把武器自己的**射程取，与 MaidAttackStrafingTask 同式：
                // 弓 15 / 弩 8，而本任务的 projectileRange 是一个固定值（应战接线传 16）。
                // 用固定值等于让弩在 3.2 格就后退（本该 1.6）、拖到 8 格才压上（本该 4），
                // 手感因此明显不如上游。只有拿不到武器射程时（模组远程武器/枪）才回落构造参数。
                double maxAttackDistance = resolveMaxAttackDistance(owner);
                if (distance > maxAttackDistance * 0.5) {
                    this.strafingBackwards = false;
                } else if (distance < maxAttackDistance * 0.2) {
                    this.strafingBackwards = true;
                }

                // 应用走位，但需要考虑玩家位置（与 MaidAttackStrafingTask 同款刹车：
                // 没有 home 且离主人超过 home 半径时停在原地，免得她一路平移着越走越远）
                if (!owner.hasHome() && owner.getOwner() instanceof Player player
                        && owner.distanceTo(player) >= owner.getHomeRadius()) {
                    owner.stopInPlace();
                } else {
                    owner.getMoveControl().strafe(this.strafingBackwards ? -strafeSpeed : strafeSpeed,
                            this.strafingClockwise ? strafeSpeed : -strafeSpeed);
                }
                owner.setYRot(Mth.rotateIfNecessary(owner.getYRot(), owner.yHeadRot, 0.0F));
                BehaviorUtils.lookAtEntity(owner, target);
            } else {
                // 否则只朝向攻击目标
                BehaviorUtils.lookAtEntity(owner, target);
            }
        });
    }

    /**
     * 走位阈值所用的射程：原版远程武器取它自己的 {@code getDefaultProjectileRange()}
     * （与 {@link MaidAttackStrafingTask} 逐字同式），否则回落构造参数。
     *
     * <p>回落分支服务的是**拿不到武器射程**的那一类——枪与模组远程武器；
     * 弓/弩永远走前一支，因此手感与上游一致。</p>
     */
    private double resolveMaxAttackDistance(EntityMaid owner) {
        if (owner.getMainHandItem().getItem() instanceof ProjectileWeaponItem weapon) {
            return weapon.getDefaultProjectileRange();
        }
        return this.projectileRange;
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        entityIn.setSwingingArms(true);
    }

    @Override
    protected void stop(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        entityIn.setSwingingArms(false);
        entityIn.getMoveControl().strafe(0, 0);
    }

    @Override
    protected boolean canStillUse(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        return this.checkExtraStartConditions(worldIn, entityIn);
    }
}