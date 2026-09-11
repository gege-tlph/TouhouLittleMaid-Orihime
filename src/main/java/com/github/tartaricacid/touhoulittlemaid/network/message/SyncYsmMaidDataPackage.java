package com.github.tartaricacid.touhoulittlemaid.network.message;

import com.github.tartaricacid.touhoulittlemaid.network.client.SyncYsmMaidDataPackageProxy;
import com.github.tartaricacid.touhoulittlemaid.util.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/**
 * 把女仆的 YSM 轮盘动画状态从服务端广播到客户端。
 *
 * <p>在此之前 {@code EntityMaid.rouletteAnim*} 三个字段是<b>纯字段</b>，没有任何同步通道：
 * 单人档里服务端与客户端渲染的是同一个实例，字段直接可见所以看不出问题；专服上客户端
 * 永远看不到服务端的写入，轮盘动画从来不播。</p>
 *
 * <p><b>{@code rouletteAnimDirty} 刻意不在线上。</b>它在两侧的角色不同，做成 wire field
 * 会让两个语义打架：</p>
 * <ul>
 *   <li>服务端 = <b>出站触发器</b>。{@code EntityMaid.tick()} 里读到它为真就发一次包并置回 false。</li>
 *   <li>客户端 = <b>渲染端脏标记</b>。收包后由 {@code playRouletteAnim}/{@code stopRouletteAnim}
 *       置回 true，OpenYSM 侧的 predicate 读到它就先刷新模型再停转场。</li>
 * </ul>
 *
 * <p><b>{@code roamingVars} 保留字段但恒为空表</b>，这是与 OpenYSM 侧对齐后的有意决定：
 * 本树的 {@code EntityMaid} 根本没有 {@code roamingVars} 字段（1.21.11 上它是字段 + NBT
 * 持久化 + 渲染器接线的一整套），而它唯一的终点 {@link com.github.tartaricacid.touhoulittlemaid.geckolib3.geo.IGeoEntity#updateRoamingVars}
 * 在 OpenYSM 的两条版本线上都是空实现。两端都没有消费者，故不建这条通道；
 * 字段留在 record 里是为了保持与 1.21.11 逐字一致的报文形状，将来真要接时不必改协议。</p>
 */
public record SyncYsmMaidDataPackage(int entityId, String rouletteAnim, boolean isRouletteAnimPlaying,
                                     Object2FloatOpenHashMap<String> roamingVars) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncYsmMaidDataPackage> TYPE =
            new CustomPacketPayload.Type<>(modLoc("sync_ysm_maid_data"));
    public static final StreamCodec<ByteBuf, SyncYsmMaidDataPackage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SyncYsmMaidDataPackage::entityId,
            ByteBufCodecs.STRING_UTF8,
            SyncYsmMaidDataPackage::rouletteAnim,
            ByteBufCodecs.BOOL,
            SyncYsmMaidDataPackage::isRouletteAnimPlaying,
            ByteBufUtils.OBJECT_2_FLOAT_OPEN_HASH_MAP_CODEC,
            SyncYsmMaidDataPackage::roamingVars,
            SyncYsmMaidDataPackage::new
    );

    public static void handle(SyncYsmMaidDataPackage message, ClientPlayNetworking.Context context) {
        context.client().execute(() -> SyncYsmMaidDataPackageProxy.handle(message));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
