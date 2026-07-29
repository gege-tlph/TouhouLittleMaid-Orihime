package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.summary.HistorySummaryManager;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.papi.PapiReplacer;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.papi.StringConstant;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.DefaultLLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMMessage;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAISite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSConfig;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSystemServices;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.data.ChatTokensAttachment;
import com.github.tartaricacid.touhoulittlemaid.entity.chatbubble.ChatBubbleManager;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataAttachment;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.TTSSystemAudioToClientPackage;
import com.github.tartaricacid.touhoulittlemaid.util.CappedQueue;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.util.Util;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.response.ResponseChat;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.Role;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.papi.StringConstant.AUTO_GEN_SETTING;

public final class MaidAIChatManager extends MaidAIChatData {
    private static final String DEEPSEEK_PLATFORM_URL = "https://platform.deepseek.com/";
    private final HistorySummaryManager historySummaryManager;

    public MaidAIChatManager(EntityMaid maid) {
        super(maid);
        this.historySummaryManager = new HistorySummaryManager(this);
    }

    public void chat(String message, ChatClientInfo clientInfo, ServerPlayer sender) {
        if (!ServerRuleConfig.get(AIConfig.LLM_ENABLED)) {
            sender.displayClientMessage(Component.translatable("ai.touhou_little_maid.chat.disable")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }
        ChatTokensAttachment chatTokens = sender.getAttachedOrCreate(InitDataAttachment.CHAT_TOKENS);
        if (chatTokens.get() >= ServerRuleConfig.get(AIConfig.MAX_TOKENS_PER_PLAYER)) {
            sender.displayClientMessage(Component.translatable("message.touhou_little_maid.ai_chat.max_tokens_limit")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }
        @Nullable LLMSite site = this.getLLMSite();
        if (site == null || !site.enabled()) {
            sender.displayClientMessage(Component.translatable("ai.touhou_little_maid.chat.llm.empty")
                    .withStyle(ChatFormatting.RED), false);
            return;
        }

        // 如果检测到是默认站点，那么大概率是新手玩家，给他提示去 DeepSeek 平台
        if (this.isDeepSeekSecretKeyMissing(site)) {
            this.sendDeepSeekTip(sender);
            return;
        }

        if (this.historySummaryManager.tryCompressBeforeChat(() -> this.tryToChat(message, clientInfo, site))) {
            return;
        }

        this.tryToChat(message, clientInfo, site);
    }

    private void sendDeepSeekTip(Player player) {
        MutableComponent tip = Component.translatable("ai.touhou_little_maid.chat.llm.deepseek_secret_key_missing")
                .withStyle(ChatFormatting.RED);
        MutableComponent url = Component.literal(DEEPSEEK_PLATFORM_URL);
        ClickEvent clickEvent = new ClickEvent.OpenUrl(URI.create(DEEPSEEK_PLATFORM_URL));
        HoverEvent hoverEvent = new HoverEvent.ShowText(Component.translatable("chat.link.open"));
        url.withStyle(style -> style.withHoverEvent(hoverEvent).withClickEvent(clickEvent)
                .withUnderlined(true).withColor(ChatFormatting.BLUE));
        player.displayClientMessage(tip, false);
        player.displayClientMessage(Component.translatable("ai.touhou_little_maid.chat.download_url").append(url), false);
    }

    private boolean isDeepSeekSecretKeyMissing(LLMSite site) {
        return site.id().equals(DefaultLLMSite.DEEPSEEK.id())
                && site instanceof LLMOpenAISite openAISite
                && StringUtils.isBlank(openAISite.secretKey());
    }

    private void tryToChat(String message, ChatClientInfo clientInfo, @NotNull LLMSite site) {
        this.chatLanguage = clientInfo.language();
        LLMClient chatClient = site.client();
        List<LLMMessage> messages = this.getMessages(this, clientInfo.language());
        if (messages.isEmpty()) {
            this.onSettingIsEmpty(clientInfo, chatClient);
        } else {
            HistoryMessagesCheck.checkMessages(messages);
            this.normalChat(message, messages, chatClient);
        }
    }

    private void normalChat(String message, List<LLMMessage> messages, LLMClient chatClient) {
        // 先插入临时的 context
        String messageWithContext = UserPromptContexts.addContext(this.maid, message);

        // http 通信添加 context
        messages.add(LLMMessage.userChat(this.maid, messageWithContext));
        // 历史记录不添加
        this.maid.getAiChatManager().addUserHistory(message);

        // 通信
        LLMCallback callback = new LLMCallback(this, messages);
        chatClient.chat(callback);
    }

    private void onSettingIsEmpty(ChatClientInfo clientInfo, LLMClient chatClient) {
        ChatBubbleManager bubbleManager = this.maid.getChatBubbleManager();
        if (ServerRuleConfig.get(AIConfig.AUTO_GEN_SETTING_ENABLED)) {
            // #4: AutoGenSettingCallback 误排除已恢复（服务端自动生成女仆人设；chatbubble 视觉反馈 P5 延后）
            List<LLMMessage> messages = this.autoGenSetting(this.maid, clientInfo);
            AutoGenSettingCallback callback = new AutoGenSettingCallback(this, messages);
            chatClient.chat(callback);
        } else {
            bubbleManager.addTextChatBubble("ai.touhou_little_maid.chat.llm.role_no_setting");
        }
    }

    @SuppressWarnings("all")
    public void tts(TTSSite site, String chatText, String ttsText, long waitingChatBubbleId) {
        // 调用系统 TTS，那么此时就只需要发送给指定的玩家即可
        TTSClient ttsClient = site.client();
        String ttsModel = getTTSModel();

        String ttsLang = "en";
        String[] split = this.getTTSLanguage().split("_");
        if (split.length >= 2) {
            ttsLang = split[0];
        }
        TTSConfig config = new TTSConfig(ttsModel, ttsLang);

        if (ttsClient instanceof TTSSystemServices services) {
            onPlaySoundLocal(site.id(), chatText, ttsText, config, services, waitingChatBubbleId);
        } else {
            TTSCallback callback = new TTSCallback(maid, chatText, waitingChatBubbleId);
            ttsClient.play(ttsText, config, callback);
        }
    }

    /**
     * 本次回复是否需要模型额外产出一段独立的 TTS 文本。
     *
     * <p>只有「TTS 确实会被调用」且「合成语言与聊天语言不同」时才需要。同语言时第二段是第一段的
     * 逐字副本，TTS 关闭或站点不可用时第二段生成完就被丢弃——两种情况下索取它都只是在多付一倍
     * 输出 token，还平白给正文里的 {@code ---} 一个被当成分隔符的机会。</p>
     *
     * <p>这组条件必须与 {@link LLMCallback#onSuccess} 里决定是否真的去合成的那组保持一致，
     * 一旦分叉就会出现「要了第二段却不用」或「用第二段却没要」。</p>
     */
    public boolean needsSeparateTtsText(String chatLanguage) {
        if (StringUtils.equals(chatLanguage, this.getTTSLanguage())) {
            return false;
        }
        TTSSite site = this.getTTSSite();
        return ServerRuleConfig.get(AIConfig.TTS_ENABLED) && site != null && site.enabled();
    }

    /**
     * 用本次对话已记录的聊天语言判定，见 {@link #needsSeparateTtsText(String)}
     */
    public boolean needsSeparateTtsText() {
        return this.needsSeparateTtsText(this.chatLanguage);
    }

    private List<LLMMessage> getMessages(MaidAIChatManager chatManager, String language) {
        // 如果含有自定义设定，则直接使用自定义设定
        if (StringUtils.isNotBlank(chatManager.customSetting)) {
            EntityMaid maid = chatManager.getMaid();
            String setting = PapiReplacer.replaceSetting(chatManager.customSetting, maid, language);
            return this.buildMessage(setting, maid, chatManager.getHistory(), language);
        }

        // 其他情况下，获取默认设定文件
        return chatManager.getSetting().map(s -> {
            EntityMaid maid = chatManager.getMaid();
            String setting = s.getSetting(maid, language);
            return this.buildMessage(setting, maid, chatManager.getHistory(), language);
        }).orElse(Lists.newArrayList());
    }

    /**
     * 根据女仆的设定和历史记录，构建发送给 LLM 的完整消息列表。
     * <p>
     * 最终结构为：{@code [SYSTEM 设定, SYSTEM 摘要(可选), ...历史记录(从旧到新), SYSTEM 权威要求]}
     *
     * <p>末尾那条是有意的：历史紧贴生成位置、权重高于开头的系统提示词，实测会让女仆照抄旧语言、
     * 或跟着一段纯聊天的历史继续不调用工具。详见 {@link StringConstant#HISTORY_IS_NOT_INSTRUCTION}。</p>
     */
    private List<LLMMessage> buildMessage(String setting, EntityMaid maid,
                                          CappedQueue<LLMMessage> history, String language) {
        List<LLMMessage> chatList = Lists.newArrayList();
        chatList.add(LLMMessage.systemChat(maid, setting));
        this.historySummaryManager.appendSummaryMessage(chatList);
        history.getDeque().descendingIterator()
                .forEachRemaining(message -> chatList.add(withoutTtsHalf(message)));
        chatList.add(LLMMessage.systemChat(maid, PapiReplacer.trailingRequirements(maid, language)));
        return chatList;
    }

    /**
     * 回放历史时剥掉旧版残留的 TTS 半段。
     *
     * <p>助手回复现在只存对话文本，但**已有存档里存的是 {@code chat---tts} 整串**，那些女仆的
     * 历史无法靠「以后不再写脏数据」自愈。这里在回放时归一化，让旧档立刻受益；
     * 无分隔符的新记录原样返回。</p>
     */
    static LLMMessage withoutTtsHalf(LLMMessage message) {
        if (message.role() != Role.ASSISTANT || StringUtils.isBlank(message.message())
                || !message.message().contains("---")) {
            return message;
        }
        return new LLMMessage(message.role(), new ResponseChat(message.message()).getChatText(),
                message.gameTime(), message.toolCalls(), message.toolCallId());
    }

    private List<LLMMessage> autoGenSetting(EntityMaid maid, ChatClientInfo clientInfo) {
        Map<String, String> valueMap = Util.make(Maps.newHashMap(), map -> {
            map.put("model_name", clientInfo.name());
            map.put("chat_language", clientInfo.language());
        });
        String setting = new StrSubstitutor(valueMap).replace(AUTO_GEN_SETTING);

        // 如果有描述文本，那么就将描述文本也加入到设定中
        if (!clientInfo.description().isEmpty()) {
            String join = StringUtils.join(clientInfo.description(), "\n");
            valueMap.put("model_desc", join);
            String desc = new StrSubstitutor(valueMap).replace(StringConstant.AUTO_GEN_SETTING_DESC);
            setting = setting + desc;
        }

        return Lists.newArrayList(LLMMessage.userChat(maid, setting));
    }

    private void onPlaySoundLocal(String name, String chatText, String ttsText, TTSConfig config,
                                  TTSSystemServices services, long waitingChatBubbleId) {
        if (!(maid.level instanceof ServerLevel serverLevel)) {
            return;
        }
        MinecraftServer server = serverLevel.getServer();
        server.submit(() -> {
            if (maid.getOwner() instanceof ServerPlayer player) {
                TTSSystemAudioToClientPackage message = new TTSSystemAudioToClientPackage(name, ttsText, config, services);
                ServerPlayNetworking.send(player, message);
            }
            maid.getChatBubbleManager().addLLMChatText(chatText, waitingChatBubbleId);
        });
    }

    public HistorySummaryManager getHistorySummaryManager() {
        return historySummaryManager;
    }
}
