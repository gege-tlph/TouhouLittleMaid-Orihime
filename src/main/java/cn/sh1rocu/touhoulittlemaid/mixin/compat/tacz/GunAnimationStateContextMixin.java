package cn.sh1rocu.touhoulittlemaid.mixin.compat.tacz;

import cn.sh1rocu.touhoulittlemaid.util.transfer.ItemUtil;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 客户端动画状态机的「有无弹药可消耗」判定看见女仆背包（决定换弹动画播不播）。
 *
 * <p>26.1.2 jar 实查漂移：目标 lambda 由 1.21.11 的 {@code lambda$hasAmmoToConsume$8}
 * 变为 {@code lambda$hasAmmoToConsume$0}（按签名 {@code (Entity)->Boolean} 对号，
 * javap 复验该类恰有两个 hasAmmoToConsume lambda：$0 吃 Entity、$1 吃 IItemHandler）。</p>
 */
@Mixin(GunAnimationStateContext.class)
public class GunAnimationStateContextMixin {
    @Shadow
    private ItemStack currentGunItem;

    @Inject(
            remap = false,
            method = "lambda$hasAmmoToConsume$0",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tlm$hasAmmoToConsume(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof EntityMaid maid) {
            var cap = maid.getAllInv();
            // 背包检查
            for (int i = 0; i < cap.getSlotCount(); i++) {
                ItemStack checkAmmoStack = ItemUtil.getStack(cap, i);
                if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(this.currentGunItem, checkAmmoStack)) {
                    cir.setReturnValue(true);
                    return;
                }
                if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(this.currentGunItem, checkAmmoStack)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }
}
