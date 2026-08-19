package com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task;

import cn.sh1rocu.touhoulittlemaid.util.PacketDistributor;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.ExperimentalConfig;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

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
        /** 原版 {@code LivingEntity.hurtServer} 里击退抛射物受害者用的就是这个值（字节码 offset 453）。 */
        private static final float KNOCKBACK_STRENGTH = 0.4F;

        private final EntityMaid shooter;
        private boolean leftShooter;

        private MaidPlaySnowball(EntityMaid shooter) {
            // ⚠️ 高度取眼高而不是 getY()（脚底）：原版基于射手的构造器就是 getEyeY()-0.1
            // （字节码实证）。上游这里用的是裸坐标构造器 + 脚底高度，而触发玩雪的前提正是
            // 女仆站在雪片上——雪球于是在贴地处出生、弹道极低，落在目标前方的地上，
            // 结果是这个功能**从来没打中过任何人**。
            super(shooter.level(), shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ(),
                    Items.SNOWBALL.getDefaultInstance());
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

        /**
         * 世界规则「雪球击退效果」开启时，把**玩家**也击退——默认关，关着就是原版表现。
         *
         * <p><b>为什么只补玩家这一种</b>：原版雪球对非烈焰人是 0 伤害，而击退整段住在
         * {@code LivingEntity.hurtServer} 里。怪物走得到那里，所以**本来就会被雪球推开**；
         * 玩家走不到——{@code Player.hurtServer} 在 {@code amount == 0} 处直接 return false
         * （26.1.2 offset 108-115 字节码实证），比击退那段早得多。所以判据不是「我猜谁需要补」，
         * 而是「原版那条路径对谁没走完」，而这个集合恰好等于 {@code Player}：
         * 雪球只对烈焰人给非 0 伤害，玩家永远不是烈焰人。</p>
         *
         * <p>补的力度与方向**照抄原版那一段**：{@code knockback(0.4F, -Δx, -Δz)}，
         * 其中 Δ 取抛射物自身速度（{@code Projectile.calculateHorizontalHurtKnockbackDirection}
         * 的方法体就是 {@code getDeltaMovement().x/.z}），原版对它取负再传入。
         * 击退抗性与运动同步由 {@code knockback} 自己处理，不必也不该在这里重做。</p>
         *
         * <p>⚠️ 前三道闸是把 {@code Player.hurtServer} 在 {@code amount == 0} <b>之前</b>
         * 的判定原样复述一遍：那些情形下原版连伤害流程都不进，我们也不许推人——
         * 否则创造模式和旁观模式会被雪球推着走，那是原版从不会有的表现。</p>
         */
        @Override
        protected void onHitEntity(EntityHitResult result) {
            super.onHitEntity(result);
            if (!(level() instanceof ServerLevel serverLevel)
                    || !(result.getEntity() instanceof Player player)
                    || !ServerRuleConfig.get(ExperimentalConfig.SNOWBALL_KNOCKBACK)) {
                return;
            }
            DamageSource source = damageSources().thrown(this, getOwner());
            if (player.isInvulnerableTo(serverLevel, source)
                    || player.getAbilities().invulnerable
                    || player.isDeadOrDying()) {
                return;
            }
            Vec3 motion = getDeltaMovement();
            player.knockback(KNOCKBACK_STRENGTH, -motion.x, -motion.z);
            // ⚠️ 还得让**受击者自己的客户端**知道，否则这次击退玩家自己毫无感觉：
            // 玩家的移动是客户端权威的，服务端算出来的速度会被下一个位置包直接覆盖。
            // 原版那一段是**两句**——offset 349 的 markHurt() 与随后的 knockback()：
            // 前者置 hurtMarked，ServerEntity 据它走 sendToTrackingPlayersAndSelf；
            // 而 knockback() 自己只置 needsSync，那条**只发给别人**。
            // 照抄一半的后果正是「别人看得见你被推，你自己没反应」（字节码实证，2026-08-19 实机报出）。
            // markHurt() 是 protected，够不着；hurtMarked 是 public 字段，直接置位。
            player.hurtMarked = true;
        }

        @Override
        protected boolean canHitEntity(Entity entity) {
            return (leftShooter || entity != shooter) && super.canHitEntity(entity);
        }
    }
}