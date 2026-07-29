package com.github.tartaricacid.touhoulittlemaid.client.proxy;

import com.github.tartaricacid.touhoulittlemaid.client.resource.loader.CustomPackLoader;
import com.github.tartaricacid.touhoulittlemaid.client.resource.pojo.MaidModelInfo;
import com.github.tartaricacid.touhoulittlemaid.compat.ysm.YsmCompat;
import com.github.tartaricacid.touhoulittlemaid.item.ItemGarageKit;
import com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.YsmMaidInfo;
import com.github.tartaricacid.touhoulittlemaid.util.ParseI18n;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import javax.annotation.Nullable;

import static com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent.ENTITY_ID_TAG_NAME;
import static com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent.MODEL_ID_TAG_NAME;

/** Client-only name resolver kept behind ItemGarageKit's environment guard. */
public final class ItemGarageKitProxy {
    private static final String DEFAULT_ENTITY_ID = "touhou_little_maid:maid";
    private static final String DEFAULT_MODEL_ID = "touhou_little_maid:hakurei_reimu";

    private ItemGarageKitProxy() {
    }

    @Nullable
    public static Component getName(ItemStack stack) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return null;
        }

        CustomData data = ItemGarageKit.getMaidData(stack);
        CompoundTag tag = data.copyTag();
        MutableComponent prefix = Component.translatable("block.touhou_little_maid.garage_kit.prefix");

        String entityId = tag.getStringOr(ENTITY_ID_TAG_NAME, DEFAULT_ENTITY_ID);
        if (!DEFAULT_ENTITY_ID.equals(entityId)) {
            return BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(entityId))
                    .<Component>map(EntityType::getDescription)
                    .map(prefix::append)
                    .orElse(null);
        }

        YsmMaidInfo ysmInfo = YsmCompat.getYsmMaidInfo(tag);
        if (ysmInfo.isYsmModel()) {
            MutableComponent ysmName = null;
            if (!ysmInfo.name().isBlank()) {
                ysmName = ComponentSerialization.CODEC
                        .parse(minecraft.level.registryAccess().createSerializationContext(JsonOps.INSTANCE),
                                JsonParser.parseString(ysmInfo.name()))
                        .result()
                        .map(Component::copy)
                        .orElse(null);
            }
            return prefix.append(ysmName == null || ysmName.equals(Component.empty())
                    ? Component.literal(ysmInfo.modelId()) : ysmName);
        }

        String modelId = tag.getStringOr(MODEL_ID_TAG_NAME, DEFAULT_MODEL_ID);
        MaidModelInfo info = CustomPackLoader.MAID_MODELS.getInfo(modelId).orElse(null);
        return info == null ? null : prefix.append(ParseI18n.parse(info.getName()));
    }
}
