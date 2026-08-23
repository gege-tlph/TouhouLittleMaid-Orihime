package com.github.tartaricacid.touhoulittlemaid.compat.gun.tacz;

import cn.sh1rocu.touhoulittlemaid.util.compat.tacz.ItemHandlerUtil;
import cn.sh1rocu.touhoulittlemaid.util.transfer.ItemUtil;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import com.tacz.guns.api.item.ammo.AmmoSource;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 把女仆背包接成 TaCZ 的弹药来源。
 *
 * <p>取代此前的四个 mixin（{@code AbstractGunItemMixin} / {@code LivingEntityShootMixin} /
 * {@code ModernKineticGunScriptAPIMixin} / {@code GunAnimationStateContextMixin}）。
 * 上游 PR #48 在 {@code 26.1.2_R2} 提供了官方 {@code AmmoSource} API，
 * 而**那四个注入锚点在 R2 里一个不剩**（R1/R2 双 jar javap 实证：
 * {@code tacz$getItemHandler} 由 2/1/2 处变 0，{@code lambda$hasAmmoToConsume$0} 由 1 处变 0）。
 * 因为 {@code touhou_little_maid_fabric.mixins.json} 是 {@code "required": true}，
 * 留着旧 mixin 上 R2 不是「兼容退化」而是**启动崩溃**——两版互斥，不存在同时兼容的写法。</p>
 *
 * <p>R2 里那四条路径逐条改调 {@code AmmoSourceRegistry}（javap -c 实证 7 个调用点，
 * 非空壳）：{@code AbstractGunItem.canReload} / {@code hasInventoryAmmo} 与
 * {@code ModernKineticGunScriptAPI.hasAmmoToConsume} 走 {@code hasAmmo}；
 * {@code LivingEntityShoot.consumeAmmoFromPlayer} 与
 * {@code ModernKineticGunScriptAPI.consumeAmmoFromPlayer} 走 {@code consumeAmmo}；
 * 客户端动画那条由匿名 lambda 提成具名 {@code GunAnimationStateContext.hasAmmoToConsumeInEntity}。
 * 与我们四个 mixin 的靶点一一对应，无遗漏、无新增。</p>
 *
 * <p><b>本类必须与 {@code TacCompat} 分开</b>：{@code TacCompat} 会被 {@code isGun} 等
 * 无条件路径加载，而本类持有 {@code AmmoSource} 的实现关系，未装 TaCZ 时一经加载即
 * {@code NoClassDefFoundError}。只在 {@code TacCompat.init()} 的 isModLoaded 守卫内被触及，
 * 与既有的 {@code TacInnerCompat} 同一惯例。</p>
 */
final class MaidAmmoSource implements AmmoSource {
    private static final MaidAmmoSource INSTANCE = new MaidAmmoSource();

    private MaidAmmoSource() {
    }

    /**
     * provider：不适用时返回 null，**首个非 null 生效**，全员 null 则回落 TaCZ 自己的
     * {@code ENTITY_INVENTORY}（原实体物品栏）。故这里只认女仆，其余一律让路。
     */
    @Nullable
    static AmmoSource findFor(LivingEntity shooter, ItemStack gunItem) {
        return shooter instanceof EntityMaid ? INSTANCE : null;
    }

    /**
     * 只读，且必须与 {@link #consumeAmmo} 判据一致——两者不一致会让换弹动画播了却抠不到弹。
     * 判据与抠除侧同为「散装弹药 {@link IAmmo} 或弹药盒 {@link IAmmoBox}」。
     */
    @Override
    public boolean hasAmmo(LivingEntity shooter, ItemStack gunItem) {
        if (!(shooter instanceof EntityMaid maid)) {
            return false;
        }
        SlottedStorage<ItemVariant> inv = maid.getAllInv();
        for (int i = 0; i < inv.getSlotCount(); i++) {
            ItemStack stack = ItemUtil.getStack(inv, i);
            if (stack.getItem() instanceof IAmmo ammo && ammo.isAmmoOfGun(gunItem, stack)) {
                return true;
            }
            if (stack.getItem() instanceof IAmmoBox box && box.isAmmoBoxOfGun(gunItem, stack)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回**实际抠到**的数量（0..requestedAmount）。{@code ItemHandlerUtil} 返回的正是
     * {@code needAmmoCount - 剩余}，语义已对上；调用方 {@code AmmoSourceRegistry.consumeAmmo}
     * 还会再夹一次 {@code max(0, min(result, requested))}（javap -c 实证）。
     */
    @Override
    public int consumeAmmo(LivingEntity shooter, ItemStack gunItem, int requestedAmount) {
        if (!(shooter instanceof EntityMaid maid)) {
            return 0;
        }
        return ItemHandlerUtil.findAndExtractInventoryAmmo(maid.getAllInv(), gunItem, requestedAmount);
    }
}
