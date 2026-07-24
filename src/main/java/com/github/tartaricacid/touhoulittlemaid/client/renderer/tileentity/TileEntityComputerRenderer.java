package com.github.tartaricacid.touhoulittlemaid.client.renderer.tileentity;

import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityComputer;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;

import static com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry.COMPUTER;

public class TileEntityComputerRenderer extends TileEntityJoyRenderer<TileEntityComputer> {
    private static final Identifier TEXTURE = IdentifierUtil.modLoc("textures/bedrock/block/computer.png");

    public TileEntityComputerRenderer(BlockEntityRendererProvider.Context context) {
        super(COMPUTER, TEXTURE);
    }
}