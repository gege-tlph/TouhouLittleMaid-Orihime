package com.github.tartaricacid.touhoulittlemaid.compat.refurbishedfurniture.chest;

import com.github.tartaricacid.touhoulittlemaid.api.bauble.IChestType;
import com.github.tartaricacid.touhoulittlemaid.compat.refurbishedfurniture.RefurbishedFurnitureCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;

/**
 * 让无线 IO 能绑定「家具重制」的储物方块。
 * <p>
 * 它们的方块实体继承 {@code RandomizableContainerBlockEntity} 并实现 {@code WorldlyContainer}，
 * 因此 Fabric 传输 API 的通用 {@code Container} 回退已经能给出可用的 {@code ItemStorage.SIDED}；
 * 唯一缺的一环就是 {@link IChestType#isChest}。
 * <p>
 * <b>这里按方块实体类型 id 白名单匹配，而不是按类匹配</b>：该模组只发布在 CurseForge，
 * 没有可用的 Modrinth 坐标，引入 typed adapter 就得把 CurseForge 的不透明 fileId
 * 钉进 build.gradle（还要连带钉住它的硬前置 framework）。id 白名单零依赖、天然可插拔，
 * 而且判据仍然带结构校验（必须真是 {@code BaseContainerBlockEntity}），不是靠名字猜语义。
 * <p>
 * 一个方块实体类型覆盖它整个家族（木种 / 颜色变体共用一个类型），所以列表短且
 * 新变体自动纳入。
 * <p>
 * <b>白名单而非黑名单，是因为漏判的方向必须是安全的：</b>
 * 该模组的 {@code recycle_bin} 会销毁放进去的物品，{@code mail_box} / {@code post_box}
 * 属于寄件系统，{@code stove} / {@code grill} / {@code microwave} / {@code toaster} /
 * {@code freezer} / {@code workbench} 都是带加工语义的机器。用黑名单的话，
 * 该模组以后新增的任何机器都会默认被当成箱子，女仆会把背包倒进加工槽。
 */
public final class RefurbishedStorageChestType implements IChestType {
    /**
     * 纯储物的方块实体类型。字段名与 id 不一致的只有储物柜：{@code STORAGE_CABINET} 注册为 {@code cabinet}。
     */
    private static final Set<Identifier> STORAGE_BLOCK_ENTITY_TYPES = Set.of(
            type("crate"),
            type("drawer"),
            type("kitchen_drawer"),
            type("cabinet"),
            type("cooler"),
            type("fridge"),
            type("storage_jar")
    );

    private static Identifier type(String path) {
        return Identifier.fromNamespaceAndPath(RefurbishedFurnitureCompat.MOD_ID, path);
    }

    @Override
    public boolean isChest(BlockEntity chest) {
        if (!(chest instanceof BaseContainerBlockEntity)) {
            return false;
        }
        Identifier id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(chest.getType());
        return id != null && STORAGE_BLOCK_ENTITY_TYPES.contains(id);
    }

    @Override
    public boolean canOpenByPlayer(BlockEntity chest, Player player) {
        return isChest(chest) && ((BaseContainerBlockEntity) chest).canOpen(player);
    }

    @Override
    public int getOpenCount(BlockGetter level, BlockPos pos, BlockEntity chest) {
        // 这些储物方块没有原版箱子那种共享打开计数，与木桶、零食柜同样直接放行
        return ALLOW_COUNT;
    }
}
