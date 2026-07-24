package com.github.tartaricacid.touhoulittlemaid.item;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityMaidBeacon;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.List;

import static com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent.STORAGE_DATA_TAG;

public class ItemMaidBeacon extends DoubleHighBlockItem {
    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.00");
    private static final String NEO_FORGE_DATA_TAG = "NeoForgeData";

    public ItemMaidBeacon(Identifier id) {
        super(InitBlocks.MAID_BEACON, (new Item.Properties())
                .setId(ResourceKey.create(Registries.ITEM, id))
                .useBlockDescriptionPrefix()
                .stacksTo(1)
                .attributes(ItemAttributeModifiers.builder()
                        .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 3, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.2F, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND)
                        .build()));
    }

    public static ItemStack tileEntityToItemStack(HolderLookup.Provider provider, TileEntityMaidBeacon beacon) {
        ItemStack stack = InitItems.MAID_BEACON.getDefaultInstance();
        stack.set(STORAGE_DATA_TAG, beacon.saveWithoutMetadata(provider));
        return stack;
    }

    public static void itemStackToTileEntity(ItemStack stack, TileEntityMaidBeacon beacon) {
        CompoundTag tag = stack.get(STORAGE_DATA_TAG);
        if (tag != null && tag.getCompound(NEO_FORGE_DATA_TAG).isPresent()) {
            // 正确读取tag数据
            CompoundTag forgeTag = tag.getCompoundOrEmpty(NEO_FORGE_DATA_TAG);
            beacon.loadData(forgeTag);
        }
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext worldIn, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flagIn){
        float numPower = 0f;
        CompoundTag tag = stack.get(STORAGE_DATA_TAG);
        if (tag != null && tag.getCompound(NEO_FORGE_DATA_TAG).isPresent()) {
            CompoundTag forgeTag = tag.getCompoundOrEmpty(NEO_FORGE_DATA_TAG);
            if (forgeTag.getFloat(TileEntityMaidBeacon.STORAGE_POWER_TAG).isPresent()) {
                numPower = forgeTag.getFloatOr(TileEntityMaidBeacon.STORAGE_POWER_TAG, 0.0F);
            }
        }
        tooltip.accept(Component.translatable("tooltips.touhou_little_maid.maid_beacon.desc", DECIMAL_FORMAT.format(numPower)).withStyle(ChatFormatting.GRAY));
    }
}
