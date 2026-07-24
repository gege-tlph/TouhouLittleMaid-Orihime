package com.github.tartaricacid.touhoulittlemaid.mixin;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 恢复女仆使用的非玩家护盾耐久路径。
 */
@Mixin(BlocksAttacks.class)
public abstract class BlocksAttacksMixin {
    @Inject(method = "hurtBlockingItem", at = @At("HEAD"))
    private void touhouLittleMaid$hurtMaidBlockingItem(Level level, ItemStack stack, LivingEntity entity,
                                                        InteractionHand hand, float blockedDamage,
                                                        CallbackInfo ci) {
        if (entity instanceof EntityMaid maid) {
            int durabilityDamage = ((BlocksAttacks) (Object) this).itemDamage().apply(blockedDamage);
            if (durabilityDamage > 0) {
                stack.hurtAndBreak(durabilityDamage, maid, hand.asEquipmentSlot());
            }
        }
    }
}
