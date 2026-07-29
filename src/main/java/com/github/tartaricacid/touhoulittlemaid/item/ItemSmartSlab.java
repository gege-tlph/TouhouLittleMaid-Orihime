package com.github.tartaricacid.touhoulittlemaid.item;

import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;
import com.github.tartaricacid.touhoulittlemaid.data.MaidNumAttachment;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.init.InitEntities;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.util.PlaceHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.github.tartaricacid.touhoulittlemaid.init.InitDataAttachment.MAID_NUM;

public class ItemSmartSlab extends AbstractStoreMaidItem {
    private final Type type;

    public ItemSmartSlab(Identifier id, Type type) {
        // 1.21.11: Item.getDescriptionId() 现 final → Properties.overrideDescription（javap 确认）。
        //   3 个变体（smart_slab_init/empty/has_maid）HEAD 覆盖统一为 smart_slab → 保留统一键，行为等价。
        super((new Properties()).setId(ResourceKey.create(Registries.ITEM, id)).stacksTo(1).rarity(Rarity.RARE).overrideDescription("item.touhou_little_maid.smart_slab"));
        this.type = type;
    }

    public static UUID getInitMaidOwner(ItemStack stack) {
        if (stack.has(InitDataComponent.INIT_MAID_OWNER_TAG)) {
            return stack.getOrDefault(InitDataComponent.INIT_MAID_OWNER_TAG, Util.NIL_UUID);
        }
        return Util.NIL_UUID;
    }

    public static boolean setInitMaidOwner(ItemStack stack, UUID ownerUid) {
        if (stack.getItem() instanceof ItemSmartSlab smartSlab && smartSlab.type == Type.INIT) {
            stack.set(InitDataComponent.INIT_MAID_OWNER_TAG, ownerUid);
            return true;
        }
        return false;
    }


    @Override
    public InteractionResult useOn(UseOnContext context) {
        Direction clickedFace = context.getClickedFace();
        Player player = context.getPlayer();
        Level worldIn = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        if (player == null) {
            return super.useOn(context);
        }
        if (clickedFace == Direction.UP && !PlaceHelper.notSuitableForPlaceMaid(worldIn, clickedPos)) {
            // B5: EntityType.create(Level) → create(Level, EntitySpawnReason)
            EntityMaid maid = InitEntities.MAID.create(worldIn, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
            if (maid == null) {
                return super.useOn(context);
            }
            if (this.type == Type.INIT) {
                // 检查是否有初始主人锁定
                UUID initOwnerUid = getInitMaidOwner(context.getItemInHand());
                // 有锁定则进行 UUID 判断
                if (!initOwnerUid.equals(Util.NIL_UUID) && !player.getUUID().equals(initOwnerUid)) {
                    MutableComponent tip = Component.translatable("tooltips.touhou_little_maid.smart_slab.not_your_maid").withStyle(ChatFormatting.DARK_RED);
                    if (!worldIn.isClientSide()) {
                        player.displayClientMessage(tip, false);
                    }
                    return InteractionResult.FAIL;
                }
                return spawnNewMaid(context, player, worldIn, maid);
            }
            if (this.type == Type.HAS_MAID) {
                return spawnFromStore(context, player, worldIn, maid, () -> {
                    player.setItemInHand(context.getHand(), InitItems.SMART_SLAB_EMPTY.getDefaultInstance());
                    player.getCooldowns().addCooldown(InitItems.SMART_SLAB_EMPTY.getDefaultInstance(), 20);
                });
            }
        } else {
            if (this.type != Type.EMPTY && worldIn.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.touhou_little_maid.photo.not_suitable_for_place_maid"), false);
            }
        }
        return super.useOn(context);
    }

    private InteractionResult spawnNewMaid(UseOnContext context, Player player, Level worldIn, EntityMaid maid) {
        MaidNumAttachment cap = player.getAttachedOrCreate(MAID_NUM, () -> new MaidNumAttachment(0));
        if (cap.canAdd() || player.isCreative()) {
            if (!player.isCreative()) {
                cap.add();
            }
            maid.tame(player);
            if (worldIn instanceof ServerLevel) {
                // 1.21.11: Level.getCurrentDifficultyAt 已移除，仅 ServerLevel 有（javap 确认）。worldIn 已由上面 instanceof 判定为 ServerLevel。
                maid.finalizeSpawn((ServerLevel) worldIn, ((ServerLevel) worldIn).getCurrentDifficultyAt(context.getClickedPos()), EntitySpawnReason.SPAWN_ITEM_USE, null);
                // 1.21.11: Entity.moveTo(BlockPos,float,float) -> snapTo(BlockPos,float,float)
                maid.snapTo(context.getClickedPos().above(), 0, 0);
                worldIn.addFreshEntity(maid);
            }
            maid.spawnExplosionParticle();
            maid.playSound(SoundEvents.PLAYER_SPLASH, 1.0F, worldIn.random.nextFloat() * 0.1F + 0.9F);
            player.setItemInHand(context.getHand(), InitItems.SMART_SLAB_EMPTY.getDefaultInstance());
            player.getCooldowns().addCooldown(InitItems.SMART_SLAB_EMPTY.getDefaultInstance(), 20);
            return InteractionResult.SUCCESS;
        } else {
            if (worldIn.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.touhou_little_maid.owner_maid_num.can_not_add", cap.get(), cap.getMaxNum()), false);
            }
            return super.useOn(context);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return this.type != Type.EMPTY;
    }

    @Override
    public boolean canFitInsideContainerItems() {
        return this.type != Type.HAS_MAID;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext worldIn, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flagIn){
        if (this.type == Type.INIT) {
            MutableComponent text = Component.translatable("tooltips.touhou_little_maid.smart_slab.maid_name", I18n.get("tooltips.touhou_little_maid.smart_slab.maid_name.unknown"));
            tooltip.accept(text.withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("tooltips.touhou_little_maid.smart_slab.desc").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (this.type == Type.HAS_MAID) {
            return super.getTooltipImage(stack);
        }
        return Optional.empty();
    }

    public enum Type {
        /**
         * Slab Type
         */
        INIT, EMPTY, HAS_MAID,
    }
}
