package cn.sh1rocu.touhoulittlemaid.mixin.common;

import com.github.tartaricacid.touhoulittlemaid.network.message.config.SyncServerRulesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * 权限变化后重新下发服务器规则快照。
 *
 * <p>{@code canEdit} 只在发包那一刻由 {@code GameModeUtil.canEditSite} 求值，而 {@code /op} 与
 * {@code /deop} 本身不触发任何重新同步，于是「先进服、后被授予 OP」的玩家在退出重进之前，
 * 配置菜单里的「玩法设置 / 高级设置」两栏一直不出现。</p>
 *
 * <p><b>注入点只能是 {@code op} 与 {@code deop}，不能是 {@code sendPlayerPermissionLevel}。</b>
 * 后者看起来是「所有权限变更的汇流处」，但 javap 26.1.2 的 {@code PlayerList} 实查，
 * 它的调用方恰是四个：{@code placeNewPlayer}、{@code respawn}、{@code op}、{@code deop}——
 * 挂上去会在**每次加入与每次重生**都白发一个规则包。
 * 「所有变更都汇流到此」往往同时意味着「它也承载了初始化」，这条在基准上是踩出来的。</p>
 *
 * <p>只注入三参 {@code op}：javap 确认单参重载的方法体就是
 * {@code op(nameAndId, Optional.empty(), Optional.empty())}，注入三参即覆盖两条路径，
 * 注入两个反而会让命令走单参时发两遍。</p>
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "op(Lnet/minecraft/server/players/NameAndId;Ljava/util/Optional;Ljava/util/Optional;)V",
            at = @At("TAIL"))
    private void tlm$onOp(NameAndId nameAndId, Optional<LevelBasedPermissionSet> permissions,
                          Optional<Boolean> bypassPlayerLimit, CallbackInfo ci) {
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
