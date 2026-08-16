package com.github.tartaricacid.touhoulittlemaid.ai.service.tts;

import com.github.tartaricacid.touhoulittlemaid.ai.service.ResponseCallback;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;

import javax.annotation.Nullable;

/**
 * TTS 合成回调的最小接口。
 *
 * <p>原先 {@code TTSClient.play} 直接吃具体类 {@code TTSCallback}，而它绑死了一只女仆——
 * 「结果还有没有意义」被写成了「女仆还活着吗」。音色试听没有女仆，语义是「界面还开着吗」，
 * 于是把这个判断交还给回调自己：{@link #isObsolete()}。各站点 client 的 HTTP 逻辑不变。</p>
 */
public interface TTSResponse extends ResponseCallback<byte[]> {
    /**
     * 这次合成绑定的女仆；无主合成（音色试听）返回 null。
     */
    @Nullable
    EntityMaid getMaid();

    /**
     * 结果是否已无意义。默认按女仆语义：没有女仆或女仆已死即作废——
     * 这是原 {@code shouldStopChat} 的行为，绑定女仆的调用方不需要改动。
     * 试听回调覆写为 false（过期由 requestId 在客户端判定）。
     */
    default boolean isObsolete() {
        EntityMaid maid = getMaid();
        return maid == null || !maid.isAlive();
    }
}
