package com.github.tartaricacid.touhoulittlemaid.compat.kubejs;

import com.github.tartaricacid.touhoulittlemaid.client.overlay.MaidTipsOverlay;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

public class ModKubeJSCompat {
    public static boolean ENABLE = false;

    public static void maidTipsOverlayInit(MaidTipsOverlay overlay) {
        if (ENABLE && FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {

        }
    }

    public static void maidBaubleInit(BaubleManager manager) {
        if (ENABLE) {

        }
    }

    public static void maidTaskInit(TaskManager manager) {
        if (ENABLE) {

        }
    }
}