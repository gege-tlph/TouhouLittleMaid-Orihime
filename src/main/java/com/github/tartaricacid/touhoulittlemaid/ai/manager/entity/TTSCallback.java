package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ErrorCode;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ResponseCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.TTSAudioToClientPackage;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.LivingEntity;

import java.net.http.HttpRequest;
import java.util.UUID;

public class TTSCallback implements com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSResponse {
    private final EntityMaid maid;
    private final String chatText;
    private final long waitingChatBubbleId;

    public TTSCallback(EntityMaid maid, String chatText, long waitingChatBubbleId) {
        this.maid = maid;
        this.chatText = chatText;
        this.waitingChatBubbleId = waitingChatBubbleId;
    }

    @Override
    public void onFailure(HttpRequest request, Throwable throwable, int errorCode) {
        if (maid.level instanceof ServerLevel serverLevel) {
            MinecraftServer server = serverLevel.getServer();
            server.submit(() -> {
                if (maid.getOwner() instanceof ServerPlayer player) {
                    String cause = throwable.getLocalizedMessage();
                    MutableComponent errorMessage = ErrorCode.getErrorMessage(ServiceType.TTS, errorCode, cause);
                    player.sendSystemMessage(errorMessage.withStyle(ChatFormatting.RED));
                }
                maid.getChatBubbleManager().addLLMChatText(chatText, waitingChatBubbleId);
            });
        }
        // 这里是 TTS 的失败，原先却记成 "LLM request failed"——按日志找故障的人会被送去查错误的服务，
        // 而「文案指向哪里故障就在哪里」正是本项目栽过的坑
        TouhouLittleMaid.LOGGER.error("TTS request failed: {}, error is {}", request, throwable.getMessage());
    }

    @Override
    public void onSuccess(byte[] data) {
        // 下面两处原先都是静默 return：音频已经合成好了（还花了钱），却在最后一步被丢掉，
        // 玩家看到的是「气泡出字、没有声音」，与「根本没发起合成」一模一样。
        // 与 TTSAudioToClientPackageProxy 对齐：每一条丢弃都要说出自己是谁。
        if (!(maid.level instanceof ServerLevel serverLevel)) {
            TouhouLittleMaid.LOGGER.warn("Dropped synthesized TTS audio for maid {}: she is no longer on a server level",
                    maid.getId());
            return;
        }
        LivingEntity owner = maid.getOwner();
        if (!(owner instanceof ServerPlayer player)) {
            logUnreachableOwner(serverLevel);
            return;
        }
        MinecraftServer server = serverLevel.getServer();
        server.submit(() -> {
            ServerPlayNetworking.send(player, new TTSAudioToClientPackage(maid.getId(), data));
            maid.getChatBubbleManager().addLLMChatText(chatText, waitingChatBubbleId);
        });
    }

    /**
     * {@code getOwner()} 只在女仆**所在的那层世界**里按 UUID 找主人，所以「主人在线但在别的维度」
     * 与「主人不在线」表现完全一样——一段已经合成好的音频被静默丢掉。两者的处置完全不同
     * （前者是设计使然，后者说明这次合成从一开始就白花），因此必须分开记。
     */
    private void logUnreachableOwner(ServerLevel serverLevel) {
        EntityReference<LivingEntity> reference = maid.getOwnerReference();
        UUID ownerId = reference == null ? null : reference.getUUID();
        ServerPlayer online = ownerId == null ? null : serverLevel.getServer().getPlayerList().getPlayer(ownerId);
        if (ownerId == null) {
            TouhouLittleMaid.LOGGER.warn("Dropped synthesized TTS audio for maid {}: she has no owner", maid.getId());
        } else if (online != null) {
            TouhouLittleMaid.LOGGER.warn(
                    "Dropped synthesized TTS audio for maid {}: her owner {} is online but in {}, while she is in {}"
                            + " (audio is only ever sent to the owner)",
                    maid.getId(), ownerId, online.level().dimension().identifier(),
                    serverLevel.dimension().identifier());
        } else {
            TouhouLittleMaid.LOGGER.warn("Dropped synthesized TTS audio for maid {}: her owner {} is not online",
                    maid.getId(), ownerId);
        }
    }

    public EntityMaid getMaid() {
        return maid;
    }
}
