package com.github.tartaricacid.touhoulittlemaid.compat.iris;

import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;

/**
 * Optional Iris bridge. All Iris API access is guarded so the normal Fabric
 * client remains loadable when Iris is absent or its API cannot initialize.
 */
public final class IrisCompat {
    private static final String MOD_ID = "iris";
    private static boolean installed;

    private IrisCompat() {
    }

    public static void init() {
        installed = false;
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }
        try {
            IrisApi.getInstance().isRenderingShadowPass();
            installed = true;
        } catch (Throwable ignored) {
            // An incompatible optional API must not prevent the client from loading.
        }
    }

    public static boolean isInstalled() {
        return installed;
    }

    public static boolean isRenderingShadow() {
        return installed && IrisApi.getInstance().isRenderingShadowPass();
    }
}
