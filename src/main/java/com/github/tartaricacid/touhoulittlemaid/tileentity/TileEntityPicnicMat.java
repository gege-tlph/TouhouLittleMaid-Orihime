package com.github.tartaricacid.touhoulittlemaid.tileentity;

import net.minecraft.nbt.IntArrayTag;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import cn.sh1rocu.touhoulittlemaid.api.extension.IBlockEntityPersistentData;
import cn.sh1rocu.touhoulittlemaid.util.itemhandler.ItemStackHandler;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.UUID;

public class TileEntityPicnicMat extends BlockEntity implements IBlockEntityPersistentData {
    public static final BlockEntityType<TileEntityPicnicMat> TYPE = FabricBlockEntityTypeBuilder.create(TileEntityPicnicMat::new, InitBlocks.PICNIC_MAT).build();
    private static final String CENTER_POS_NAME = "CenterPos";
    private static final String STORAGE_ITEM = "StorageItem";
    private static final String SIT_IDS = "SitIds";
    private final ItemStackHandler handler = new ItemStackHandler(9) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.get(DataComponents.FOOD) != null;
        }
    };
    private final UUID[] sitIds = new UUID[]{Util.NIL_UUID, Util.NIL_UUID, Util.NIL_UUID, Util.NIL_UUID};
    private BlockPos centerPos = BlockPos.ZERO;

    public TileEntityPicnicMat(BlockPos pos, BlockState blockState) {
        super(TYPE, pos, blockState);
    }

    public void setCenterPos(BlockPos centerPos) {
        this.centerPos = centerPos;
        this.refresh();
    }

    public void setSitId(int index, UUID uuid) {
        if (index < 0 || index >= 4) {
            return;
        }
        this.sitIds[index] = uuid;
        this.refresh();
    }

    public UUID[] getSitIds() {
        return sitIds;
    }

    public BlockPos getCenterPos() {
        return centerPos;
    }

    public ItemStack getStorageItem(int slotId) {
        return handler.getStackInSlot(slotId);
    }

    public boolean isEmpty(int slotId) {
        return handler.getStackInSlot(slotId).isEmpty();
    }

    public void setHandler(ItemStackHandler stackHandler) {
        for (int i = 0; i < stackHandler.getSlots(); i++) {
            ItemStack stack = stackHandler.getStackInSlot(i);
            if (i >= this.handler.getSlots()) {
                return;
            }
            this.handler.setStackInSlot(i, stack);
        }
        this.refresh();
    }

    public ItemStackHandler getHandler() {
        return handler;
    }

    @Override
    protected void saveAdditional(ValueOutput output){
        tlm$getPersistentData().putIntArray(CENTER_POS_NAME,
                new int[]{centerPos.getX(), centerPos.getY(), centerPos.getZ()});
        tlm$getPersistentData().put(STORAGE_ITEM, handler.serializeNBT(tlm$registries()));
        ListTag listTag = new ListTag();
        for (UUID uuid : sitIds) {
            listTag.add(new IntArrayTag(UUIDUtil.uuidToIntArray(uuid)));
        }
        tlm$getPersistentData().put(SIT_IDS, listTag);
        super.saveAdditional(output);
    }

    @Override
    public void loadAdditional(ValueInput input){
        super.loadAdditional(input);
        tlm$getPersistentData().getIntArray(CENTER_POS_NAME)
                .filter(a -> a.length == 3)
                .map(a -> new BlockPos(a[0], a[1], a[2]))
                .ifPresent(pos -> centerPos = pos);
        this.handler.deserializeNBT(input.lookup(), tlm$getPersistentData().getCompound(STORAGE_ITEM).orElse(new CompoundTag()));
        ListTag sitIdsTag = tlm$getPersistentData().getListOrEmpty(SIT_IDS);
        for (int i = 0; i < sitIdsTag.size() && i < 4; i++) {
            int finalI = i;
            sitIdsTag.getIntArray(i).map(UUIDUtil::uuidFromIntArray)
                    .ifPresent(u -> this.sitIds[finalI] = u);
        }
    }

    public void refresh() {
        this.setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return this.saveWithoutMetadata(pRegistries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public BlockPos getWorldPosition() {
        return this.worldPosition;
    }

    @javax.annotation.Nullable
    private HolderLookup.Provider tlm$registries() {
        return this.level != null ? this.level.registryAccess() : null;
    }
}
