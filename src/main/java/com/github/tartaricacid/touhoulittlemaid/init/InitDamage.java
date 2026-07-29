package com.github.tartaricacid.touhoulittlemaid.init;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.*;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.EntityDanmaku;
import net.minecraft.world.entity.Entity;

public final class InitDamage {
    public static final ResourceKey<DamageType> DANMAKU = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "danmaku"));
    public static final ResourceKey<DamageType> DANMAKU_ENDER_KILLER = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "danmaku_ender_killer"));

    // HEAD/26.1 均为 2 参（由 danmaku.isHurtEnderman() 内部决定伤害类型）；移植期误改成 boolean 参且未更新调用点 → 还原 2 参。
    public static DamageSource danmakuDamage(Entity thrower, EntityDanmaku danmaku) {
        var damageTypes = thrower.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
        if (danmaku.isHurtEnderman()) {
            return new DamageSource(damageTypes.getOrThrow(DANMAKU_ENDER_KILLER), danmaku, thrower);
        } else {
            return new DamageSource(damageTypes.getOrThrow(DANMAKU), danmaku, thrower);
        }
    }

    public static void bootstrap(BootstrapContext<DamageType> context) {
        context.register(DANMAKU, new DamageType("touhou_little_maid.danmaku", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, DamageEffects.HURT, DeathMessageType.DEFAULT));
        context.register(DANMAKU_ENDER_KILLER, new DamageType("touhou_little_maid.danmaku", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1f, DamageEffects.HURT, DeathMessageType.DEFAULT));
    }
}
