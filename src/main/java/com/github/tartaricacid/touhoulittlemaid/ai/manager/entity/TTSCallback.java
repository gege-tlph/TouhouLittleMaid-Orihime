package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ErrorCode;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ResponseCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSResponse;
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

/**
 * 女仆对话链路上的 TTS 回调。实现 {@link TTSResponse} 而不是让 client 直接吃本类——
 * 「结果还有没有意义」在这条链路上是「女仆还活着吗」，在音色试听那条链路上是别的语义，
 * 各自答各自的（{@link TTSResponse#isObsolete()} 的默认实现正是本链路的答案）。
 */
public class TTSCallback implements TTSResponse {
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
        TouhouLittleMaid.LOGGER.error("LLM request failed: {}, error is {}", request, throwable.getMessage());
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