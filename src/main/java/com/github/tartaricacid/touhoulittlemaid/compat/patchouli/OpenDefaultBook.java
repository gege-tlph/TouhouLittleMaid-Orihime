package com.github.tartaricacid.touhoulittlemaid.compat.patchouli;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.event.client.OpenPatchouliBookEvent;
import net.minecraft.resources.Identifier;
import vazkii.patchouli.api.PatchouliAPI;

public final class OpenDefaultBook {
    private static final Identifier BOOK_ID = Identifier.fromNamespaceAndPath(
            TouhouLittleMaid.MOD_ID, "memorizable_gensokyo");

    private OpenDefaultBook() {
    }

    public static void onPatchouliBookEvent(OpenPatchouliBookEvent event) {
        Identifier taskId = event.getTask().getUid();
        if (taskId.getNamespace().equals(TouhouLittleMaid.MOD_ID)) {
            PatchouliAPI.get().openBookGUI(BOOK_ID);
        }
    }
}
