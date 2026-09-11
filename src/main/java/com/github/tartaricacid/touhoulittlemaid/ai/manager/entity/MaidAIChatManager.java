package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.summary.HistorySummaryManager;
import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.papi.PapiReplacer;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.papi.StringConstant;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.DefaultLLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMClient;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMMessage;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.response.FunctionToolCall;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.response.ToolCall;
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
import com.github.tartaricacid.touhoulittlemaid.entity.passive.MaidManagerDef;
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

// 代码宿主用 manager codegen 生成 EntityMaid 上的字段与访问器（EntityMaid.aiChatManager /
// getAiChatManager()）。行为基准没有这套机制，故其同名文件上没有本注解——**搬运时必须补回**，
// 漏了不是本类编译不过，而是 EntityMaid 那边的字段整个不存在。
@MaidManagerDef(alias = "aiChatManager", exposeView = false)
public final class MaidAIChatManager extends MaidAIChatData {
    private static final String DEEPSEEK_PLATFORM_URL = "https://platform.deepseek.com/";
    private final HistorySummaryManager historySummaryManager;

    public MaidAIChatManager(EntityMaid maid) {
        super(maid);
        this.historySummaryManager = new HistorySummaryManager(this);
    }

    public void chat(String message, ChatClientInfo clientInfo, ServerPlayer sender) {
        if (!ServerRuleConfig.get(AIConfig.LLM_ENABLED)) {
            sender.sendSystemMessage(Component.translatable("ai.touhou_little_maid.chat.disable")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        ChatTokensAttachment chatTokens = sender.getAttachedOrCreate(InitDataAttachment.CHAT_TOKENS);
        if (chatTokens.get() >= ServerRuleConfig.get(AIConfig.MAX_TOKENS_PER_PLAYER)) {
            sender.sendSystemMessage(Component.translatable("message.touhou_little_maid.ai_chat.max_tokens_limit")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        @Nullable LLMSite site = this.getLLMSite();
        if (site == null || !site.enabled()) {
            sender.sendSystemMessage(Component.translatable("ai.touhou_little_maid.chat.llm.empty")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // 如果检测到是默认站点，那么大概率是新手玩家，给他提示去 DeepSeek 平台
        if (this.isDeepSeekSecretKeyMissing(site)) {
            this.sendDeepSeekTip(sender);
            return;
        }

        if (this.historySummaryManager.tryCompressBeforeChat(() -> this.decideThenChat(message, clientInfo, site))) {
            return;
        }

        this.decideThenChat(message, clientInfo, site);
    }

    /**
     * <b>先做，后说。</b>
     *
     * <p>原先的顺序是「回话 → 判定 → 执行」，于是女仆有机会承诺一件还没发生的事：
     * 实测她回过「好的主人，酒狐跟着你走啦~」而工具一次都没调。<b>说了没做，比说做不到糟得多</b>——
     * 它一次就摧毁玩家的信任，而后者只是让人失望。</p>
     *
     * <p>现在的顺序：</p>
     * <pre>
     * 玩家说话 → 判定（无历史、约一百多 token）
     *    ├─ 不是指令 → 直接说话（闲聊零额外延迟，与从前完全一样）
     *    └─ 是指令   → 先执行 → 结果写进历史 → 再说话（她讲的是已经发生的事）
     * </pre>
     *
     * <p>历史为空时跳过判定：实测弱档位模型在空历史下本来就调得动工具，多问一次纯属浪费。</p>
     *
     * <p>等待气泡在这里就建好并一路传下去——判定与执行对玩家不可见，
     * 若等到说话那一步才建，玩家会先看到两三秒空白，以为没反应。</p>
     */
    private void decideThenChat(String message, ChatClientInfo clientInfo, LLMSite site) {
        long bubbleId = this.maid.getChatBubbleManager()
                .addThinkingText("ai.touhou_little_maid.chat.chat_bubble_waiting");
        // 布尔参数表示「动作是否已由旁路做完」——做完了主对话就只说不动
        Runnable narrate = () -> this.tryToChat(message, clientInfo, site, bubbleId, false);
        Runnable narrateAfterAction = () -> this.tryToChat(message, clientInfo, site, bubbleId, true);

        if (this.getHistory().size() == 0) {
            narrate.run();
            return;
        }
        this.requestToolDispatch(message, narrate, narrateAfterAction);
    }

    private void sendDeepSeekTip(Player player) {
        MutableComponent tip = Component.translatable("ai.touhou_little_maid.chat.llm.deepseek_secret_key_missing")
                .withStyle(ChatFormatting.RED);
        MutableComponent url = Component.literal(DEEPSEEK_PLATFORM_URL);
        ClickEvent clickEvent = new ClickEvent.OpenUrl(URI.create(DEEPSEEK_PLATFORM_URL));
        HoverEvent hoverEvent = new HoverEvent.ShowText(Component.translatable("chat.link.open"));
        url.withStyle(style -> style.withHoverEvent(hoverEvent).withClickEvent(clickEvent)
                .withUnderlined(true).withColor(ChatFormatting.BLUE));
        player.sendSystemMessage(tip);
        player.sendSystemMessage(Component.translatable("ai.touhou_little_maid.chat.download_url").append(url));
    }

    private boolean isDeepSeekSecretKeyMissing(LLMSite site) {
        return site.id().equals(DefaultLLMSite.DEEPSEEK.id())
                && site instanceof LLMOpenAISite openAISite
                && StringUtils.isBlank(openAISite.secretKey());
    }

    private void tryToChat(String message, ChatClientInfo clientInfo, @NotNull LLMSite site,
                           long bubbleId, boolean actionAlreadyDone) {
        this.chatLanguage = clientInfo.language();
        LLMClient chatClient = site.client();
        List<LLMMessage> messages = this.getMessages(this, clientInfo.language());
        if (messages.isEmpty()) {
            this.maid.getChatBubbleManager().removeChatBubble(bubbleId);
            this.onSettingIsEmpty(clientInfo, chatClient);
        } else {
            HistoryMessagesCheck.checkMessages(messages);
            this.normalChat(message, messages, chatClient, bubbleId, actionAlreadyDone);
        }
    }

    private void normalChat(String message, List<LLMMessage> messages, LLMClient chatClient,
                            long bubbleId, boolean actionAlreadyDone) {
        // 先插入临时的 context
        String messageWithContext = UserPromptContexts.addContext(this.maid, message);

        // http 通信添加 context
        messages.add(LLMMessage.userChat(this.maid, messageWithContext));
        // 历史记录不添加
        this.maid.getAiChatManager().addUserHistory(message);

        // 通信
        // subagents=true + withExistingBubble：气泡在 decideThenChat 就建好了，这里接管而不是再建一个
        LLMCallback callback = new LLMCallback(this, messages, true)
                .withExistingBubble(bubbleId)
                .withActionAlreadyDone(actionAlreadyDone)
                .withRawUserMessage(message);
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
     * 第一轮没调工具时，用一条**不带历史**的微型请求判定这句是不是动作指令。
     *
     * <p>成因与逐轮实测见 {@link StringConstant#TOOL_DISPATCH_DECISION}。
     * 只在历史非空时才走：空历史下弱档位模型本来就调得动，那时多问一次纯属浪费。</p>
     */
    public void requestToolDispatch(String rawMessage, Runnable narrate, Runnable narrateAfterAction) {
        @Nullable LLMSite llmSite = this.getLLMSite();
        if (llmSite == null || !llmSite.enabled()) {
            narrate.run();
            return;
        }
        // 带上状态快照：判定只看玩家那句话时，「坐下」在她已经坐着的情况下照样被判成动作，
        // 于是白白多走一次执行去做无意义的空操作（实测多花 3 次请求约 3 秒）。
        // <context> 是一条 **user 消息**而不是 assistant 回合，因此不会重新引入那个抑制——
        // 抑制来自助手回合的示范，不是来自上下文的存在。
        String messageWithContext = UserPromptContexts.addContext(this.maid, rawMessage);
        List<LLMMessage> messages = Lists.newArrayList(
                LLMMessage.systemChat(this.maid, StringConstant.TOOL_DISPATCH_DECISION),
                LLMMessage.userChat(this.maid, messageWithContext));
        llmSite.client().chat(new ToolDispatchCallback(this, messages, rawMessage, narrate, narrateAfterAction));
    }

    /**
     * 判定为动作之后，用一条**不带历史**的请求把工具真正调出来。
     *
     * <p>消息只有两条：执行指令与玩家原话。<b>不得加历史或人设</b>——空历史正是弱档位模型
     * 仍会调用工具的那个条件，加回去这个修复就当场失效。契约测试钉着这一点。</p>
     */
    public void requestToolExecution(String rawMessage, Runnable narrate) {
        @Nullable LLMSite llmSite = this.getLLMSite();
        if (llmSite == null || !llmSite.enabled()) {
            narrate.run();
            return;
        }
        List<LLMMessage> messages = Lists.newArrayList(
                LLMMessage.systemChat(this.maid, StringConstant.TOOL_DISPATCH_EXECUTION),
                LLMMessage.userChat(this.maid, rawMessage));
        llmSite.client().chat(new ToolExecutionCallback(this, messages, narrate));
    }

    /**
     * 发起一次**不带历史**的翻译请求，把已写好的回复翻成合成语言。
     *
     * <p>这是「第二段」的新来源。原先它由主对话在同一条回复里用 {@code ---} 分出，而那在多轮下
     * 确定性失效——逐轮实测见 {@link StringConstant#TTS_TRANSLATION}。</p>
     *
     * <p>拿不到可用的 LLM 站点时降级成用对话文本合成，但**必须留声**：这条路走过一次，
     * 玩家听到的就是错误的语言，静默的话没人能从日志里看出发生过什么。</p>
     */
    public void requestTtsTranslation(TTSSite ttsSite, String chatText, long waitingChatBubbleId) {
        @Nullable LLMSite llmSite = this.getLLMSite();
        if (llmSite == null || !llmSite.enabled()) {
            TouhouLittleMaid.LOGGER.warn(
                    "No usable LLM site to translate the TTS text for maid {}. Falling back to synthesizing "
                            + "the chat text, so TTS will speak {} instead of {}.",
                    this.maid.getId(), this.getChatLanguage(), this.getTTSLanguage());
            this.tts(ttsSite, chatText, chatText, waitingChatBubbleId);
            return;
        }
        // 只有两条消息：翻译指令与待翻译文本。没有历史、没有人设、没有工具——没有可照抄的范例
        List<LLMMessage> messages = Lists.newArrayList(
                LLMMessage.systemChat(this.maid, PapiReplacer.ttsTranslationPrompt(this.maid)),
                LLMMessage.userChat(this.maid, chatText));
        TtsTranslationCallback callback = new TtsTranslationCallback(this, messages, ttsSite, chatText,
                waitingChatBubbleId);
        llmSite.client().chat(callback);
    }

    /**
     * 本次回复是否需要一段独立的待合成文本。
     *
     * <p>只有「TTS 确实会被调用」且「合成语言与聊天语言不同」时才需要。同语言时译文与原文相同，
     * TTS 关闭或站点不可用时译文生成完就被丢弃——两种情况下发那次翻译请求都是纯浪费。</p>
     *
     * <p><b>语义已变</b>：它以前决定「要不要在主对话里索取第二段」，现在决定
     * 「要不要发一次独立的翻译请求」（{@link #requestTtsTranslation}）。主对话的格式要求
     * 已恒定为单段，理由见 {@code PapiReplacer#outputFormat}。</p>
     *
     * <p>这个判据必须与 {@link LLMCallback#onSuccess} 里那个保持同一个，
     * 一旦分叉就会出现「翻了却不用」或「该翻却没翻」。</p>
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
        // 与 TTSCallback.onSuccess 同一族的静默丢弃：系统语音也只发给主人，主人解析不到就什么都不发。
        // 只修云端那一半会让两条路的可诊断性不对称，而玩家侧它们的表现是同一个「没声音」。
        if (!(maid.level instanceof ServerLevel serverLevel)) {
            TouhouLittleMaid.LOGGER.warn("Dropped system TTS request for maid {}: she is no longer on a server level",
                    maid.getId());
            return;
        }
        MinecraftServer server = serverLevel.getServer();
        server.submit(() -> {
            if (maid.getOwner() instanceof ServerPlayer player) {
                TTSSystemAudioToClientPackage message = new TTSSystemAudioToClientPackage(name, ttsText, config, services);
                ServerPlayNetworking.send(player, message);
            } else {
                TouhouLittleMaid.LOGGER.warn(
                        "Dropped system TTS request for maid {}: her owner is not reachable in {}"
                                + " (speech is only ever sent to the owner)",
                        maid.getId(), serverLevel.dimension().identifier());
            }
            maid.getChatBubbleManager().addLLMChatText(chatText, waitingChatBubbleId);
        });
    }

    public HistorySummaryManager getHistorySummaryManager() {
        return historySummaryManager;
    }
}
