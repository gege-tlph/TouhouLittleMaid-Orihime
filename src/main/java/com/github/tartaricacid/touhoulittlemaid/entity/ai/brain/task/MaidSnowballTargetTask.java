package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import cn.sh1rocu.touhoulittlemaid.util.PacketDistributor;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.network.message.MaidAnimationPackage;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.phys.AABB;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class MaidSnowballTargetTask extends Behavior<EntityMaid> {
    private static final float CHANCE_STOPPING = 1 / 32F;
    private final int attackCooldown;
    private boolean canThrow = false;
    private int attackTime = -1;
    private int playPickUpAnimationDelayTime = -1;
    /**
     * 本任务开打时的那个「玩伴」。
     *
     * <p>{@code ATTACK_TARGET} 是**共享槽**——威胁响应、敌我策略、各战斗行为都读它，
     * 而打雪仗只是借它存玩伴。收尾时若无条件擦除，就会擦掉**别人刚写进去的**东西：
     * 取证得机制是「威胁来了 → 应战写入攻击者并切换活动 → 本任务因活动切换被 stop
     * → 擦掉应战刚设好的目标 → 应战下一 tick 发现目标对不上而自我撤销 → 雪仗夺回控制」（症状为用户实机报告，机制链为逐行取证，未插桩实证）。
     * 记下 UUID（不持实体引用）以便收尾时判断「现在槽里的还是不是我放的那个」。</p>
     */
    private @Nullable UUID playmateId;

    public MaidSnowballTargetTask(int attackCooldown) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT), 1200);
        this.attackCooldown = attackCooldown;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, EntityMaid owner) {
        Optional<LivingEntity> memory = owner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (memory.isPresent()) {
            LivingEntity target = memory.get();
            return owner.isHolding(item -> item.getItem() instanceof SnowballItem || item.isEmpty()) && BehaviorUtils.canSee(owner, target) && inMaxDistance(owner);
        }
        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        return chanceStop(entityIn) && entityIn.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET) && isCurrentTargetInSameLevel(entityIn) && isCurrentTargetAlive(entityIn) && this.checkExtraStartConditions(worldIn, entityIn);
    }

    @Override
    protected void start(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        // 必须在第一个 return 之前记下——下面两支都可能提前返回
        this.playmateId = entityIn.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET)
                .map(LivingEntity::getUUID).orElse(null);
        if (entityIn.getMainHandItem().isEmpty()) {
            entityIn.setItemInHand(InteractionHand.MAIN_HAND, Items.SNOWBALL.getDefaultInstance());
            PacketDistributor.sendToPlayersTrackingEntity(entityIn, MaidAnimationPackage.pickUpSnowball(entityIn));
            return;
        }
        if (!(entityIn.getMainHandItem().getItem() instanceof SnowballItem) && entityIn.getOffhandItem().isEmpty()) {
            entityIn.setItemInHand(InteractionHand.OFF_HAND, Items.SNOWBALL.getDefaultInstance());
            PacketDistributor.sendToPlayersTrackingEntity(entityIn, MaidAnimationPackage.pickUpSnowball(entityIn));
        }
    }

    @Override
    protected void tick(ServerLevel worldIn, EntityMaid owner, long gameTime) {
        owner.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).ifPresent((target) -> {
            boolean canSee = BehaviorUtils.canSee(owner, target);
            if (canThrow && canSee) {
                canThrow = false;
                if (owner.getMainHandItem().getItem() instanceof SnowballItem) {
                    owner.swing(InteractionHand.MAIN_HAND);
                } else {
                    owner.swing(InteractionHand.OFF_HAND);
                }
                BehaviorUtils.lookAtEntity(owner, target);
                performRangedAttack(owner, target);
                this.attackTime = this.attackCooldown + owner.getRandom().nextInt(this.attackCooldown);
                // 略微早一些播放动画
                this.playPickUpAnimationDelayTime = 25;
            } else if (--this.attackTime <= 0) {
                this.canThrow = true;
            }

            // 拾取雪球的动画需要延迟 30 tick 播放，给丢出动画预留一些时间
            if (this.playPickUpAnimationDelayTime >= 0) {
                this.playPickUpAnimationDelayTime--;
            }
            if (this.playPickUpAnimationDelayTime == 0) {
                PacketDistributor.sendToPlayersTrackingEntity(owner, MaidAnimationPackage.pickUpSnowball(owner));
            }

            // 如果女仆处于捡雪球动画中，禁止移动
            if (owner.getAnimationManager().animationId == MaidAnimationPackage.PICK_UP_SNOWBALL) {
                // 捡雪球动画默认 1750 毫秒
                if (System.currentTimeMillis() - owner.getAnimationManager().animationRecordTime > 1750) {
                    owner.getAnimationManager().animationId = MaidAnimationPackage.NONE;
                    owner.getAnimationManager().animationRecordTime = -1L;
                }
                owner.getNavigation().stop();
            }
        });
    }

    private void performRangedAttack(EntityMaid shooter, LivingEntity target) {
        // 发射的是无 shooter 雪球，避免打中其他生物惹来攻击
        // 形参那个 ItemStack 是渲染用的贴图来源，与归属无关（原版 ThrowableItemProjectile 约定）
        Snowball snowball = new MaidPlaySnowball(shooter);
        double x = target.getX() - shooter.getX();
        double y = target.getBoundingBox().minY + target.getBbHeight() / 3.0F - snowball.position().y;
        double z = target.getZ() - shooter.getZ();
        double pitch = Math.sqrt(x * x + z * z) * 0.15D;
        snowball.shoot(x, y + pitch, z, 1.6F, 1);
        shooter.playSound(SoundEvents.SNOWBALL_THROW, 0.5F, 0.4F / (shooter.getRandom().nextFloat() * 0.4F + 0.8F));
        shooter.level().addFreshEntity(snowball);
    }

    @Override
    protected void stop(ServerLevel worldIn, EntityMaid entityIn, long gameTimeIn) {
        this.canThrow = false;
        clearAttackTarget(entityIn);
        this.playmateId = null;
    }

    private boolean isCurrentTargetInSameLevel(LivingEntity entity) {
        Optional<LivingEntity> optional = entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        return optional.isPresent() && optional.get().level() == entity.level();
    }

    private boolean isCurrentTargetAlive(LivingEntity entity) {
        Optional<LivingEntity> optional = entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        return optional.isPresent() && optional.get().isAlive();
    }

    private boolean inMaxDistance(EntityMaid maid) {
        Optional<LivingEntity> optional = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        return optional.isPresent() && maid.isWithinHome(optional.get().blockPosition());
    }

    private boolean chanceStop(LivingEntity entity) {
        return entity.getRandom().nextFloat() > CHANCE_STOPPING;
    }

    /**
     * 只擦自己放进去的那个玩伴——**谁设的谁擦**。
     *
     * <p>槽里现在若换成了别人（最典型的是威胁响应写进去的攻击者），这里必须放手，
     * 否则收尾会把别人的状态一起清掉。见 {@link #playmateId} 的说明。</p>
     */
    private void clearAttackTarget(LivingEntity entity) {
        if (this.playmateId == null) {
            return;
        }
        LivingEntity current = entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
        if (current != null && this.playmateId.equals(current.getUUID())) {
            entity.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        }
    }

    /**
     * 保持雪球无归属（这样打中别的生物不会算成女仆挑衅），但把原版「出膛前不打到发射者」
     * 那条只应用在造出它的这只女仆身上。
     *
     * <p>缺了它有两个后果：雪球一出手就打在女仆自己身上；以及无归属带来的好处也拿不到，
     * 因为原版会把它当成普通抛射物处理。</p>
     */
    private static final class MaidPlaySnowball extends Snowball {
        private final EntityMaid shooter;
        private boolean leftShooter;

        private MaidPlaySnowball(EntityMaid shooter) {
            super(shooter.level(), shooter.getX(), shooter.getY(), shooter.getZ(), Items.SNOWBALL.getDefaultInstance());
            this.shooter = shooter;
        }

        @Override
        public void tick() {
            if (!leftShooter) {
                AABB sweptBounds = getBoundingBox().expandTowards(getDeltaMovement()).inflate(1.0);
                leftShooter = !sweptBounds.intersects(shooter.getBoundingBox());
            }
            super.tick();
        }

        @Override
        protected boolean canHitEntity(Entity entity) {
            return (leftShooter || entity != shooter) && super.canHitEntity(entity);
        }
    }
}