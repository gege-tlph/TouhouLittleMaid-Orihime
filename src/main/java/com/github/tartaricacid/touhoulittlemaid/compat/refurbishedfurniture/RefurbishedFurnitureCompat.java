package com.github.tartaricacid.touhoulittlemaid.compat.refurbishedfurniture;

import net.fabricmc.fabric.api.event.registry.RegistryAttribute;
import net.fabricmc.fabric.api.event.registry.RegistryAttributeHolder;
import net.minecraft.core.registries.Registries;

/**
 * MrCrayfish's Furniture Mod: Refurbished 的可选兼容。
 * <p>
 * 家具本身（桌子、书桌、坐具、厨房台面）走纯数据的避让 / 禁跳 / 零食台标签，见
 * {@code TagBlock#addRefurbishedFurniture}；本类只处理一件代码层的事——注册表同步。
 * <p>
 * <b>本类刻意不实现 {@code ILittleMaid}</b>：行为基准在那个接口上覆写了
 * {@code addChestType} 来让无线 IO 能绑定该模组的储物家具，而代码宿主 origin/26.1
 * 把 {@code IChestType} / {@code ChestManager} 整套白名单机制删掉了
 * （四棵树实查：上游 8 个文件、行为基准 8 个，宿主与本树 0），改由
 * {@code ItemWirelessIO} 直接走 Fabric 的 {@code ItemStorage.SIDED} 平台通用查找。
 * 该模组的储物方块继承 {@code RandomizableContainerBlockEntity} 并实现
 * {@code WorldlyContainer}，本来就能被通用查找拿到可用的 storage——白名单曾是唯一缺的那一环，
 * 而这里没有白名单，所以<b>储物那一半在本树无需任何代码</b>。
 * 用户 2026-08-19 已就「白名单换成平台通用查找」裁决为可接受（跨版本 wiki 的 I9）。
 */
public final class RefurbishedFurnitureCompat {
    public static final String MOD_ID = "refurbished_furniture";

    private RefurbishedFurnitureCompat() {
    }

    /**
     * 让 {@code minecraft:recipe_serializer} 参与注册表同步。<b>只在装了该模组时被调用。</b>
     * <p>
     * 家具重制用原版 {@code RecipeHolder.STREAM_CODEC} 整批同步它的工作台配方，而那条 codec
     * <b>按数字注册 id 分派序列化器</b>——26.1.2 字节码实证：{@code Recipe.STREAM_CODEC} 的静态
     * 初始化在 offset 38 取 {@code ByteBufCodecs.registry(Registries.RECIPE_SERIALIZER)} 再
     * {@code dispatch}，写上线的是 varint 数字 id 而不是名字。
     * <p>
     * 而这张表默认不同步（原版自己早已不再向客户端发配方，没人需要它对齐），
     * 专服与客户端又本来就装着不同的 mod（JEI / Sodium / Iris 是客户端专属），
     * 注册顺序必然不同。行为基准侧实测同一轮 38 项里有 16 项对不上，
     * 于是分歧点之后每个配方都用错的序列化器解码，客户端一进服就被踢。
     * <p>
     * 标记 {@code SYNCED} 后 Fabric 会把服务端的「id ↔ 名字」表发给客户端并<b>重映射客户端的 id</b>，
     * 两端就此对齐（基准侧同样的 mod 集合复测：0/38 不一致）。这修的是<b>缺陷本身所属的那一类</b>
     * ——一张会上网的表没有被同步——而不是给家具重制的代码打补丁。
     * <p>
     * <b>代价照实写下</b>：注册表同步后，服务端若持有客户端没有的配方序列化器，会<b>显式断开</b>
     * 该客户端并点名缺失项，而在此之前是静默错解码。{@code RegistryAttribute.OPTIONAL} 救不了这个
     * ——它只覆盖「整张表缺席」，不覆盖「表里缺条目」。
     * <p>
     * 上游已有同一问题的 issue（MrCrayfish/MrCrayfishFurnitureMod-Refurbished #186，
     * 2026-02 提出，行为基准落地时未修）。上游修好后本方法即可删除。
     */
    public static void init() {
        RegistryAttributeHolder.get(Registries.RECIPE_SERIALIZER).addAttribute(RegistryAttribute.SYNCED);
    }
}
