package com.github.tartaricacid.touhoulittlemaid.item;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAndItemTransformEvent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.network.NetworkHandler;
import com.github.tartaricacid.touhoulittlemaid.network.message.SpawnParticlePackage;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

public class ItemFilm extends AbstractStoreMaidItem {
    private static final String ID_TAG = "id";

    public ItemFilm(Identifier id) {
        super((new Item.Properties()).setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(1));
    }

    public static ItemStack maidToFilm(EntityMaid maid) {
        ItemStack film = InitItems.FILM.getDefaultInstance();
        maid.setHomeModeEnable(false);
        // 1.21.11: Entity.saveWithoutId(CompoundTag) -> saveWithoutId(ValueOutput)
        TagValueOutput valueOutput = TagValueOutput.createWithContext(
                ProblemReporter.DISCARDING, maid.registryAccess());
        maid.saveWithoutId(valueOutput);
        CompoundTag maidTag = valueOutput.buildResult();
        removeMaidSomeData(maidTag);
        maidTag.putString(ID_TAG, Objects.requireNonNull(BuiltInRegistries.ENTITY_TYPE.getKey(InitEntities.MAID)).toString());

        var event = new MaidAndItemTransformEvent.ToItem(maid, film, maidTag);
        MaidAndItemTransformEvent.TO_ITEM.invoker().onToItem(event);

        film.set(InitDataComponent.MAID_INFO, CustomData.of(maidTag));
        return film;
    }

    public static void filmToMaid(ItemStack film, Level worldIn, BlockPos pos, Player player) {
        CustomData compoundData = film.get(InitDataComponent.MAID_INFO);
        if (compoundData == null) {
            return;
        }
        CompoundTag data = compoundData.copyTag();
        // 1.21.11: CompoundTag.getString(k) 返回 Optional<String> -> getStringOr(k, "")
        Identifier entityId = Identifier.tryParse(data.getStringOr(ID_TAG, ""));
        Identifier maidId = BuiltInRegistries.ENTITY_TYPE.getKey(InitEntities.MAID);

        if (entityId != null && entityId.equals(maidId)) {
            EntityMaid maid = new EntityMaid(worldIn);

            var event = new MaidAndItemTransformEvent.ToMaid(maid, film, data);
            MaidAndItemTransformEvent.TO_MAID.invoker().onToMaid(event);

            // 1.21.11: readAdditionalSaveData(CompoundTag) -> (ValueInput)。
            // 保持 HEAD 的 readAdditionalSaveData（非 load）：load 还会读回 Pos/Motion/UUID，
            // 而此处紧接着显式 setPos，且复用旧 UUID 有重复实体风险。
            // （26.1 在此改用了 load，属其行为变更，不采纳。）
            maid.readAdditionalSaveData(TagValueInput.create(
                    ProblemReporter.DISCARDING, worldIn.registryAccess(), data));
            maid.setPos(pos.getX(), pos.getY(), pos.getZ());
            // 实体生成必须在服务端应用
            if (!worldIn.isClientSide()) {
                worldIn.addFreshEntity(maid);
                NetworkHandler.sendToNearby(maid, new SpawnParticlePackage(maid.getId(), SpawnParticlePackage.Type.EXPLOSION));
                worldIn.playSound(null, pos, InitSounds.ALTAR_CRAFT, SoundSource.VOICE, 1.0f, 1.0f);
            }
            film.shrink(1);
            return;
        }

        if (!worldIn.isClientSide()) {
            player.displayClientMessage(Component.translatable("tooltips.touhou_little_maid.film.no_data.desc"), false);
        }
    }

    private static void removeMaidSomeData(CompoundTag nbt) {
        nbt.remove(EntityMaid.MAID_BACKPACK_TYPE);
        nbt.remove(EntityMaid.MAID_INVENTORY_TAG);
        nbt.remove(EntityMaid.MAID_BAUBLE_INVENTORY_TAG);
        nbt.remove(EntityMaid.EXPERIENCE_TAG);
        nbt.remove("ArmorItems");
        nbt.remove("HandItems");
        nbt.remove("Leash");
        nbt.remove("Health");
        nbt.remove("HurtTime");
        nbt.remove("DeathTime");
        nbt.remove("HurtByTimestamp");
        nbt.remove("Pos");
        nbt.remove("Motion");
        nbt.remove("FallDistance");
        nbt.remove("Fire");
        nbt.remove("Air");
        nbt.remove("TicksFrozen");
        nbt.remove("HasVisualFire");
        nbt.remove("Passengers");
        nbt.remove("ActiveEffects");
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext worldIn, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flagIn){
        if (stack.get(InitDataComponent.MAID_INFO) == null) {
            tooltip.accept(Component.translatable("tooltips.touhou_little_maid.film.no_data.desc").withStyle(ChatFormatting.DARK_RED));
        }
    }
}
