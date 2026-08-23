package com.github.tartaricacid.touhoulittlemaid.compat.patchouli;

import com.github.tartaricacid.touhoulittlemaid.api.event.client.OpenPatchouliBookEvent;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import vazkii.patchouli.api.PatchouliAPI;

public final class PatchouliCompat {
    private PatchouliCompat() {
    }

    public static void init() {
        MultiblockRegistry.init();
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            OpenPatchouliBookEvent.CALLBACK.register(OpenDefaultBook::onPatchouliBookEvent);
        }
    }

    /** Called only after the Patchouli mod-id gate has passed. */
    public static ItemStack getBookStack(Identifier bookId) {
        return PatchouliAPI.get().getBookStack(bookId);
    }
}
