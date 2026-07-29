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
import net.minecraft.world.entity.LivingEntity;

import java.net.http.HttpRequest;

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
                    player.displayClientMessage(errorMessage.withStyle(ChatFormatting.RED), false);
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
        if (!(maid.level instanceof ServerLevel serverLevel)) {
            return;
        }
        LivingEntity owner = maid.getOwner();
        if (!(owner instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = serverLevel.getServer();
        server.submit(() -> {
            ServerPlayNetworking.send(player, new TTSAudioToClientPackage(maid.getId(), data));
            maid.getChatBubbleManager().addLLMChatText(chatText, waitingChatBubbleId);
        });
    }

    public EntityMaid getMaid() {
        return maid;
    }
}
