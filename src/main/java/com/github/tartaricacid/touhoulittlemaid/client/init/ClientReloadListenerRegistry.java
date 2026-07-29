package com.github.tartaricacid.touhoulittlemaid.client.init;

import com.github.tartaricacid.touhoulittlemaid.client.resource.bedrock.InternalBedrockModelManager;
import com.github.tartaricacid.touhoulittlemaid.client.resource.listener.CustomPackReloadListener;
import com.github.tartaricacid.touhoulittlemaid.client.resource.listener.EmojiReloadListener;
import com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;

/** Registers all client resource reload listeners. */
@Environment(EnvType.CLIENT)
public final class ClientReloadListenerRegistry {
    private static final Identifier BEDROCK_MODEL = IdentifierUtil.modLoc("bedrock_model");
    private static final Identifier BEDROCK_ENTITY_MODEL = IdentifierUtil.modLoc("bedrock_entity_model");
    private static final Identifier CUSTOM_PACK = IdentifierUtil.modLoc("custom_pack");
    private static final Identifier MAID_EMOJI_RELOAD = IdentifierUtil.modLoc("maid_emoji_reload");

    public static void onRegisterClientReloadListeners() {
        InternalBedrockModelManager.INSTANCE = InternalBedrockModelManager.create();

        ResourceLoader registry = ResourceLoader.get(PackType.CLIENT_RESOURCES);
        registry.registerReloader(BEDROCK_MODEL, InternalBedrockModelManager.INSTANCE.getModelSet());
        registry.registerReloader(BEDROCK_ENTITY_MODEL, InternalBedrockModelManager.INSTANCE.getEntityModelSet());
        registry.registerReloader(CUSTOM_PACK, new CustomPackReloadListener());
        registry.registerReloader(MAID_EMOJI_RELOAD, new EmojiReloadListener());
    }

    private ClientReloadListenerRegistry() {
    }
}
