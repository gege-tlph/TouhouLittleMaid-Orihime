package com.github.tartaricacid.touhoulittlemaid.init.registry;

import com.github.tartaricacid.touhoulittlemaid.compat.patchouli.PatchouliCompat;
import com.github.tartaricacid.touhoulittlemaid.compat.refurbishedfurniture.RefurbishedFurnitureCompat;
import net.fabricmc.loader.api.FabricLoader;

public final class CompatRegistry {
    public static final String PATCHOULI = "patchouli";
    public static final String CLOTH_CONFIG = "cloth-config";
    public static final String CARRY_ON = "carryon";

    private CompatRegistry() {
    }

    public static void onEnqueue() {
        checkModLoad(PATCHOULI, PatchouliCompat::init);
        checkModLoad(RefurbishedFurnitureCompat.MOD_ID, RefurbishedFurnitureCompat::init);
    }

    private static void checkModLoad(String modId, Runnable runnable) {
        if (FabricLoader.getInstance().isModLoaded(modId)) {
            runnable.run();
        }
    }
}
