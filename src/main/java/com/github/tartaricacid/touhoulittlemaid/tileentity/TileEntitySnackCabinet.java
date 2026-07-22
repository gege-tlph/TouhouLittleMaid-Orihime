package com.github.tartaricacid.touhoulittlemaid.tileentity;

import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import com.github.tartaricacid.touhoulittlemaid.block.BlockSnackCabinet;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntitySnackCabinet extends RandomizableContainerBlockEntity {
    public static final BlockEntityType<TileEntitySnackCabinet> TYPE = FabricBlockEntityTypeBuilder.create(TileEntitySnackCabinet::new, InitBlocks.SNACK_CABINET).build();

    private static final int SLOT_COUNT = 27;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    private final ContainerOpenersCounter openersCounter = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            TileEntitySnackCabinet.this.playSound(state, SoundEvents.BARREL_OPEN);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            TileEntitySnackCabinet.this.playSound(state, SoundEvents.BARREL_CLOSE);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int prevCount, int newCount) {
        }

        @Override
        public boolean isOwnContainer(Player player) {
            if (player.containerMenu instanceof ChestMenu menu) {
                return menu.getContainer() == TileEntitySnackCabinet.this;
            } else {
                return false;
            }
        }
    };

    public TileEntitySnackCabinet(BlockPos pos, BlockState blockState) {
        super(TYPE, pos, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.trySaveLootTable(output)) {
            ContainerHelper.saveAllItems(output, this.items);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(input)) {
            ContainerHelper.loadAllItems(input, this.items);
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.touhou_little_maid.snack_cabinet");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory playerInventory) {
        return ChestMenu.threeRows(id, playerInventory, this);
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator() && this.level != null) {
            this.openersCounter.incrementOpeners(user.getLivingEntity(), this.level, this.getBlockPos(),
                    this.getBlockState(), user.getContainerInteractionRange());
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator() && this.level != null) {
            this.openersCounter.decrementOpeners(user.getLivingEntity(), this.level, this.getBlockPos(),
                    this.getBlockState());
        }
    }

    public void recheckOpen() {
        if (!this.remove && this.level != null) {
            this.openersCounter.recheckOpeners(this.level, this.getBlockPos(), this.getBlockState());
        }
    }

    void playSound(BlockState state, SoundEvent sound) {
        if (this.level != null) {
            Vec3i facing = state.getValue(BlockSnackCabinet.FACING).getUnitVec3i();
            double x = this.worldPosition.getX() + 0.5 + facing.getX() / 2.0;
            double y = this.worldPosition.getY() + 0.5 + facing.getY() / 2.0;
            double z = this.worldPosition.getZ() + 0.5 + facing.getZ() / 2.0;
            this.level.playSound(null, x, y, z, sound, SoundSource.BLOCKS, 0.5F,
                    this.level.random.nextFloat() * 0.1F + 0.8F);
        }
    }
}