package com.github.tartaricacid.touhoulittlemaid.ai.service.tts;


import com.github.tartaricacid.touhoulittlemaid.ai.service.Client;
import com.github.tartaricacid.touhoulittlemaid.ai.service.ErrorCode;

import javax.annotation.Nullable;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public interface TTSClient extends Client {
    /**
     * 语音合成接口
     *
     * @param message  需要合成的文本
     * @param config   语音合成配置
     * @param callback 回调，返回合成的音频数据
     */
    void play(String message, TTSConfig config, TTSResponse callback);

    /**
     * 提供的工具方法，用来处理 HTTP 响应信息
     *
     * @param callback  回调
     * @param response  响应信息
     * @param throwable 响应的错误，没有错误时为 null
     * @param request   之前 HTTP 发送的的请求
     */
    default void handleResponse(TTSResponse callback, HttpResponse<byte[]> response,
                                @Nullable Throwable throwable, HttpRequest request) {
        // 结果是否已无意义由回调自己判定：女仆流看女仆死活，试听流看请求是否过期
        if (callback.isObsolete()) {
            return;
        }
        if (throwable != null) {
            callback.onFailure(request, throwable, ErrorCode.REQUEST_SENDING_ERROR);
            return;
        }
        if (isSuccessful(response)) {
            callback.onSuccess(response.body());
        } else {
            String errorMsg = new String(response.body(), StandardCharsets.UTF_8);
            String message = "HTTP Error Code: %d, Response %s".formatted(response.statusCode(), errorMsg);
            callback.onFailure(request, new Throwable(message), ErrorCode.REQUEST_RECEIVED_ERROR);
        }
    }
}
