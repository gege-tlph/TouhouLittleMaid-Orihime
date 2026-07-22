package com.github.tartaricacid.touhoulittlemaid.tileentity;

import net.minecraft.nbt.IntArrayTag;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import cn.sh1rocu.touhoulittlemaid.api.extension.IBlockEntityPersistentData;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import com.google.common.collect.Lists;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

public class TileEntityStatue extends BlockEntity implements IBlockEntityPersistentData {
    public static final BlockEntityType<TileEntityStatue> TYPE = FabricBlockEntityTypeBuilder.create(TileEntityStatue::new, InitBlocks.STATUE).build();
    private static final String STATUE_SIZE_TAG = "StatueSize";
    private static final String CORE_BLOCK_TAG = "CoreBlock";
    private static final String CORE_BLOCK_POS_TAG = "CoreBlockPos";
    private static final String STATUE_FACING_TAG = "StatueFacing";
    private static final String ALL_BLOCKS_TAG = "AllBlocks";
    private static final String EXTRA_MAID_DATA = "ExtraMaidData";
    private Size size = Size.SMALL;
    private boolean isCoreBlock = false;
    private BlockPos coreBlockPos = BlockPos.ZERO;
    private Direction facing = Direction.NORTH;
    private List<BlockPos> allBlocks = Lists.newArrayList();
    @Nullable
    private CompoundTag extraMaidData = null;

    public TileEntityStatue(BlockPos blockPos, BlockState blockState) {
        super(TYPE, blockPos, blockState);
    }

    public void setForgeData(Size size, boolean isCoreBlock, BlockPos coreBlockPos, Direction facing,
                             List<BlockPos> allBlocks, @Nullable CompoundTag extraData) {
        this.size = size;
        this.isCoreBlock = isCoreBlock;
        this.coreBlockPos = coreBlockPos;
        this.facing = facing;
        this.allBlocks = allBlocks;
        this.extraMaidData = extraData;
        refresh();
    }

    @Override
    public void saveAdditional(ValueOutput output){
        tlm$getPersistentData().putInt(STATUE_SIZE_TAG, size.ordinal());
        tlm$getPersistentData().putBoolean(CORE_BLOCK_TAG, isCoreBlock);
        tlm$getPersistentData().putIntArray(CORE_BLOCK_POS_TAG,
                new int[]{coreBlockPos.getX(), coreBlockPos.getY(), coreBlockPos.getZ()});
        tlm$getPersistentData().putString(STATUE_FACING_TAG, facing.getSerializedName());
        ListTag blockList = new ListTag();
        for (BlockPos pos : allBlocks) {
            blockList.add(new IntArrayTag(new int[]{pos.getX(), pos.getY(), pos.getZ()}));
        }
        tlm$getPersistentData().put(ALL_BLOCKS_TAG, blockList);
        if (extraMaidData != null) {
            tlm$getPersistentData().put(EXTRA_MAID_DATA, extraMaidData);
        }
        super.saveAdditional(output);
    }

    @Override
    public void loadAdditional(ValueInput input){
        super.loadAdditional(input);
        size = Size.getSizeByIndex(tlm$getPersistentData().getIntOr(STATUE_SIZE_TAG, 0));
        isCoreBlock = tlm$getPersistentData().getBooleanOr(CORE_BLOCK_TAG, false);
        tlm$getPersistentData().getIntArray(CORE_BLOCK_POS_TAG)
                .filter(a -> a.length == 3)
                .map(a -> new BlockPos(a[0], a[1], a[2]))
                .ifPresent(pos -> coreBlockPos = pos);
        facing = Direction.byName(tlm$getPersistentData().getStringOr(STATUE_FACING_TAG, ""));
        allBlocks.clear();
        ListTag blockList = tlm$getPersistentData().getListOrEmpty(ALL_BLOCKS_TAG);
        for (int i = 0; i < blockList.size(); i++) {
            blockList.getIntArray(i)
                    .filter(a -> a.length == 3)
                    .ifPresent(a -> allBlocks.add(new BlockPos(a[0], a[1], a[2])));
        }
        // getCompound 仅在键存在且为 Compound 时返回值，语义与旧的 contains(k, TAG_COMPOUND) 一致
        tlm$getPersistentData().getCompound(EXTRA_MAID_DATA).ifPresent(t -> extraMaidData = t);
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

    public Size getSize() {
        return size;
    }

    public boolean isCoreBlock() {
        return isCoreBlock;
    }

    public BlockPos getCoreBlockPos() {
        return coreBlockPos;
    }

    public Direction getFacing() {
        return facing;
    }

    public List<BlockPos> getAllBlocks() {
        return allBlocks;
    }

    @Nullable
    public CompoundTag getExtraMaidData() {
        return extraMaidData;
    }

    public enum Size {
        // 雕像的尺寸
        TINY(0.5f, new Vec3i(1, 1, 1)),
        SMALL(1.0f, new Vec3i(1, 2, 1)),
        MIDDLE(2.0f, new Vec3i(2, 4, 2)),
        BIG(3.0f, new Vec3i(3, 6, 3));

        private final float scale;
        private final Vec3i dimension;

        Size(float scale, Vec3i dimension) {
            this.scale = scale;
            this.dimension = dimension;
        }

        public static Size getSizeByIndex(int index) {
            return Size.values()[Mth.clamp(index, 0, Size.values().length - 1)];
        }

        public float getScale() {
            return scale;
        }

        public Vec3i getDimension() {
            return dimension;
        }
    }


}
