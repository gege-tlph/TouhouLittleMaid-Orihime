package com.github.tartaricacid.touhoulittlemaid.util;

import cn.sh1rocu.touhoulittlemaid.util.transfer.ItemStackStorage;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.SingleFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;

/**
 * 液体背包的桶↔储罐交互，自行为基准搬入。
 *
 * <p>写法对新基：基准的 {@code IItemHandler}/{@code ItemHandlerHelper.insertItemStacked}
 * （Forge 形态）换成宿主 transfer 的 {@code SlottedStorage<ItemVariant>} 与
 * {@link ItemsUtil#insertItemStacked}；流体侧本就是 Fabric transfer，原样保留。
 * 空容器（倒空的桶）塞进女仆背包，塞不下的部分由 insertItemStacked 落地。</p>
 */
public class MaidFluidUtil {
    public static long tankToBucket(ItemStack bucket, SingleFluidStorage tank, SlottedStorage<ItemVariant> maidBackpack) {
        if (bucket.isEmpty()) {
            return 0;
        }

        ContainerItemContext context = ContainerItemContext.ofSingleSlot(new ItemStackStorage(bucket));
        Storage<FluidVariant> bucketStorage = context.find(FluidStorage.ITEM);
        if (bucketStorage == null) {
            return 0;
        }
        if (tank.isResourceBlank()) {
            return 0;
        }

        try (Transaction tx = Transaction.openOuter()) {
            long result = StorageUtil.move(tank, bucketStorage, v -> !v.isBlank(), tank.getCapacity(), tx);
            if (result > 0) {
                ItemsUtil.insertItemStacked(maidBackpack, context.getItemVariant().toStack(), false, tx);
                bucket.shrink(1);
                tx.commit();
                return result;
            }
            return 0;
        }
    }

    public static long bucketToTank(ItemStack bucket, SingleFluidStorage tank, SlottedStorage<ItemVariant> maidBackpack) {
        if (bucket.isEmpty()) {
            return 0;
        }

        ContainerItemContext context = ContainerItemContext.ofSingleSlot(new ItemStackStorage(bucket));
        Storage<FluidVariant> bucketStorage = context.find(FluidStorage.ITEM);
        if (bucketStorage == null) {
            return 0;
        }

        try (Transaction tx = Transaction.openOuter()) {
            long result = StorageUtil.move(bucketStorage, tank, v -> !v.isBlank(), tank.getCapacity(), tx);
            if (result > 0) {
                ItemsUtil.insertItemStacked(maidBackpack, context.getItemVariant().toStack(), false, tx);
                bucket.shrink(1);
                tx.commit();
                return result;
            }
            return 0;
        }
    }
}
