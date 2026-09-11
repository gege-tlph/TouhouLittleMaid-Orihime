package com.github.tartaricacid.touhoulittlemaid.network.message.ai;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SiteSecretPresence;
import com.github.tartaricacid.touhoulittlemaid.util.GameModeUtil;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/**
 * 检查一个站点的配置：地址填没填、密钥填没填、那台主机连不连得上。
 *
 * <p><b>它叫「检查配置」而不是「测试连接」，是因为它确实没有验证密钥。</b>
 * 真正验证密钥要对每种服务各发一次真实请求（各家的请求体、鉴权头、错误码都不同），
 * 那是另一件工程；在此之前把它叫成「测试连接」，就是又一个说谎的标签——
 * 而本轮重构的起因之一，正是「服务器不提供」那句提示把三轮排查引向了从未损坏的链路。</p>
 *
 * <p>它仍然有实际价值：**把「地址/网络不通」与「密钥不对」分开**。
 * 这两种故障管理员的处置完全不同，而在此之前他只能看到一句笼统的失败。</p>
 *
 * <p><b>检查跑在服务端</b>——密钥本来就只在那一侧，客户端只发来站点 id。</p>
 */
public record CheckSiteConfigPackage(String service, String siteId) implements CustomPacketPayload {
    public static final String LLM = "llm";
    public static final String TTS = "tts";

    private static final int TIMEOUT_MS = 4000;
    private static final int MAX_ID_LENGTH = 64;

    /**
     * 回执配色。**必须带 alpha**：1.21.11 的 {@code Font} 不再把 alpha=0 补成不透明，
     * 照抄 {@code ChatFormatting} 那种裸 RGB 值会把字画成全透明。
     */
    private static final int COLOR_FAILURE = 0xFFFF5555;
    private static final int COLOR_WARNING = 0xFFFFFF55;
    private static final int COLOR_OK = 0xFF55FF55;

    public static final Type<CheckSiteConfigPackage> TYPE = new Type<>(modLoc("check_site_config"));
    public static final StreamCodec<ByteBuf, CheckSiteConfigPackage> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CheckSiteConfigPackage decode(ByteBuf byteBuf) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            return new CheckSiteConfigPackage(buf.readUtf(MAX_ID_LENGTH), buf.readUtf(MAX_ID_LENGTH));
        }

        @Override
        public void encode(ByteBuf byteBuf, CheckSiteConfigPackage message) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            buf.writeUtf(message.service, MAX_ID_LENGTH);
            buf.writeUtf(message.siteId, MAX_ID_LENGTH);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CheckSiteConfigPackage message, ServerPlayNetworking.Context context) {
        context.server().execute(() -> check(message, context.player(), context.server()));
    }

    /**
     * 每玩家的并发闸：一个在途探测 + 1 秒冷却。
     *
     * <p>每次探测都是一条阻塞原生线程，最长 4 秒（而且 {@code InetSocketAddress} 的 DNS 解析
     * 发生在这个超时之外，黑洞域名会更久）。这是 AI 网络面上仅剩的一处裸 {@code new Thread}。
     * 只有 {@code canEditSite} 的人能触发，所以这不是安全洞，是卫生问题——但没有上限的东西
     * 迟早会被人按住不放。判据与试听那道限频同形，别各写各的。</p>
     */
    private static final long COOLDOWN_MS = 1000;
    private static final java.util.Map<java.util.UUID, Long> LAST_ACCEPTED = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Set<java.util.UUID> IN_FLIGHT =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    static boolean tryAcquire(java.util.UUID playerId, long nowMs) {
        // 过期条目对判定毫无作用：冷却窗口之外的记录本来就一律放行。留着它们只会让这张表
        // 随「历史上点过这个按钮的独立玩家数」无上限增长，而那个键空间没有上限，也没有下线钩子
        // 来清它。清理判据与下面那条检查严格互补，所以这是纯粹的回收，不改任何放行/拒绝结果。
        LAST_ACCEPTED.entrySet().removeIf(entry -> nowMs - entry.getValue() >= COOLDOWN_MS);
        if (IN_FLIGHT.contains(playerId)) {
            return false;
        }
        Long previous = LAST_ACCEPTED.get(playerId);
        if (previous != null && nowMs - previous < COOLDOWN_MS) {
            return false;
        }
        LAST_ACCEPTED.put(playerId, nowMs);
        IN_FLIGHT.add(playerId);
        return true;
    }

    static void release(java.util.UUID playerId) {
        IN_FLIGHT.remove(playerId);
    }

    /**
     * 探测体的执行壳：无论 body 以哪种方式结束（正常返回 / Exception / Error），在途额度都必须归还。
     *
     * <p>单独抽出来只为一件事——让「Error 逃逸时也归还」这条<b>可直测</b>，而且产品与用例走的是
     * 同一条实现。方法有两种逃逸方式，{@code return} 与 {@code throw}，只盯前一种的看守等于没看守。</p>
     */
    static void runProbe(java.util.UUID owner, Runnable body) {
        try {
            body.run();
        } finally {
            release(owner);
        }
    }

    private static void check(CheckSiteConfigPackage message, @Nullable ServerPlayer player, MinecraftServer server) {
        if (player == null || !GameModeUtil.canEditSite(player)) {
            return;
        }
        Site site = switch (message.service) {
            case LLM -> AvailableSites.getLLMSite(message.siteId);
            case TTS -> AvailableSites.getTTSSite(message.siteId);
            default -> null;
        };
        if (site == null) {
            reply(player, "missing", COLOR_FAILURE, message.siteId);
            return;
        }
        if (StringUtils.isBlank(site.url())) {
            reply(player, "no_url", COLOR_FAILURE, message.siteId);
            return;
        }
        if (!hasSecret(site)) {
            reply(player, "no_secret", COLOR_WARNING, message.siteId);
            return;
        }

        // 闸放在三个早退判定之后：那几条都不开线程，没必要占额度
        java.util.UUID owner = player.getUUID();
        if (!tryAcquire(owner, System.currentTimeMillis())) {
            reply(player, "busy", COLOR_WARNING, message.siteId);
            return;
        }

        // 网络等待不能压在主线程上：这里最长会阻塞 TIMEOUT_MS。
        String url = site.url();
        String id = message.siteId;
        // release 走 runProbe 的 finally，不挂在 server.execute 的任务里：
        //   ① reachabilityFailure 只 catch Exception，Error（如 LOGGER 持有类静态初始化失败）
        //      会让这条线程在 execute 入队之前就死掉；
        //   ② 服务器关闭时排进去的任务不保证被排空。
        // 两条都会把 owner 永久留在 IN_FLIGHT 里——那不只是泄漏，是把这个玩家**永久锁死**在这个
        // 按钮之外。闸的语义是「一个在途探测」，探测结束即释放才是它本来的意思；比原先晚放到回执
        // 渲染时才释放更准确，而 1 秒冷却仍然照常拦连点。
        Thread probe = new Thread(() -> runProbe(owner, () -> {
            String failure = reachabilityFailure(url);
            server.execute(() -> {
                if (failure == null) {
                    reply(player, "reachable", COLOR_OK, id);
                } else {
                    reply(player, "unreachable", COLOR_FAILURE, id, failure);
                }
            });
        }), "tlm-site-config-check");
        probe.setDaemon(true);
        probe.start();
    }

    /**
     * 只探到 TCP 建连为止。不发 HTTP 请求：各家 API 对 HEAD / 空 GET 的反应千奇百怪，
     * 拿那种响应下结论只会制造新的假信号，而**建连成功已经足以把「地址或网络不通」摘出去**。
     */
    private static @Nullable String reachabilityFailure(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) {
                return "invalid url";
            }
            int port = uri.getPort() > 0 ? uri.getPort() : ("http".equalsIgnoreCase(uri.getScheme()) ? 80 : 443);
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            }
            return null;
        } catch (Exception exception) {
            TouhouLittleMaid.LOGGER.debug("Site config check failed for {}: {}", url, exception.toString());
            String message = exception.getMessage();
            return StringUtils.isBlank(message) ? exception.getClass().getSimpleName() : message;
        }
    }

    private static boolean hasSecret(Site site) {
        return SiteSecretPresence.hasAnySecret(site);
    }

    /**
     * 回执发回**发起检查的那个界面**，不再打进聊天栏。
     *
     * <p>这个按钮只存在于站点编辑屏上，点它的时候那个屏必然开着，而**聊天栏在界面底下看不见**——
     * 原先的 {@code displayClientMessage} 等于把回执写到一个当时读不到的地方，管理员点完按钮
     * 屏幕上什么也没有。屏已关掉时由客户端自行回落到聊天栏，见 {@code SiteCheckResultDisplay}。</p>
     */
    private static void reply(ServerPlayer player, String key, int argb, Object... args) {
        List<String> stringArgs = Arrays.stream(args).map(String::valueOf).toList();
        ServerPlayNetworking.send(player,
                new SiteCheckResultPackage("ai.touhou_little_maid.chat.site_check." + key, stringArgs, argb));
    }
}
