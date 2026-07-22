package com.github.tartaricacid.touhoulittlemaid;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.KeyMapping;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

@Environment(EnvType.CLIENT)
public class TouhouLittleMaidClient {
    public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(modLoc("main"));

    public static void setup() {
        registerClientOnly();
    }

    private static void registerClientOnly() {
        // 这个仅用于客户端，所以不需要在服务端注册


    }
}
