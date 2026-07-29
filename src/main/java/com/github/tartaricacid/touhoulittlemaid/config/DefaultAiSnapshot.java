package com.github.tartaricacid.touhoulittlemaid.config;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SupportModelSelect;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.apache.commons.lang3.StringUtils;

/**
 * 世界默认值（默认模型 / 默认音色）的变更检测与在线告知。
 *
 * <p><b>为什么必须是一个共享实现</b>：默认值有两条生效路径——集成服务器上保存即激活
 * （{@code SaveServerRulesPacket}），专用服务器上写文件后经 {@code /tlm config reload} 激活
 * （{@code ConfigCommand}）。告知只挂其中一条，就会出现「配置已生效但玩家完全不知情」的半条路，
 * 而那正是跟随语义最伤人的失败方式：女仆悄悄换了模型，玩家只觉得她变笨了。
 * 两个调用点由 {@code AiReloadWiringContractTest} 钉死。</p>
 *
 * <p>用法：激活前 {@link #capture()}，激活成功后 {@link #diffAndNotify(MinecraftServer)}。</p>
 */
public final class DefaultAiSnapshot {
    private final String llmSite;
    private final String llmModel;
    private final String ttsSite;
    private final String ttsModel;

    private DefaultAiSnapshot(String llmSite, String llmModel, String ttsSite, String ttsModel) {
        this.llmSite = llmSite;
        this.llmModel = llmModel;
        this.ttsSite = ttsSite;
        this.ttsModel = ttsModel;
    }

    public static DefaultAiSnapshot capture() {
        return new DefaultAiSnapshot(
                ServerRuleConfig.get(AIConfig.DEFAULT_LLM_SITE),
                ServerRuleConfig.get(AIConfig.DEFAULT_LLM_MODEL),
                ServerRuleConfig.get(AIConfig.DEFAULT_TTS_SITE),
                ServerRuleConfig.get(AIConfig.DEFAULT_TTS_MODEL));
    }

    /** 与当前生效值比较，LLM 对或 TTS 对有变化就各广播一条本地化聊天提示。 */
    public void diffAndNotify(MinecraftServer server) {
        String newLlmSite = ServerRuleConfig.get(AIConfig.DEFAULT_LLM_SITE);
        String newLlmModel = ServerRuleConfig.get(AIConfig.DEFAULT_LLM_MODEL);
        if (!StringUtils.equals(llmSite, newLlmSite) || !StringUtils.equals(llmModel, newLlmModel)) {
            broadcast(server, "ai.touhou_little_maid.chat.notice.default_llm_changed",
                    newLlmSite, newLlmModel, AvailableSites.getLLMSite(newLlmSite));
        }
        String newTtsSite = ServerRuleConfig.get(AIConfig.DEFAULT_TTS_SITE);
        String newTtsModel = ServerRuleConfig.get(AIConfig.DEFAULT_TTS_MODEL);
        if (!StringUtils.equals(ttsSite, newTtsSite) || !StringUtils.equals(ttsModel, newTtsModel)) {
            broadcast(server, "ai.touhou_little_maid.chat.notice.default_tts_changed",
                    newTtsSite, newTtsModel, AvailableSites.getTTSSite(newTtsSite));
        }
    }

    /**
     * 站点名用嵌套 translatable（客户端本地化），模型显示名服务端从站点的模型表里取；
     * 默认清空（回到内置兜底）时站点名显示兜底语义由空串退化承担——不额外造词。
     */
    private static void broadcast(MinecraftServer server, String key,
                                  String siteId, String modelId, Site site) {
        Component siteName = StringUtils.isBlank(siteId)
                ? Component.translatable("ai.touhou_little_maid.chat.site.none.name")
                : Component.translatable("ai.touhou_little_maid.chat.site.%s.name".formatted(siteId));
        String modelName = modelId;
        if (site instanceof SupportModelSelect select && select.models().containsKey(modelId)) {
            modelName = select.models().get(modelId);
        }
        Component message = Component.translatable(key, siteName,
                StringUtils.defaultIfBlank(modelName, "-"));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.displayClientMessage(message, false);
        }
    }
}
