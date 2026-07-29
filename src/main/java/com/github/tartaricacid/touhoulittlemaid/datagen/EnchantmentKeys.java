package com.github.tartaricacid.touhoulittlemaid.datagen;

import com.github.tartaricacid.touhoulittlemaid.datagen.tag.TagItem;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;

import static com.github.tartaricacid.touhoulittlemaid.util.ResourceLocationUtil.getResourceLocation;

public class EnchantmentKeys {
    public static final ResourceKey<Enchantment> IMPEDING = registerKey("impeding");
    public static final ResourceKey<Enchantment> SPEEDY = registerKey("speedy");
    public static final ResourceKey<Enchantment> ENDERS_ENDER = registerKey("enders_ender");

    private static ResourceKey<Enchantment> registerKey(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, getResourceLocation(name));
    }

    // B4_DATAGEN_RESTORE 已恢复（Phase 2 datagen 解冻）：Enchantment.Builder API 在 1.21.11 未变（definition/
    //   dynamicCost/constantCost/build 签名一致），仅 ResourceKey.location() → identifier()。bootstrap 只在 runData 跑。
    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<DamageType> damageTypes = context.lookup(Registries.DAMAGE_TYPE);
        HolderGetter<Enchantment> enchantments = context.lookup(Registries.ENCHANTMENT);
        HolderGetter<Item> items = context.lookup(Registries.ITEM);
        HolderGetter<Block> blocks = context.lookup(Registries.BLOCK);

        register(context, IMPEDING, new Enchantment.Builder(
                Enchantment.definition(
                        items.getOrThrow(TagItem.GOHEI_ENCHANTABLE),
                        5,
                        4,
                        Enchantment.dynamicCost(10, 8),
                        Enchantment.dynamicCost(15, 8),
                        1,
                        EquipmentSlotGroup.MAINHAND
                )
        ));

        register(context, SPEEDY, new Enchantment.Builder(
                Enchantment.definition(
                        items.getOrThrow(TagItem.GOHEI_ENCHANTABLE),
                        2,
                        2,
                        Enchantment.dynamicCost(25, 10),
                        Enchantment.dynamicCost(30, 10),
                        2,
                        EquipmentSlotGroup.MAINHAND
                )
        ));

        register(context, ENDERS_ENDER, new Enchantment.Builder(
                Enchantment.definition(
                        items.getOrThrow(TagItem.GOHEI_ENCHANTABLE),
                        1,
                        1,
                        Enchantment.constantCost(20),
                        Enchantment.constantCost(50),
                        4,
                        EquipmentSlotGroup.MAINHAND
                )
        ));
    }

    private static void register(BootstrapContext<Enchantment> context, ResourceKey<Enchantment> key, Enchantment.Builder builder) {
        context.register(key, builder.build(key.identifier()));
    }

    public static int getEnchantmentLevel(RegistryAccess access, ResourceKey<Enchantment> enchantmentResourceKey, ItemStack mainHandItem) {
        return EnchantmentHelper.getItemEnchantmentLevel(getEnchantmentHolder(access, enchantmentResourceKey), mainHandItem);
    }

    public static Holder<Enchantment> getEnchantmentHolder(RegistryAccess access, ResourceKey<Enchantment> enchantmentResourceKey) {
        // B4: 1.21.11 运行时 API 迁移（javap 确认）：
        //   RegistryAccess.registryOrThrow(key) → lookupOrThrow(key)（返回 Registry<E>）
        //   Registry.getHolderOrThrow(key) → getOrThrow(key)（继承自 HolderGetter，返回 Holder.Reference）
        return access.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(enchantmentResourceKey);
    }
}
