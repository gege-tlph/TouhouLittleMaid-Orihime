package com.github.tartaricacid.touhoulittlemaid.client.renderer.tileentity;

import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityBookshelf;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;

import static com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry.BOOKSHELF;

public class TileEntityBookshelfRenderer extends TileEntityJoyRenderer<TileEntityBookshelf> {
    private static final Identifier TEXTURE = IdentifierUtil.modLoc("textures/bedrock/block/bookshelf.png");

    public TileEntityBookshelfRenderer(BlockEntityRendererProvider.Context context) {
        super(BOOKSHELF, TEXTURE);
    }
}
