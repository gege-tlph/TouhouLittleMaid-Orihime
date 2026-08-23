package com.github.tartaricacid.touhoulittlemaid.compat.refurbishedfurniture;

import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder;
import net.minecraft.core.registries.Registries;

/**
 * Self-contained Fabric extension entrypoint for MrCrayfish's Furniture Mod: Refurbished.
 * Core code only discovers the standard little_maid_extension contract and
 * never names this compatibility module directly.
 * <p>
 * 家具本身（桌子、书桌、坐具、厨房台面）走纯数据的避让 / 禁跳 / 零食台标签，见
 * {@code TagBlock}。储物方块不再需要专门的箱子类型：无线 IO 已改为对任意暴露
 * {@code ItemStorage.SIDED} 的容器方块实体通用绑定（与 26.1.2 对齐），本类只保留
 * 装了该模组时把配方序列化器注册表标记为 SYNCED 的职责（见 {@link #init()}）。
 */
public final class RefurbishedFurnitureCompat implements ILittleMaid {
    public static final String MOD_ID = "refurbished_furniture";

    public RefurbishedFurnitureCompat() {
    }

    /**
     * 让 {@code minecraft:recipe_serializer} 参与注册表同步。<b>只在装了该模组时生效。</b>
     * <p>
     * 家具重制的 {@code MessageWorkbench$SyncRecipes} 用原版 {@code RecipeHolder.STREAM_CODEC}
     * 整批同步工作台配方，而那个 codec <b>按数字注册 id 分派序列化器</b>。
     * 1.21.11 的原版已经不再向客户端发配方，所以这张表默认不同步；
     * 而专服与客户端本来就装着不同的 mod（JEI / Sodium / Iris 是客户端专属），
     * 注册顺序必然不同——实测同一轮里 38 项中有 16 项对不上，
     * 于是分歧点之后每个配方都用错的序列化器解码，客户端一进服就被踢。
     * <p>
     * 标记 {@code SYNCED} 后，Fabric 会把服务端的「id ↔ 名字」表发给客户端并<b>重映射客户端的 id</b>，
     * 两端就此对齐。这修的是<b>缺陷本身所属的那一类</b>（一张会上网的表没有被同步），
     * 而不是给家具重制的代码打补丁，所以不与「不在 TLM 里替第三方兜底」冲突。
     * <p>
     * 上游已有同一问题的 issue（MrCrayfish/MrCrayfishFurnitureMod-Refurbished #186，
     * 2026-02 至今未修）。上游修好后本方法即可删除。
     */
    public static void init() {
        RegistryAttributeHolder.get(Registries.RECIPE_SERIALIZER).addAttribute(RegistryAttribute.SYNCED);
    }
}
