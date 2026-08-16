package com.github.tartaricacid.touhoulittlemaid.network.message.ai;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SiteSecretPresence;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SupportModelSelect;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSConfig;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSResponse;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSystemServices;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.net.http.HttpRequest;

import static com.github.tartaricacid.touhoulittlemaid.util.IdentifierUtil.modLoc;

/**
 * 请求试听一个音色（C2S）。合成在服务端做——密钥只在那一侧。
 *
 * <p>不加额外权限门：与「不加滥用门禁」的定案一致，正常聊天本就会产生同等的合成调用，
 * 试听只是把同一次调用提前到选择时。系统语音不该走到这里（客户端本地直合成），
 * 服务端仍然防御性拦截——防的是未来某个客户端改动忘了这条特例。</p>
 */
public record RequestVoicePreviewPackage(String site, String model, int requestId,
                                         String language) implements CustomPacketPayload {
    public static final Type<RequestVoicePreviewPackage> TYPE = new Type<>(modLoc("request_voice_preview"));
    private static final int MAX_ID = 64;

    /**
     * 服务端限频：云合成有真实成本。正常客户端本就单请求在途（{@code VoicePreviewClient}
     * 的 pending 闸），这道闸只拦改装客户端的刷量——「不加滥用门禁」的定案针对的是权限门，
     * 不是让人白嫖无限合成调用。拒绝不滑动窗口：连点不会把冷却越推越远。
     */
    public static final long COOLDOWN_MS = 1000;
    private static final java.util.Map<java.util.UUID, Long> LAST_ACCEPTED = new java.util.concurrent.ConcurrentHashMap<>();

    /** 只给看门狗用的单线程定时器；守护线程，不拖住服务器关闭 */
    private static final java.util.concurrent.ScheduledExecutorService WATCHDOG =
            java.util.concurrent.Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "tlm-voice-preview-watchdog");
                thread.setDaemon(true);
                return thread;
            });

    static boolean tryAcquire(java.util.UUID playerId, long nowMs) {
        Long previous = LAST_ACCEPTED.get(playerId);
        if (previous != null && nowMs - previous < COOLDOWN_MS) {
            return false;
        }
        LAST_ACCEPTED.put(playerId, nowMs);
        return true;
    }

    /** 样例文本按**请求方客户端的界面语言**二选一——预览给谁听，就说谁的语言 */
    private static final String SAMPLE_ZH = "你好呀，主人，今天想听我说点什么？";
    private static final String SAMPLE_EN = "Hello, master. How do I sound today?";

    public static final StreamCodec<ByteBuf, RequestVoicePreviewPackage> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RequestVoicePreviewPackage decode(ByteBuf byteBuf) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            return new RequestVoicePreviewPackage(buf.readUtf(MAX_ID), buf.readUtf(MAX_ID), buf.readVarInt(), buf.readUtf(MAX_ID));
        }

        @Override
        public void encode(ByteBuf byteBuf, RequestVoicePreviewPackage message) {
            FriendlyByteBuf buf = new FriendlyByteBuf(byteBuf);
            buf.writeUtf(message.site, MAX_ID);
            buf.writeUtf(message.model, MAX_ID);
            buf.writeVarInt(message.requestId);
            buf.writeUtf(message.language, MAX_ID);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 校验结论。除 OK 外都映射到一条本地化的「不可用」原因。 */
    public enum Validation {
        OK(null),
        MISSING("ai.touhou_little_maid.chat.preview.unavailable.missing"),
        DISABLED("ai.touhou_little_maid.chat.preview.unavailable.disabled"),
        CLIENT_LOCAL("ai.touhou_little_maid.chat.preview.unavailable.client_local"),
        BAD_MODEL("ai.touhou_little_maid.chat.preview.unavailable.bad_model");

        public final String detailKey;

        Validation(String detailKey) {
            this.detailKey = detailKey;
        }
    }

    /**
     * 静态校验，JUnit 直测四个不可用分支——它们不需要网络，也不该需要。
     *
     * <p><b>「本地播放」按 client 类型判，不按站点 id 判。</b>原先只认 {@code system} 这个 id，
     * 于是 Player2 被当成云站点送进服务端合成——而它的 client 同样只在物理客户端动作、
     * 且**丢弃 callback**，结果服务端既不出声也永不回包，玩家干等 10 秒超时。
     * 聊天路径早就是按类型判的（{@code MaidAIChatManager} 里的 {@code instanceof TTSSystemServices}），
     * 这里跟上；将来再加本地播放的站点也自动覆盖。</p>
     */
    public static Validation validate(String siteId, String modelId) {
        TTSSite site = AvailableSites.getTTSSite(siteId);
        if (site == null) {
            return Validation.MISSING;
        }
        if (!site.enabled()) {
            return Validation.DISABLED;
        }
        if (site.client() instanceof TTSSystemServices) {
            return Validation.CLIENT_LOCAL;
        }
        if (StringUtils.isNotBlank(modelId) && site instanceof SupportModelSelect select
                && !select.models().containsKey(modelId)) {
            return Validation.BAD_MODEL;
        }
        return Validation.OK;
    }

    public static void handle(RequestVoicePreviewPackage message, ServerPlayNetworking.Context context) {
        context.server().execute(() -> run(message, context.player(), context.server()));
    }

    private static void run(RequestVoicePreviewPackage message, @Nullable ServerPlayer player, MinecraftServer server) {
        if (player == null) {
            return;
        }
        if (!tryAcquire(player.getUUID(), System.currentTimeMillis())) {
            reply(server, player, message.requestId,
                    VoicePreviewResultPackage.Status.FAILED, "ai.touhou_little_maid.chat.preview.rate_limited");
            return;
        }
        Validation validation = validate(message.site, message.model);
        if (validation != Validation.OK) {
            reply(server, player, message.requestId,
                    VoicePreviewResultPackage.Status.UNAVAILABLE, validation.detailKey);
            return;
        }
        TTSSite site = AvailableSites.getTTSSite(message.site);
        if (!SiteSecretPresence.hasAnySecret(site)) {
            reply(server, player, message.requestId,
                    VoicePreviewResultPackage.Status.FAILED, "ai.touhou_little_maid.chat.preview.no_secret");
            return;
        }

        String effectiveModel = message.model;
        if (StringUtils.isBlank(effectiveModel) && site instanceof SupportModelSelect select
                && !select.models().isEmpty()) {
            effectiveModel = select.getDefaultModel();
        }
        String language = message.language;
        String languagePrefix = language.contains("_") ? language.split("_")[0] : language;
        String sample = "zh".equals(languagePrefix) ? SAMPLE_ZH : SAMPLE_EN;

        PreviewResponse response = new PreviewResponse(server, player, message.requestId);
        response.arm();
        site.client().play(sample, new TTSConfig(effectiveModel, languagePrefix), response);
    }

    private static void reply(MinecraftServer server, ServerPlayer player, int requestId,
                              VoicePreviewResultPackage.Status status, String detail) {
        send(server, player, new VoicePreviewResultPackage(requestId, status, StringUtils.defaultString(detail), new byte[0]));
    }

    /** 云合成要几秒，玩家可能中途退服——给已断线的连接发包只会白抛异常 */
    private static void send(MinecraftServer server, ServerPlayer player, VoicePreviewResultPackage message) {
        server.execute(() -> {
            if (!player.hasDisconnected()) {
                ServerPlayNetworking.send(player, message);
            }
        });
    }

    /**
     * 无主合成的回调：没有女仆，「结果是否作废」由客户端的 requestId 判定，服务端一律送达。
     *
     * <p><b>终态由服务端负责，且恰好一个。</b>闩住第一个结果（某个 client 双调也只算一次），
     * 并挂一只看门狗：站点的 client 若压根不回调（Player2 那种「只在客户端动作、丢弃 callback」的形态
     * 就是这样），到点自己发一条失败。看门狗用**独立的**文案键——
     * 「站点从没回话」和「网络超时」必须能分辨，否则下一次排查又会被文案带偏。</p>
     */
    private static final class PreviewResponse implements TTSResponse {
        /** 比客户端 10 秒超时晚一点：正常情况下客户端先自己放弃，这只狗只在它没放弃时兜底 */
        private static final long WATCHDOG_MS = 15_000;

        private final MinecraftServer server;
        private final ServerPlayer player;
        private final int requestId;
        private final java.util.concurrent.atomic.AtomicBoolean settled =
                new java.util.concurrent.atomic.AtomicBoolean();

        private PreviewResponse(MinecraftServer server, ServerPlayer player, int requestId) {
            this.server = server;
            this.player = player;
            this.requestId = requestId;
        }

        /** 交给站点 client 之前武装；到点仍未落定就自己发终态 */
        private void arm() {
            WATCHDOG.schedule(() -> {
                if (this.settled.compareAndSet(false, true)) {
                    TouhouLittleMaid.LOGGER.warn("Voice preview {} got no answer from its site client", this.requestId);
                    reply(this.server, this.player, this.requestId, VoicePreviewResultPackage.Status.FAILED,
                            "ai.touhou_little_maid.chat.preview.no_response");
                }
            }, WATCHDOG_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
        }

        @Override
        public @Nullable EntityMaid getMaid() {
            return null;
        }

        @Override
        public boolean isObsolete() {
            return false;
        }

        @Override
        public void onSuccess(byte[] data) {
            if (this.settled.compareAndSet(false, true)) {
                send(this.server, this.player,
                        new VoicePreviewResultPackage(this.requestId, VoicePreviewResultPackage.Status.OK, "", data));
            }
        }

        @Override
        public void onFailure(HttpRequest request, Throwable throwable, int errorCode) {
            if (!this.settled.compareAndSet(false, true)) {
                return;
            }
            // 原因原样给管理员看（HTTP 401 之类正是他需要的），但不透传我们自己的请求内容
            String cause = StringUtils.abbreviate(
                    StringUtils.defaultIfBlank(throwable.getLocalizedMessage(), throwable.getClass().getSimpleName()), 300);
            TouhouLittleMaid.LOGGER.warn("Voice preview failed for site {}: {}", this.requestId, cause);
            reply(this.server, this.player, this.requestId, VoicePreviewResultPackage.Status.FAILED, cause);
        }
    }
}
