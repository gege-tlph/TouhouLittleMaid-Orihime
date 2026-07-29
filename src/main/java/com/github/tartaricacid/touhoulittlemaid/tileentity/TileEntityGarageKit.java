package com.github.tartaricacid.touhoulittlemaid.tileentity;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import cn.sh1rocu.touhoulittlemaid.api.extension.IBlockEntityPersistentData;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class TileEntityGarageKit extends BlockEntity implements IBlockEntityPersistentData {
    public static final BlockEntityType<TileEntityGarageKit> TYPE = FabricBlockEntityTypeBuilder.create(TileEntityGarageKit::new, InitBlocks.GARAGE_KIT).build();
    private static final String FACING_TAG = "GarageKitFacing";
    private static final String EXTRA_DATA = "ExtraData";
    private Direction facing = Direction.NORTH;
    private CompoundTag extraData = new CompoundTag();

    public TileEntityGarageKit(BlockPos blockPos, BlockState blockState) {
        super(TYPE, blockPos, blockState);
    }

    @Override
    public void saveAdditional(ValueOutput output){
        tlm$getPersistentData().putString(FACING_TAG, facing.getSerializedName());
        tlm$getPersistentData().put(EXTRA_DATA, extraData);
        super.saveAdditional(output);
    }

    @Override
    public void loadAdditional(ValueInput input){
        super.loadAdditional(input);
        facing = Direction.byName(tlm$getPersistentData().getStringOr(FACING_TAG, ""));
        extraData = tlm$getPersistentData().getCompoundOrEmpty(EXTRA_DATA);
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

    public Direction getFacing() {
        return facing;
    }

    public CompoundTag getExtraData() {
        return extraData;
    }

    public void setData(Direction facing, CompoundTag extraData) {
        this.facing = facing;
        this.extraData = extraData;
        this.setChanged();
        if (level != null) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    /**
     * 1.21.11: 破坏时掉落带数据的手办物品。原位于 {@code BlockGarageKit.onRemove}（5 参 onRemove 已移除），
     * 迁至此处（BE 尚存活可读 extraData；同 {@code TileEntityAltar.preRemoveSideEffects} 架构）。
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        ItemStack stack = new ItemStack(InitBlocks.GARAGE_KIT);
        stack.set(InitDataComponent.MAID_INFO, CustomData.of(this.extraData));
        Block.popResource(this.level, pos, stack);
    }
}
