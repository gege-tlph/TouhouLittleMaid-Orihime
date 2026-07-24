package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ErrorCode;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ResponseCallback;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ServiceType;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.message.SendUserChatPackage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.StringUtils;

import java.net.http.HttpRequest;

@Environment(EnvType.CLIENT)
public class STTCallback implements ResponseCallback<String> {
    private final Player player;
    private final EntityMaid maid;
    private final boolean serverProvided;

    public STTCallback(Player player, EntityMaid maid) {
        this(player, maid, false);
    }

    public STTCallback(Player player, EntityMaid maid, boolean serverProvided) {
        this.player = player;
        this.maid = maid;
        this.serverProvided = serverProvided;
    }

    @Override
    public void onFailure(HttpRequest request, Throwable throwable, int errorCode) {
        TouhouLittleMaid.LOGGER.error("STT request failed: {}, error is {}", request, throwable.getMessage());
        Minecraft.getInstance().execute(() -> {
            if (serverProvided && errorCode != ErrorCode.MICROPHONE_NOT_FOUND) {
                player.displayClientMessage(Component.translatable(
                        "ai.touhou_little_maid.chat.stt.server_connection_failed")
                        .withStyle(ChatFormatting.RED), false);
                return;
            }
            String cause = throwable.getLocalizedMessage();
            MutableComponent errorMessage = ErrorCode.getErrorMessage(ServiceType.STT, errorCode, cause);
            player.displayClientMessage(errorMessage.withStyle(ChatFormatting.RED), false);
        });
    }

    @Override
    public void onSuccess(String chatText) {
        Minecraft.getInstance().execute(() -> {
            if (StringUtils.isNotBlank(chatText)) {
                ChatClientInfo clientInfo = ChatClientInfo.fromMaid(this.maid);
                ClientPlayNetworking.send(new SendUserChatPackage(maid.getId(), chatText, clientInfo));
                String name = player.getScoreboardName();
                String format = String.format("<%s> %s", name, chatText);
                player.displayClientMessage(Component.literal(format).withStyle(ChatFormatting.GRAY), false);
            } else {
                MutableComponent component = Component.translatable("ai.touhou_little_maid.chat.stt.content_is_empty");
                player.displayClientMessage(component.withStyle(ChatFormatting.GRAY), false);
            }
        });
    }
}
