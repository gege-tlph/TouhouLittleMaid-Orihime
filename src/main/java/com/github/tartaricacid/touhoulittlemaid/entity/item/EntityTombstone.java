package com.github.tartaricacid.touhoulittlemaid.entity.item;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import cn.sh1rocu.touhoulittlemaid.util.itemhandler.ItemHandlerHelper;
import cn.sh1rocu.touhoulittlemaid.util.itemhandler.ItemStackHandler;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.world.data.MaidWorldData;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.UUID;

import static com.github.tartaricacid.touhoulittlemaid.util.ItemsUtil.canItemInsert;

public class EntityTombstone extends Entity {
    // 1.21.11: EntityType.Builder.build(String) 已移除 -> build(ResourceKey)
    public static final EntityType<EntityTombstone> TYPE = EntityType.Builder.<EntityTombstone>of(EntityTombstone::new, MobCategory.MISC)
            .sized(0.8f, 1.2f).clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(TouhouLittleMaid.MOD_ID, "tombstone")));
    private static final String OWNER_ID_TAG = "OwnerId";
    private static final String TOMBSTONE_ITEMS_TAG = "TombstoneItems";
    private static final String MAID_NAME_TAG = "MaidName";
    private static final EntityDataAccessor<Component> MAID_NAME = SynchedEntityData.defineId(EntityTombstone.class, EntityDataSerializers.COMPONENT);
    // 考虑其他模组会添加额外的存储内容，加之饰品模组拓展了数量，故将墓碑存储上限修改为 256 组
    private final ItemStackHandler items = new ItemStackHandler(256);
    private UUID ownerId = Util.NIL_UUID;

    public EntityTombstone(EntityType<?> entityTypeIn, Level worldIn) {
        super(entityTypeIn, worldIn);
    }

    public EntityTombstone(Level worldIn, UUID ownerId, Vec3 pos) {
        this(TYPE, worldIn);
        this.ownerId = ownerId;
        this.setPos(pos);
    }

    public void insertItem(ItemStack item) {
        ItemHandlerHelper.insertItemStacked(this.items, item, false);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        Ingredient ntrItem = EntityMaid.getNtrItem();
        // 只能主手触发
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        // NTR 工具可以收回墓碑
        if (player.getUUID().equals(this.ownerId) || ntrItem.test(itemInHand)) {
            // 第一步：预检查所有物品是否能被玩家容纳（不实际提取物品）
            // 如果玩家按下了 Shift 键，则强制取出
            if (!player.isSecondaryUseActive()) {
                for (int i = 0; i < this.items.getSlots(); i++) {
                    ItemStack stack = this.items.getStackInSlot(i);
                    if (stack.isEmpty() || canItemInsert(player, stack)) {
                        continue;
                    }
                    // 一旦发现有物品不能插入，立即中断检查
                    if (!player.level.isClientSide()) {
                        player.displayClientMessage(Component.translatable("message.touhou_little_maid.tombstone.player_inventory_full.1"), false);
                        player.displayClientMessage(Component.translatable("message.touhou_little_maid.tombstone.player_inventory_full.2"), false);
                    }
                    return InteractionResult.FAIL;
                }
            }

            // 第二步：确认可以处理后，才实际提取并给予物品
            for (int i = 0; i < this.items.getSlots(); i++) {
                int size = this.items.getSlotLimit(i);
                ItemStack extractItem = this.items.extractItem(i, size, false);
                if (!extractItem.isEmpty()) {
                    ItemHandlerHelper.giveItemToPlayer(player, extractItem);
                }
            }
            // 所有物品处理完毕后，再销毁实体
            this.discard();
            return InteractionResult.SUCCESS;
        }

        // 还原 HEAD：非拥有者交互时提示 "not yours"。1.21.11: Ingredient.getItems() 移除 → items():Stream<Holder<Item>>。
        if (!player.level().isClientSide()) {
            ItemStack stack = ntrItem.items().findFirst().map(h -> new ItemStack(h.value())).orElse(ItemStack.EMPTY);
            Component displayName = stack.getDisplayName();
            player.displayClientMessage(Component.translatable("message.touhou_little_maid.tombstone.not_yours.1"), false);
            player.displayClientMessage(Component.translatable("message.touhou_little_maid.tombstone.not_yours.2").append(displayName), false);
        }
        return super.interact(player, hand);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(MAID_NAME, Component.empty());
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        // owner：HEAD 用 putUUID（int-array）→ UUIDUtil.CODEC（逐字节兼容；同 EntityBroom）。
        output.store(OWNER_ID_TAG, UUIDUtil.CODEC, this.ownerId);
        // items：HEAD 用 compound.put(TAG, serializeNBT) → COMPOUND_TAG_CODEC（逐字节兼容）。
        output.store(TOMBSTONE_ITEMS_TAG, CustomData.COMPOUND_TAG_CODEC, this.items.serializeNBT(this.registryAccess()));
        // MAID_NAME：HEAD 用 Component.Serializer.toJson(...)（已移除）存 JSON 字符串。
        // 忠实保留 JSON-string 格式：ComponentSerialization.CODEC + 注册表 JsonOps。
        JsonElement nameJson = ComponentSerialization.CODEC
                .encodeStart(this.registryAccess().createSerializationContext(JsonOps.INSTANCE), this.getMaidName())
                .getOrThrow();
        output.putString(MAID_NAME_TAG, nameJson.toString());
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        input.read(OWNER_ID_TAG, UUIDUtil.CODEC).ifPresent(uuid -> this.ownerId = uuid);
        input.read(TOMBSTONE_ITEMS_TAG, CustomData.COMPOUND_TAG_CODEC)
                .ifPresent(tag -> this.items.deserializeNBT(this.registryAccess(), tag));
        input.getString(MAID_NAME_TAG).ifPresent(nameJson -> {
            if (!nameJson.isEmpty()) {
                ComponentSerialization.CODEC
                        .parse(this.registryAccess().createSerializationContext(JsonOps.INSTANCE), JsonParser.parseString(nameJson))
                        .resultOrPartial(TouhouLittleMaid.LOGGER::error)
                        .ifPresent(this::setMaidName);
            }
        });
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        if (!this.level.isClientSide()) {
            this.checkBelowWorld();
        }
    }

    @Override
    public boolean skipAttackInteraction(Entity pEntity) {
        return true;
    }

    // TODO: 1.21.11 fix - hurt() is now final in Entity, cannot override
    // @Override
    // public boolean hurt(DamageSource pSource, float pAmount) {
    //     return false;
    // }

    @Override
    public void move(MoverType pType, Vec3 pPos) {
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    public void push(double pX, double pY, double pZ) {
    }

    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }

    @Override
    public void thunderHit(ServerLevel pLevel, LightningBolt pLightning) {
    }

    @Override
    public void refreshDimensions() {
    }

    @Override
    public void remove(RemovalReason reason) {
        if (reason.shouldDestroy()) {
            MaidWorldData maidWorldData = MaidWorldData.get(level);
            if (maidWorldData != null) {
                maidWorldData.removeTombstones(this);
            }
        }
        super.remove(reason);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return this.isAlive();
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setMaidName(@Nullable Component name) {
        if (name != null) {
            this.entityData.set(MAID_NAME, name);
        }
    }

    public Component getMaidName() {
        return this.entityData.get(MAID_NAME);
    }

    public ItemStackHandler getItems() {
        return items;
    }
}
