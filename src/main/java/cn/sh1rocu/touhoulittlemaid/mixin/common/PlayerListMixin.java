package cn.sh1rocu.touhoulittlemaid.mixin.common;

import cn.sh1rocu.touhoulittlemaid.api.event.PlayerLoggedInEvent;
import com.github.tartaricacid.touhoulittlemaid.network.message.config.SyncServerRulesPacket;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void tlm$playerLoggedIn(Connection connection, ServerPlayer serverPlayer, CommonListenerCookie cookie, CallbackInfo ci) {
        PlayerLoggedInEvent event = new PlayerLoggedInEvent(serverPlayer);
        PlayerLoggedInEvent.CALLBACK.invoker().post(event);
    }

    /**
     * 权限变化后重新下发服务器规则快照。
     *
     * <p>{@code canEdit} 只在发包那一刻由 {@code GameModeUtil.canEditSite} 求值，而 /op 与 /deop
     * 本身不会触发任何重新同步，于是「先进服、后被授予 OP」的玩家在退出重进之前，全局配置菜单里
     * 只有 OP 能编辑的条目一直不出现。</p>
     *
     * <p>注入的是 op 的三参重载（单参版委托给它）与 deop，它们只由命令路径调用。
     * <b>不能</b>改注入 {@code sendPlayerPermissionLevel}：它在 placeNewPlayer 与玩家重生时同样会被
     * 调用，那样每次加入与每次重生都会白发一个规则包。</p>
     *
     * <p>历史注记：这个选择原本还有一条更硬的理由——多发的包会让客户端把随后真正的首包误判成
     * 「代理换了后端」。那套判定随「服务器提供 STT」于 2026-07-27 一并撤除，理由只剩上面那条，
     * 但结论不变。</p>
     */
    @Inject(method = "op(Lnet/minecraft/server/players/NameAndId;Ljava/util/Optional;Ljava/util/Optional;)V",
            at = @At("TAIL"))
    private void tlm$onOp(NameAndId nameAndId, Optional<?> permissions, Optional<?> bypassPlayerLimit,
                          CallbackInfo ci) {
        tlm$resyncServerRules(nameAndId);
    }

    @Inject(method = "deop(Lnet/minecraft/server/players/NameAndId;)V", at = @At("TAIL"))
    private void tlm$onDeop(NameAndId nameAndId, CallbackInfo ci) {
        tlm$resyncServerRules(nameAndId);
    }

    private void tlm$resyncServerRules(NameAndId nameAndId) {
        PlayerList self = (PlayerList) (Object) this;
        ServerPlayer player = self.getPlayer(nameAndId.id());
        if (player != null && player.connection != null) {
            SyncServerRulesPacket.sendTo(player);
        }
    }
}