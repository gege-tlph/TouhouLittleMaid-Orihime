package com.github.tartaricacid.touhoulittlemaid.entity.backpack;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.IMaidBackpack;
import com.github.tartaricacid.touhoulittlemaid.api.backpack.MaidBackpackRenderData;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.github.tartaricacid.touhoulittlemaid.api.backpack.MaidBackpackRenderData.EMPTY;

public class BackpackManager {
    /**
     * 渲染数据，客户端才能用
     */
    public static final Function<Identifier, MaidBackpackRenderData> RENDER_DATA_CACHE = Util.memoize(id ->
            findBackpack(id).map(IMaidBackpack::getRenderData).orElse(EMPTY)
    );
    private static Map<Identifier, IMaidBackpack> BACKPACK_ID_MAP;
    private static Map<Item, IMaidBackpack> BACKPACK_ITEM_MAP;
    @Environment(EnvType.CLIENT)
    private static Map<Identifier, Pair<EntityModel<?>, Identifier>> BACKPACK_MODEL_MAP;
    private static IMaidBackpack EMPTY_BACKPACK;

    private BackpackManager() {
        EMPTY_BACKPACK = new EmptyBackpack();
        BACKPACK_ID_MAP = Maps.newHashMap();
        BACKPACK_ITEM_MAP = Maps.newHashMap();
    }

    public static void init() {
        BackpackManager manager = new BackpackManager();
        manager.add(EMPTY_BACKPACK);
        manager.add(new SmallBackpack());
        manager.add(new MiddleBackpack());
        manager.add(new BigBackpack());
        manager.add(new CraftingTableBackpack());
        manager.add(new EnderChestBackpack());
        manager.add(new FurnaceBackpack());
        manager.add(new TankBackpack());
        // SWEEP R9-1（2026-07-19）：原「EXTENSIONS not available (26.1 feature)」TODO 系误判——
        // TouhouLittleMaid.EXTENSIONS(:21) 本树存在且他处在用；还原 origin 的 addon 扩展点循环
        for (ILittleMaid littleMaid : TouhouLittleMaid.EXTENSIONS) {
            littleMaid.addMaidBackpack(manager);
        }
        BACKPACK_ID_MAP = ImmutableMap.copyOf(BACKPACK_ID_MAP);
        // 将物品和背包绑定
        BACKPACK_ID_MAP.forEach((id, backpack) -> BACKPACK_ITEM_MAP.put(backpack.getItem(), backpack));
        BACKPACK_ITEM_MAP = ImmutableMap.copyOf(BACKPACK_ITEM_MAP);
    }

    @Environment(EnvType.CLIENT)
    public static void initClient(EntityModelSet modelSet) {
        // 有些模组可能会比上面 init 还要早执行这块，所以需要检查一下？
        if (BACKPACK_ID_MAP == null) {
            init();
        }
        BACKPACK_MODEL_MAP = Maps.newHashMap();
        // 显式循环，不用 forEach(lambda)：给方法标 @Environment(CLIENT) 拦不住 lambda——
        // 编译器会为它生成**不带注解的合成方法**，而该合成方法的描述符里带着 EntityModelSet。
        // Fabric 的客户端剥离按注解走，剥不掉合成方法，于是客户端类型留在了专服可见的签名里，
        // 被反射扫描到就是 NoClassDefFoundError。与祭坛/雕像/手办那三个方块的粒子效果方法同一个缺陷
        // （见 `97380380b`），只是当初那轮扫描漏了这一处。
        for (Map.Entry<Identifier, IMaidBackpack> entry : BACKPACK_ID_MAP.entrySet()) {
            IMaidBackpack backpack = entry.getValue();
            BACKPACK_MODEL_MAP.put(entry.getKey(),
                    Pair.of(backpack.getBackpackModel(modelSet), backpack.getBackpackTexture()));
        }
        BACKPACK_MODEL_MAP = ImmutableMap.copyOf(BACKPACK_MODEL_MAP);
    }

    public static IMaidBackpack getEmptyBackpack() {
        return EMPTY_BACKPACK;
    }

    public static Optional<IMaidBackpack> findBackpack(Identifier id) {
        return Optional.ofNullable(BACKPACK_ID_MAP.get(id));
    }

    public static Optional<IMaidBackpack> findBackpack(ItemStack stack) {
        return Optional.ofNullable(BACKPACK_ITEM_MAP.get(stack.getItem()));
    }

    public static void addBackpackCooldown(Player player) {
        for (Item backpack : BACKPACK_ITEM_MAP.keySet()) {
            player.getCooldowns().addCooldown(backpack.getDefaultInstance(), 20);
        }
    }

    @Environment(EnvType.CLIENT)
    public static Optional<Pair<EntityModel<?>, Identifier>> findBackpackModel(Identifier id) {
        Pair<EntityModel<?>, Identifier> pair = BACKPACK_MODEL_MAP.get(id);
        if (pair == null) {
            return Optional.empty();
        }
        if (pair.getLeft() == null) {
            return Optional.empty();
        }
        return Optional.of(pair);
    }

    public void add(IMaidBackpack backpack) {
        BACKPACK_ID_MAP.put(backpack.getId(), backpack);
    }
}