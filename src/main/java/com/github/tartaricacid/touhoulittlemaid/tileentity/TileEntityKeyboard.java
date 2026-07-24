package com.github.tartaricacid.touhoulittlemaid.tileentity;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import com.github.tartaricacid.touhoulittlemaid.init.InitBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityKeyboard extends TileEntityJoy {
    public static final BlockEntityType<TileEntityKeyboard> TYPE = FabricBlockEntityTypeBuilder.create(TileEntityKeyboard::new, InitBlocks.KEYBOARD).build();

    public TileEntityKeyboard(BlockPos pos, BlockState blockState) {
        super(TYPE, pos, blockState);
    }
}