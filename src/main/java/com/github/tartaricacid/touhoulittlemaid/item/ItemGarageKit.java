package com.github.tartaricacid.touhoulittlemaid.item;

import com.github.tartaricacid.touhoulittlemaid.client.proxy.ItemGarageKitProxy;

import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import com.github.tartaricacid.touhoulittlemaid.compat.ysm.YsmCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.YsmMaidInfo;
import com.github.tartaricacid.touhoulittlemaid.util.ParseI18n;
import com.mojang.serialization.Codec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Objects;

import static com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent.ENTITY_ID_TAG_NAME;
import static com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent.MODEL_ID_TAG_NAME;

public class ItemGarageKit extends BlockItem {
    private static final String DEFAULT_ENTITY_ID = "touhou_little_maid:maid";
    private static final String DEFAULT_MODEL_ID = "touhou_little_maid:hakurei_reimu";
    private static final CustomData DEFAULT_DATA = getDefaultData();

    public ItemGarageKit(Identifier id) {
        super(InitBlocks.GARAGE_KIT, (new Item.Properties()).setId(ResourceKey.create(Registries.ITEM, id)).useBlockDescriptionPrefix().stacksTo(1));
    }

    public static CustomData getMaidData(ItemStack stack) {
        return Objects.requireNonNullElse(stack.get(InitDataComponent.MAID_INFO), DEFAULT_DATA);
    }

    private static CustomData getDefaultData() {
        CompoundTag data = new CompoundTag();
        data.putString(ENTITY_ID_TAG_NAME, DEFAULT_ENTITY_ID);
        data.putString(MODEL_ID_TAG_NAME, DEFAULT_MODEL_ID);
        // 默认数据需要强制指定 YSM 渲染为空
        data.putBoolean(EntityMaid.IS_YSM_MODEL_TAG, false);
        return CustomData.of(data);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            Component name = ItemGarageKitProxy.getName(stack);
            if (name != null) {
                return name;
            }
        }
        return super.getName(stack);
    }


}
