package com.github.tartaricacid.touhoulittlemaid.compat.iris;

import net.fabricmc.loader.api.FabricLoader;
import net.irisshaders.iris.api.v0.IrisApi;

/**
 * 可选虹膜桥。所有 Iris API 访问均受到保护，因此当 Iris 不存在或其 API 无法初始化时，正常 Fabric 客户端仍可加载。
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
            // 不兼容的可选 API 不得阻止客户端加载。
        }
    }

    public static boolean isInstalled() {
        return installed;
    }

    public static boolean isRenderingShadow() {
        return installed && IrisApi.getInstance().isRenderingShadowPass();
    }
}
