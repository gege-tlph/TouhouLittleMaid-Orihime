package com.github.tartaricacid.touhoulittlemaid.config;

import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTApiType;

/**
 * 可由服务器向玩家发布的 STT 类型。Player2 固定连接玩家本机，不能作为服务器付费站点。
 */
public enum ServerSTTApiType {
    ALIYUN(STTApiType.ALIYUN),
    SILICONFLOW(STTApiType.SILICONFLOW),
    TENCENT(STTApiType.TENCENT);

    private final STTApiType apiType;

    ServerSTTApiType(STTApiType apiType) {
        this.apiType = apiType;
    }

    public STTApiType apiType() {
        return apiType;
    }

    public String siteId() {
        return apiType.getName();
    }
}
