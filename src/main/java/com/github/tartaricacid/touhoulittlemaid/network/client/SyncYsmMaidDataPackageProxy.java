package com.github.tartaricacid.touhoulittlemaid.network.client;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncYsmMaidDataPackage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;

public final class SyncYsmMaidDataPackageProxy {
    public static void handle(SyncYsmMaidDataPackage message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        Entity entity = level.getEntity(message.entityId());
        if (!(entity instanceof EntityMaid maid)) {
            return;
        }
        // 这两个方法各自会把 rouletteAnimDirty 置回 true——在客户端那是渲染端脏标记，
        // 由 OpenYSM 的 predicate 消费，不是出站触发器。见 SyncYsmMaidDataPackage 的类注释。
        if (message.isRouletteAnimPlaying()) {
            maid.playRouletteAnim(message.rouletteAnim());
        } else {
            maid.stopRouletteAnim();
        }
    }
}
