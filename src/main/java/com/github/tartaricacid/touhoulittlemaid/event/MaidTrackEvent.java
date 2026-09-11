package com.github.tartaricacid.touhoulittlemaid.event;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncBaublePackage;
import com.github.tartaricacid.touhoulittlemaid.network.message.SyncYsmMaidDataPackage;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class MaidTrackEvent {
    public static void onTrackingPlayer(Entity target, Player player) {
        if (target instanceof EntityMaid maid && player instanceof ServerPlayer serverPlayer) {
            // 轮盘状态的**初始同步**：tick 里那个出站触发器只在状态发生变化时发一次，
            // 后来才进入追踪范围的玩家（新登录、跨维度、走近）收不到那一次，
            // 她的轮盘动画对这些人就永远不播。这里补一次全量。
            if (maid.isYsmModel()) {
                ServerPlayNetworking.send(serverPlayer, new SyncYsmMaidDataPackage(
                        maid.getId(), maid.rouletteAnim, maid.rouletteAnimPlaying, new Object2FloatOpenHashMap<>()));
            }

            // 如果包含需要同步到客户端的饰品信息，那么同步
            var syncClientBauble = maid.getMaidBauble().getSyncClientBauble(maid);
            if (!syncClientBauble.isEmpty()) {
                SyncBaublePackage msg = SyncBaublePackage.fullSync(maid.getId(), syncClientBauble);
                ServerPlayNetworking.send(serverPlayer, msg);
            }
        }
    }
}
