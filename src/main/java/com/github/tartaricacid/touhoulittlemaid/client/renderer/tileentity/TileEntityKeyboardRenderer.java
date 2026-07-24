package com.github.tartaricacid.touhoulittlemaid.client.renderer.tileentity;

import com.github.tartaricacid.touhoulittlemaid.tileentity.TileEntityKeyboard;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.Identifier;

import static com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelRegistry.KEYBOARD;

public class TileEntityKeyboardRenderer extends TileEntityJoyRenderer<TileEntityKeyboard> {
    private static final Identifier TEXTURE = IdentifierUtil.modLoc("textures/bedrock/block/keyboard.png");

    public TileEntityKeyboardRenderer(BlockEntityRendererProvider.Context context) {
        super(KEYBOARD, TEXTURE);
    }
}
