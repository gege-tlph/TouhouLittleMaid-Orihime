package com.github.tartaricacid.touhoulittlemaid.entity.ai.combat;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * Routes only final, positive server damage into per-maid transient combat.
 * The Fabric AFTER_DAMAGE hook is deliberately used instead of the existing
 * hurtServer-HEAD compatibility event or pre-impact attack timestamps.
 */
public final class MaidCombatDamageListener {
    private MaidCombatDamageListener() {
    }

    public static void onAfterDamage(LivingEntity victim, DamageSource source,
                                     float baseDamageTaken, float damageTaken, boolean blocked) {
        if (damageTaken <= 0 || blocked || !(victim.level() instanceof ServerLevel level)) {
            return;
        }

        if (victim instanceof EntityMaid maid) {
            maid.getCombatManager().onMaidDamaged(source);
        }

        Entity responsible = source.getEntity();
        if (victim instanceof ServerPlayer owner && responsible instanceof LivingEntity attacker
                && attacker != owner) {
            forEachLocalOwnedMaid(level, owner,
                    maid -> maid.getCombatManager().onOwnerDamaged(attacker));
        }

        if (responsible instanceof ServerPlayer owner && victim != owner
                && isDirectOwnerDamage(owner, source)) {
            forEachLocalOwnedMaid(level, owner,
                    maid -> maid.getCombatManager().onOwnerDealtDirectDamage(victim));
        }
    }

    private static boolean isDirectOwnerDamage(ServerPlayer owner, DamageSource source) {
        if (source.is(DamageTypes.THORNS) || source.is(DamageTypeTags.IS_FIRE)) {
            return false;
        }
        Entity direct = source.getDirectEntity();
        if (direct == owner) {
            return true;
        }
        return direct instanceof Projectile projectile && projectile.getOwner() == owner;
    }

    private static void forEachLocalOwnedMaid(ServerLevel level, ServerPlayer owner,
                                               java.util.function.Consumer<EntityMaid> action) {
        double range = MaidCombatManager.LOCAL_PROTECTION_RANGE;
        level.getEntitiesOfClass(EntityMaid.class, owner.getBoundingBox().inflate(range),
                        maid -> maid.isOwnedBy(owner))
                .forEach(action);
    }
}
