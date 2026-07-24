package com.github.tartaricacid.touhoulittlemaid.tileentity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityBookshelf extends TileEntityJoy {
    public static final BlockEntityType<TileEntityBookshelf> TYPE = FabricBlockEntityTypeBuilder.create(TileEntityBookshelf::new, InitBlocks.BOOKSHELF).build();

    public TileEntityBookshelf(BlockPos pos, BlockState blockState) {
        super(TYPE, pos, blockState);
    }
}