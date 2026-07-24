package com.github.tartaricacid.touhoulittlemaid.tileentity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import cn.sh1rocu.touhoulittlemaid.api.extension.IBlockEntityPersistentData;
import cn.sh1rocu.touhoulittlemaid.util.itemhandler.ItemStackHandler;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class TileEntityShrine extends BlockEntity implements IBlockEntityPersistentData {
    public static final BlockEntityType<TileEntityShrine> TYPE = FabricBlockEntityTypeBuilder.create(TileEntityShrine::new, InitBlocks.SHRINE).build();
    private static final String STORAGE_ITEM = "StorageItem";
    private final ItemStackHandler handler = new ItemStackHandler() {
        @Override
        protected void onContentsChanged(int slot) {
            // 当物品栏内容发生变化时，这个方法会被调用
            // 我们需要在这里调用 refresh() 来通知 Minecraft 该方块实体的数据已更新，需要保存并同步到客户端
            refresh();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() == InitItems.FILM;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    public TileEntityShrine(BlockPos pos, BlockState blockState) {
        super(TYPE, pos, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        // 必须在 super 之前填充：BlockEntityMixin 在 super.saveAdditional 的 RETURN 处写出 persistentData
        tlm$getPersistentData().put(STORAGE_ITEM, handler.serializeNBT(tlm$registries()));
        super.saveAdditional(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        // 必须在 super 之后读取：persistentData 由 BlockEntityMixin 在 super 的 RETURN 处填充
        super.loadAdditional(input);
        handler.deserializeNBT(input.lookup(), tlm$getPersistentData().getCompound(STORAGE_ITEM).orElse(new CompoundTag()));
    }

    @Nullable
    private HolderLookup.Provider tlm$registries() {
        return this.level != null ? this.level.registryAccess() : null;
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

    public ItemStack getStorageItem() {
        return handler.getStackInSlot(0);
    }

    public void insertStorageItem(ItemStack stack) {
        handler.insertItem(0, stack, false);
    }

    public ItemStack extractStorageItem() {
        return handler.extractItem(0, 1, false);
    }

    public boolean isEmpty() {
        return handler.getStackInSlot(0).isEmpty();
    }

    public boolean canInsert(ItemStack stack) {
        return handler.isItemValid(0, stack);
    }
    /**
     * 1.21.2+ 方块移除重设计：{@code Block.onRemove(state, Level, pos, newState, isMoving)} 已完全移除。
     * 掉落改由 {@code BlockEntity.preRemoveSideEffects} 负责（LevelChunk 在 BE 尚存活时调用；
     * {@code Block.affectNeighborsAfterRemoval} 只管邻居更新）。其默认实现仅对 {@code implements Container}
     * 的 BE 自动掉落——本类是 {@code extends BlockEntity} + 自研 handler，**不会**被自动处理，
     * 故在此显式恢复原本位于 Block.onRemove 的逻辑。
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        ItemStack storageItem = this.extractStorageItem();
        if (!storageItem.isEmpty()) {
            Block.popResource(this.level, pos.offset(0, 1, 0), storageItem);
        }
    }

}
