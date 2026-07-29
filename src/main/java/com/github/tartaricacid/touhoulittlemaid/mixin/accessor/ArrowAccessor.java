package com.github.tartaricacid.touhoulittlemaid.mixin.accessor;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AbstractArrow.class)
public interface ArrowAccessor {
    // 1.21.11: inGround 字段已移除，改由 EntityDataAccessor<Boolean> IN_GROUND 同步，
    // 外部只能经 protected isInGround() 读取 → 必须用 @Invoker 而非 @Accessor（javap 确认）
    @Invoker("isInGround")
    boolean tlmInGround();

    @Invoker("getPickupItem")
    ItemStack getTlmPickupItem();
}
