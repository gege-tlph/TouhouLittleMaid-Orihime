package com.github.tartaricacid.touhoulittlemaid.tileentity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import cn.sh1rocu.touhoulittlemaid.api.extension.IBlockEntityPersistentData;
import cn.sh1rocu.touhoulittlemaid.util.itemhandler.ItemStackHandler;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.github.tartaricacid.touhoulittlemaid.inventory.handler.AltarItemHandler;
import com.github.tartaricacid.touhoulittlemaid.util.PosListData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import javax.annotation.Nullable;

public class TileEntityAltar extends BlockEntity implements IBlockEntityPersistentData {
    public static final BlockEntityType<TileEntityAltar> TYPE = FabricBlockEntityTypeBuilder.create(TileEntityAltar::new, InitBlocks.ALTAR).build();
    private static final String STORAGE_ITEM = "StorageItem";
    private static final String IS_RENDER = "IsRender";
    private static final String CAN_PLACE_ITEM = "CanPlaceItem";
    private static final String STORAGE_STATE_ID = "StorageBlockStateId";
    private static final String DIRECTION = "Direction";
    private static final String STORAGE_BLOCK_LIST = "StorageBlockList";
    private static final String CAN_PLACE_ITEM_POS_LIST = "CanPlaceItemPosList";
    public final ItemStackHandler handler = new AltarItemHandler();
    private boolean isRender = false;
    private boolean canPlaceItem = false;
    private BlockState storageState = Blocks.AIR.defaultBlockState();
    private PosListData blockPosList = new PosListData();
    private PosListData canPlaceItemPosList = new PosListData();
    private Direction direction = Direction.SOUTH;

    public TileEntityAltar(BlockPos blockPos, BlockState blockState) {
        super(TYPE, blockPos, blockState);
    }

    public void setForgeData(BlockState storageState, boolean isRender, boolean canPlaceItem, Direction direction,
                             PosListData blockPosList, PosListData canPlaceItemPosList) {
        this.isRender = isRender;
        this.canPlaceItem = canPlaceItem;
        this.storageState = storageState;
        this.direction = direction;
        this.blockPosList = blockPosList;
        this.canPlaceItemPosList = canPlaceItemPosList;
        refresh();
    }

    /**
     * 1.21.2+ 方块移除重设计：{@code Block.onRemove(state, Level, pos, newState, isMoving)} 已完全移除。
     * 容器掉落改由 {@link net.minecraft.world.level.block.entity.BlockEntity#preRemoveSideEffects} 负责
     * （由 LevelChunk 在 BE 尚存活时调用；而 {@code Block.affectNeighborsAfterRemoval} 只管邻居更新）。
     * <p>
     * 其默认实现仅对 {@code implements Container} 的 BE 自动掉落；本类是
     * {@code extends BlockEntity} + 自研 {@code ItemStackHandler}，**不会**被自动处理，
     * 故在此显式恢复 HEAD 中原本位于 {@code BlockAltar.onRemove} 的掉落逻辑。
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        ItemStack stack = this.handler.getStackInSlot(0);
        if (!stack.isEmpty()) {
            Block.popResource(this.level, pos.offset(0, 1, 0), stack);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        // 必须在 super 之前填充：BlockEntityMixin 在 super.saveAdditional 的 RETURN 处写出 persistentData
        tlm$getPersistentData().putBoolean(IS_RENDER, isRender);
        tlm$getPersistentData().putBoolean(CAN_PLACE_ITEM, canPlaceItem);
        tlm$getPersistentData().putInt(STORAGE_STATE_ID, Block.getId(storageState));
        tlm$getPersistentData().put(STORAGE_ITEM, handler.serializeNBT(tlm$registries()));
        tlm$getPersistentData().putString(DIRECTION, direction.getSerializedName());
        tlm$getPersistentData().put(STORAGE_BLOCK_LIST, blockPosList.serialize());
        tlm$getPersistentData().put(CAN_PLACE_ITEM_POS_LIST, canPlaceItemPosList.serialize());
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        // 必须在 super 之后读取：persistentData 由 BlockEntityMixin 在 super 的 RETURN 处填充
        super.loadAdditional(input);
        isRender = tlm$getPersistentData().getBooleanOr(IS_RENDER, false);
        canPlaceItem = tlm$getPersistentData().getBooleanOr(CAN_PLACE_ITEM, false);
        storageState = Block.stateById(tlm$getPersistentData().getIntOr(STORAGE_STATE_ID, 0));
        handler.deserializeNBT(input.lookup(), tlm$getPersistentData().getCompoundOrEmpty(STORAGE_ITEM));
        direction = Direction.byName(tlm$getPersistentData().getStringOr(DIRECTION, "north"));
        blockPosList.deserialize(tlm$getPersistentData().getListOrEmpty(STORAGE_BLOCK_LIST));
        canPlaceItemPosList.deserialize(tlm$getPersistentData().getListOrEmpty(CAN_PLACE_ITEM_POS_LIST));
    }

    @Nullable
    private HolderLookup.Provider tlm$registries() {
        return this.level != null ? this.level.registryAccess() : null;
    }

    public BlockPos getWorldPosition() {
        return this.worldPosition;
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

    public void refresh() {
        this.setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    public boolean isRender() {
        return isRender;
    }

    public boolean isCanPlaceItem() {
        return canPlaceItem;
    }

    public BlockState getStorageState() {
        return storageState;
    }

    public PosListData getBlockPosList() {
        return blockPosList;
    }

    public PosListData getCanPlaceItemPosList() {
        return canPlaceItemPosList;
    }

    public ItemStack getStorageItem() {
        if (canPlaceItem) {
            return handler.getStackInSlot(0);
        }
        return ItemStack.EMPTY;
    }

    public Direction getDirection() {
        return direction;
    }
}
