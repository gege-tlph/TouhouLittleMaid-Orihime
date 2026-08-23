package com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz;

import cn.sh1rocu.touhoulittlemaid.util.compat.tacz.ItemHandlerUtil;
import cn.sh1rocu.touhoulittlemaid.util.itemhandler.IItemHandler;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.ammo.AmmoSource;
import com.tacz.guns.api.item.ammo.AmmoSourceRegistry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 把女仆背包接成 TaCZ 的弹药来源，走 {@code 1.21.11_R2} 起提供的官方 {@code AmmoSource} API。
 *
 * <p>与 {@code mixin/compat/tacz/} 那四个 mixin **是同一件事的两种实现，按装着的 TaCZ 版本二选一**
 * （判据见 {@link cn.sh1rocu.touhoulittlemaid.util.compat.tacz.TaczAmmoSourceApi}，
 * mixin 那侧由 {@code MixinPlugin.shouldApplyMixin} 用同一个判据关掉）。行为逐条对应：</p>
 *
 * <table border="1">
 *   <caption>旧 mixin 靶点 → 新 API</caption>
 *   <tr><td>{@code AbstractGunItem.canReload}</td><td rowspan="4">{@link #hasAmmo}</td></tr>
 *   <tr><td>{@code AbstractGunItem.hasInventoryAmmo}</td></tr>
 *   <tr><td>{@code ModernKineticGunScriptAPI.hasAmmoToConsume}</td></tr>
 *   <tr><td>{@code GunAnimationStateContext.lambda$hasAmmoToConsume$8}（换弹动画，客户端）</td></tr>
 *   <tr><td>{@code LivingEntityShoot.consumeAmmoFromPlayer}</td><td rowspan="2">{@link #consumeAmmo}</td></tr>
 *   <tr><td>{@code ModernKineticGunScriptAPI.consumeAmmoFromPlayer}</td></tr>
 * </table>
 *
 * <p><b>本类必须与 {@link TacCompat} 分开放</b>：{@code TacCompat.isGun} 这类无条件路径会让
 * {@code TacCompat} 在没装 TaCZ 时也被类加载，而本类实现了 {@code AmmoSource}——同样的加载
 * 会直接 {@code NoClassDefFoundError}。它只在 {@code TacCompat.init()} 的判据成立时才被触及，
 * 与既有的 {@code TacInnerCompat} 同一惯例。</p>
 */
final class MaidAmmoSource implements AmmoSource {
    private static final MaidAmmoSource INSTANCE = new MaidAmmoSource();

    private MaidAmmoSource() {
    }

    /**
     * 由 {@link TacCompat#init()} 在确认装着的 TaCZ 有这套 API 之后调用。
     *
     * <p>登记在公共入口即可覆盖两个逻辑端：换弹动画那条判定跑在客户端，而
     * {@code TacCompat.init()} 经 {@code CommonRegistry.onSetupEvent → TaskManager.init()}
     * 在两端都会执行。</p>
     */
    static void register() {
        AmmoSourceRegistry.EVENT.register(MaidAmmoSource::findFor);
    }

    /**
     * provider 语义：不适用时返回 null，**首个非 null 生效**，全员 null 则回落 TaCZ 自己的
     * 实体物品栏来源。故这里只认女仆，其余一律让路——这与旧 mixin 里
     * {@code if (shooter instanceof EntityMaid)} 之外不动 {@code cir} 的写法等价。
     */
    @Nullable
    private static AmmoSource findFor(LivingEntity shooter, ItemStack gunItem) {
        return shooter instanceof EntityMaid ? INSTANCE : null;
    }

    /**
     * 只读判定，必须与 {@link #consumeAmmo} 的判据一致——不一致就会出现「换弹动画播了却抠不到弹」。
     * 两侧同为「散装弹药 {@link IAmmo} 或弹药盒 {@link IAmmoBox}」，与四个 mixin 里那段循环逐字同义。
     */
    @Override
    public boolean hasAmmo(LivingEntity shooter, ItemStack gunItem) {
        if (!(shooter instanceof EntityMaid maid)) {
            return false;
        }
        IItemHandler inv = maid.getAllInv();
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack checkAmmoStack = inv.getStackInSlot(i);
            if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(gunItem, checkAmmoStack)) {
                return true;
            }
            if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(gunItem, checkAmmoStack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回**实际抠到**的数量（0..requestedAmount）。{@link ItemHandlerUtil#findAndExtractInventoryAmmo}
     * 返回的正是 {@code needAmmoCount - 剩余}，语义已经对上，故此处不再换算——
     * 它也正是两个 {@code consumeAmmoFromPlayer} mixin 调的那一个方法。
     */
    @Override
    public int consumeAmmo(LivingEntity shooter, ItemStack gunItem, int requestedAmount) {
        if (!(shooter instanceof EntityMaid maid)) {
            return 0;
        }
        return ItemHandlerUtil.findAndExtractInventoryAmmo(maid.getAllInv(), gunItem, requestedAmount);
    }
}
