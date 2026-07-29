package com.github.tartaricacid.touhoulittlemaid.item;

import cn.sh1rocu.touhoulittlemaid.api.extension.IItemEntity;
import com.github.tartaricacid.touhoulittlemaid.api.event.MaidAndItemTransformEvent;
// TODO: 1.21.11 — restore when compat.ysm.YsmCompat is available
// import com.github.tartaricacid.touhoulittlemaid.compat.ysm.YsmCompat;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.ItemMaidTooltip;
import com.github.tartaricacid.touhoulittlemaid.inventory.tooltip.YsmMaidInfo;
import com.github.tartaricacid.touhoulittlemaid.compat.ysm.YsmCompat;
import com.mojang.serialization.Codec;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.UUID;

import static com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent.MODEL_ID_TAG_NAME;

public abstract class AbstractStoreMaidItem extends Item implements IItemEntity {
    static final String CUSTOM_NAME = "CustomName";
    private static final String MAID_OWNER = "Owner";

    public AbstractStoreMaidItem(Properties properties) {
        super(properties);
    }

    public static void storeMaidData(ItemStack stack, EntityMaid maid) {
        CustomData compoundData = stack.get(InitDataComponent.MAID_INFO);
        if (compoundData == null) {
            // 1.21.11: Entity.saveWithoutId(CompoundTag) -> saveWithoutId(ValueOutput)。
            // TagValueOutput 是官方的 ValueOutput -> CompoundTag 桥（与 MaidBackupsManager 同款）。
            TagValueOutput valueOutput = TagValueOutput.createWithContext(
                    ProblemReporter.DISCARDING, maid.registryAccess());
            maid.saveWithoutId(valueOutput);
            CompoundTag tag = valueOutput.buildResult();
            var event = new MaidAndItemTransformEvent.ToItem(maid, stack, tag);
            MaidAndItemTransformEvent.TO_ITEM.invoker().onToItem(event);
            stack.set(InitDataComponent.MAID_INFO, CustomData.of(tag));
        }
    }

    @Override
    public boolean tlm$onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.isCurrentlyGlowing()) {
            entity.setGlowingTag(true);
        }
        if (!entity.isInvulnerable()) {
            entity.setInvulnerable(true);
        }
        Vec3 position = entity.position();
        int minY = entity.level.getMinY();
        if (position.y < minY) {
            entity.setNoGravity(true);
            entity.setDeltaMovement(Vec3.ZERO);
            entity.setPos(position.x, minY, position.z);
            return true;
        }
        return IItemEntity.super.tlm$onEntityItemUpdate(stack, entity);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        CustomData maidInfo = stack.get(InitDataComponent.MAID_INFO);
        if (maidInfo == null) {
            return Optional.empty();
        }
        // 1.21.11: CustomData.read(MapCodec) 已移除（HEAD 用的就是它）。
        // 替代为 CompoundTag.read(MapCodec)，注意它直接返回 Optional，不再有 .result()。
        CompoundTag infoTag = maidInfo.copyTag();
        Optional<String> modelId = infoTag.read(Codec.STRING.fieldOf(MODEL_ID_TAG_NAME));
        if (modelId.isEmpty()) {
            return Optional.empty();
        }
        // CustomName 对未命名女仆是可缺键：origin 用 .result() 静默取空；1.21.11 CompoundTag.read(fieldOf)
        // 缺键会记 ERROR 日志（tooltip 每帧刷屏）→ 改用静默的 Optional getter，恢复 origin 语义
        String customName = infoTag.getString(CUSTOM_NAME).orElse(StringUtils.EMPTY);
        // YSM 渲染相关数据
        // TODO: 1.21.11 — restore when YsmCompat is available
        YsmMaidInfo ysmMaidInfo = YsmCompat.getYsmMaidInfo(infoTag);
        return Optional.of(new ItemMaidTooltip(modelId.get(), customName, ysmMaidInfo));
    }

    public InteractionResult spawnFromStore(UseOnContext context, Player player, Level worldIn, EntityMaid maid, Runnable runnable) {
        ItemStack stack = context.getItemInHand();
        CustomData compoundData = stack.get(InitDataComponent.MAID_INFO);
        if (compoundData != null) {
            CompoundTag maidCompound = compoundData.copyTag();
            // 1.21.11: CompoundTag.getUUID / NbtUtils.loadUUID 均已移除。
            // "Owner" 由 EntityReference.store 以 UUIDUtil.CODEC 写入（字节码确认），
            // 该 codec 即 int-array 编码 —— 与 HEAD 的 putUUID/getUUID 格式相同，存档兼容。
            UUID ownerUid = maidCompound.read(MAID_OWNER, UUIDUtil.CODEC).orElse(Util.NIL_UUID);
            if (!player.getUUID().equals(ownerUid)) {
                return InteractionResult.FAIL;
            }

            var event = new MaidAndItemTransformEvent.ToMaid(maid, stack, maidCompound);
            MaidAndItemTransformEvent.TO_MAID.invoker().onToMaid(event);

            // 1.21.11: Entity.load(CompoundTag) -> load(ValueInput)
            maid.load(TagValueInput.create(ProblemReporter.DISCARDING, maid.registryAccess(), maidCompound));
            // 1.21.11: Entity.moveTo(BlockPos,float,float) -> snapTo(BlockPos,float,float)
            maid.snapTo(context.getClickedPos().above(), 0, 0);
            if (worldIn instanceof ServerLevel) {
                worldIn.addFreshEntity(maid);
            }
            maid.spawnExplosionParticle();
            maid.playSound(SoundEvents.PLAYER_SPLASH, 1.0F, worldIn.random.nextFloat() * 0.1F + 0.9F);
            runnable.run();
            return InteractionResult.SUCCESS;
        } else {
            if (worldIn.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.touhou_little_maid.photo.have_no_nbt_data"), false);
            }
        }
        return super.useOn(context);
    }
}
