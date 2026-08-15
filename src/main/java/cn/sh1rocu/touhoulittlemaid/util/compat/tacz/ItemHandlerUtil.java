package cn.sh1rocu.touhoulittlemaid.util.compat.tacz;

import cn.sh1rocu.touhoulittlemaid.util.transfer.ItemUtil;
import com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil;
import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IAmmo;
import com.tacz.guns.api.item.IAmmoBox;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.minecraft.world.item.ItemStack;

/**
 * 从女仆背包里查找并抠除枪械弹药（TACZ mixin 层专用）。
 *
 * <p>行为照 origin/1.21.1 的同名件；容器 API 按宿主的 Fabric transfer 形态重写：
 * {@code getSlots()/getStackInSlot()} → {@code getSlotCount()/ItemUtil.getStack()}，
 * 抽取走 {@code ItemsUtil.extractItem}。⚠️ 关键差异：Forge 的 {@code getStackInSlot}
 * 返回活引用（origin 对弹药盒直接原地改 NBT），而 {@code ItemUtil.getStack} 是
 * {@code variant.toStack()} 的**拷贝**——改完弹药盒必须 {@code setStackInSlot} 回写，
 * 否则消耗静默丢失。</p>
 *
 * <p>本类引用 TACZ 类（compileOnly），仅由 isModLoaded 门控的 tacz mixin 调用，
 * 未装 TACZ 时不会被类加载。</p>
 */
public class ItemHandlerUtil {
    public static int findAndExtractInventoryAmmo(SlottedStorage<ItemVariant> itemHandler, ItemStack gunItem, int needAmmoCount) {
        int cnt = needAmmoCount;
        // 背包检查
        for (int i = 0; i < itemHandler.getSlotCount(); i++) {
            ItemStack checkAmmoStack = ItemUtil.getStack(itemHandler, i);
            if (checkAmmoStack.getItem() instanceof IAmmo iAmmo && iAmmo.isAmmoOfGun(gunItem, checkAmmoStack)) {
                ItemStack extractItem = ItemsUtil.extractItem(itemHandler, i, cnt, false, null);
                cnt = cnt - extractItem.getCount();
                if (cnt <= 0) {
                    break;
                }
            }
            if (checkAmmoStack.getItem() instanceof IAmmoBox iAmmoBox && iAmmoBox.isAmmoBoxOfGun(gunItem, checkAmmoStack)) {
                int boxAmmoCount = iAmmoBox.getAmmoCount(checkAmmoStack);
                int extractCount = Math.min(boxAmmoCount, cnt);
                int remainCount = boxAmmoCount - extractCount;
                iAmmoBox.setAmmoCount(checkAmmoStack, remainCount);
                if (remainCount <= 0) {
                    iAmmoBox.setAmmoId(checkAmmoStack, DefaultAssets.EMPTY_AMMO_ID);
                }
                ItemsUtil.setStackInSlot(itemHandler, i, checkAmmoStack);
                cnt = cnt - extractCount;
                if (cnt <= 0) {
                    break;
                }
            }
        }
        return needAmmoCount - cnt;
    }
}
