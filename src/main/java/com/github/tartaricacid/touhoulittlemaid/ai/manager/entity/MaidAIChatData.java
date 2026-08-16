package com.github.tartaricacid.touhoulittlemaid.ai.manager.entity;

import com.github.tartaricacid.touhoulittlemaid.TouhouLittleMaid;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.CharacterSetting;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.setting.SettingReader;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.SupportModelSelect;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.DefaultLLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMMessage;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.response.ToolCall;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.system.TTSSystemSite;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.util.CappedQueue;
import com.google.common.collect.Lists;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

@SuppressWarnings("all")
public abstract class MaidAIChatData extends MaidAIChatSerializable {
    protected static final String MAID_HISTORY_CHAT_TAG = "MaidHistoryChat";
    protected static final String MAID_HISTORY_SUMMARY_TAG = "MaidHistorySummary";
    protected static final String MAID_LAST_CHAT_TOKEN_USAGE_TAG = "MaidLastChatTokenUsage";

    protected final EntityMaid maid;
    protected final CappedQueue<LLMMessage> history;

    protected String compressedSummary = StringUtils.EMPTY;
    protected volatile int lastChatTokenUsage = 0;
    public volatile boolean historySummaryRunning = false;

    public MaidAIChatData(EntityMaid maid) {
        this.maid = maid;
        this.history = new CappedQueue<>(512);
    }

    @Override
    public CompoundTag readFromTag(CompoundTag tag) {
        if (tag.contains(MAID_HISTORY_CHAT_TAG)) {
            try {
                this.history.getDeque().clear();
                Tag historyTag = tag.get(MAID_HISTORY_CHAT_TAG);
                if (historyTag != null) {
                    LLMMessage.CODEC.listOf().parse(NbtOps.INSTANCE, historyTag)
                            .resultOrPartial(TouhouLittleMaid.LOGGER::error)
                            .ifPresent(list -> {
                                ListIterator<LLMMessage> iterator = list.listIterator(list.size());
                                while (iterator.hasPrevious()) {
                                    history.add(iterator.previous());
                                }
                            });
                }
            } catch (Exception e) {
                TouhouLittleMaid.LOGGER.error("Failed to parse MaidHistoryChat", e);
            }
        }
        this.compressedSummary = tag.getString(MAID_HISTORY_SUMMARY_TAG).orElse("");
        this.lastChatTokenUsage = tag.getInt(MAID_LAST_CHAT_TOKEN_USAGE_TAG).orElse(0);
        return super.readFromTag(tag);
    }

    @Override
    public CompoundTag writeToTag(CompoundTag tag) {
        if (this.history.size() > 0) {
            try {
                ArrayList<LLMMessage> llmMessages = Lists.newArrayList(this.history.getDeque());
                LLMMessage.CODEC.listOf().encodeStart(NbtOps.INSTANCE, llmMessages)
                        .resultOrPartial(TouhouLittleMaid.LOGGER::error)
                        .ifPresent(t -> tag.put(MAID_HISTORY_CHAT_TAG, t));
            } catch (Exception e) {
                TouhouLittleMaid.LOGGER.error("Failed to parse MaidHistoryChat", e);
            }
        }
        if (StringUtils.isNotBlank(this.compressedSummary)) {
            tag.putString(MAID_HISTORY_SUMMARY_TAG, this.compressedSummary);
        }
        if (this.lastChatTokenUsage > 0) {
            tag.putInt(MAID_LAST_CHAT_TOKEN_USAGE_TAG, this.lastChatTokenUsage);
        }
        return super.writeToTag(tag);
    }

    // 1.21.11 entity-save 路径（ValueOutput/ValueInput）。与 writeToTag/readFromTag 写出完全相同的根级键
    // （历史 list 用同一 LLMMessage.CODEC.listOf() → ListTag、summary/tokenUsage 同守卫、"MaidAIChat" 子 compound），
    // 逐字节同 HEAD 存档格式。writeToTag/readFromTag(CompoundTag) 保留给网络同步（SyncMaidAIDataPacket）。
    // ⚠️ 两条路径字段必须保持一致 —— 新增字段时两处都要改。
    @Override
    public void save(ValueOutput output) {
        if (this.history.size() > 0) {
            output.store(MAID_HISTORY_CHAT_TAG, LLMMessage.CODEC.listOf(), Lists.newArrayList(this.history.getDeque()));
        }
        if (StringUtils.isNotBlank(this.compressedSummary)) {
            output.putString(MAID_HISTORY_SUMMARY_TAG, this.compressedSummary);
        }
        if (this.lastChatTokenUsage > 0) {
            output.putInt(MAID_LAST_CHAT_TOKEN_USAGE_TAG, this.lastChatTokenUsage);
        }
        super.save(output);
    }

    @Override
    public void read(ValueInput input) {
        input.list(MAID_HISTORY_CHAT_TAG, LLMMessage.CODEC).ifPresent(typedList -> {
            try {
                this.history.getDeque().clear();
                List<LLMMessage> list = new ArrayList<>();
                typedList.forEach(list::add);
                ListIterator<LLMMessage> iterator = list.listIterator(list.size());
                while (iterator.hasPrevious()) {
                    history.add(iterator.previous());
                }
            } catch (Exception e) {
                TouhouLittleMaid.LOGGER.error("Failed to parse MaidHistoryChat", e);
            }
        });
        this.compressedSummary = input.getStringOr(MAID_HISTORY_SUMMARY_TAG, "");
        this.lastChatTokenUsage = input.getIntOr(MAID_LAST_CHAT_TOKEN_USAGE_TAG, 0);
        super.read(input);
    }

    @Nullable
    public LLMSite getLLMSite() {
        return resolveLLMSite(llmSite);
    }

    /**
     * 显式的两层继承链：覆盖 → 世界默认 → 内置兜底。
     *
     * <p>空的 {@code overrideSite} 表示「跟随默认」，这是一个<b>合法状态</b>而不是缺数据——
     * 管理员换默认时，跟随的女仆自动跟着变；写了具体值（哪怕和默认相同）则钉住不跟。
     * 旧档里的具体值天然成为覆盖，旧档里的空天然成为跟随，无需迁移。</p>
     *
     * <p>静态化是为了让 JUnit 不用构造 EntityMaid 就能测整条链。</p>
     */
    @Nullable
    public static LLMSite resolveLLMSite(String overrideSite) {
        if (StringUtils.isNotBlank(overrideSite)) {
            LLMSite site = AvailableSites.getLLMSite(overrideSite);
            if (site != null && site.enabled()) {
                return site;
            }
            // 覆盖失效（站点被删/被禁）→ 回落到默认，而不是直接跳内置兜底
        }
        String defaultSite = ServerRuleConfig.get(AIConfig.DEFAULT_LLM_SITE);
        if (StringUtils.isNotBlank(defaultSite)) {
            LLMSite site = AvailableSites.getLLMSite(defaultSite);
            if (site != null && site.enabled()) {
                return site;
            }
        }
        return builtinFallbackLLMSite();
    }

    private static LLMSite builtinFallbackLLMSite() {
        LLMSite site = AvailableSites.getLLMSite(DefaultLLMSite.DEEPSEEK.id());
        return site == null ? DefaultLLMSite.DEEPSEEK : site;
    }

    @Nullable
    public TTSSite getTTSSite() {
        if (isNoTTSSite(ttsSite)) {
            return null;
        }
        return resolveTTSSite(ttsSite);
    }

    /**
     * 与 {@link #resolveLLMSite} 同构。{@code __none__}（不说话）在调用方处理，
     * 本方法只管「跟随/覆盖/回落」三态——不说话是覆盖的一种，默认怎么变都不影响它。
     */
    @Nullable
    public static TTSSite resolveTTSSite(String overrideSite) {
        if (StringUtils.isNotBlank(overrideSite)) {
            TTSSite site = AvailableSites.getTTSSite(overrideSite);
            if (site != null && site.enabled()) {
                return site;
            }
        }
        String defaultSite = ServerRuleConfig.get(AIConfig.DEFAULT_TTS_SITE);
        if (StringUtils.isNotBlank(defaultSite)) {
            TTSSite site = AvailableSites.getTTSSite(defaultSite);
            if (site != null && site.enabled()) {
                return site;
            }
        }
        return AvailableSites.getTTSSite(TTSSystemSite.API_TYPE);
    }

    public String getLLMModel() {
        return resolveLLMModel(llmSite, llmModel);
    }

    /**
     * 模型也走链：跟随（站点覆盖为空）且真的落在默认站点上时，用世界默认模型；
     * 其余情况沿用原逻辑（空 → 站点的第一个模型）。
     *
     * <p>「真的落在默认站点上」这个条件不能省：默认站点失效时链会滑到内置兜底，
     * 把默认模型套在另一个站点头上是错的。</p>
     */
    public static String resolveLLMModel(String overrideSite, String overrideModel) {
        LLMSite site = resolveLLMSite(overrideSite);
        if (!(site instanceof SupportModelSelect select)) {
            return StringUtils.EMPTY;
        }
        String chosen = overrideModel;
        if (StringUtils.isBlank(chosen) && StringUtils.isBlank(overrideSite)
                && site.id().equals(ServerRuleConfig.get(AIConfig.DEFAULT_LLM_SITE))) {
            chosen = ServerRuleConfig.get(AIConfig.DEFAULT_LLM_MODEL);
        }
        return StringUtils.isBlank(chosen) ? select.getDefaultModel() : select.getModel(chosen);
    }

    public String getTTSModel() {
        return resolveTTSModel(ttsSite, ttsModel);
    }

    public static String resolveTTSModel(String overrideSite, String overrideModel) {
        TTSSite site = resolveTTSSite(overrideSite);
        if (!(site instanceof SupportModelSelect select)) {
            return StringUtils.EMPTY;
        }
        String chosen = overrideModel;
        if (StringUtils.isBlank(chosen) && StringUtils.isBlank(overrideSite)
                && site.id().equals(ServerRuleConfig.get(AIConfig.DEFAULT_TTS_SITE))) {
            chosen = ServerRuleConfig.get(AIConfig.DEFAULT_TTS_MODEL);
        }
        return StringUtils.isBlank(chosen) ? select.getDefaultModel() : select.getModel(chosen);
    }

    public String getTTSLanguage() {
        if (StringUtils.isNotBlank(ttsLanguage)) {
            return ttsLanguage;
        }
        // 语种是纯女仆属性（T 屏语种按钮是唯一编辑点）。空值只出现在从未打开过聊天屏的女仆身上，
        // 兜底与 T 屏强填的取值一致——「世界默认语种」因与之重合且实际管不到人，已按用户定案删除
        return "en_us";
    }

    public String getChatLanguage() {
        if (StringUtils.isNotBlank(chatLanguage)) {
            return chatLanguage;
        }
        return "en_us";
    }

    public CappedQueue<LLMMessage> getHistory() {
        return history;
    }

    public String getCompressedSummary() {
        return compressedSummary;
    }

    public void setCompressedSummary(String newSummary) {
        this.compressedSummary = newSummary;
    }

    public boolean hasCompressedSummary() {
        return StringUtils.isNotBlank(compressedSummary);
    }

    public int getLastChatTokenUsage() {
        return lastChatTokenUsage;
    }

    public void setLastChatTokenUsage(int lastChatTokenUsage) {
        this.lastChatTokenUsage = Math.max(0, lastChatTokenUsage);
    }

    public void clearAllChatMemory() {
        this.history.getDeque().clear();
        this.compressedSummary = StringUtils.EMPTY;
        this.lastChatTokenUsage = 0;
        this.historySummaryRunning = false;
    }

    public void addUserHistory(String message) {
        this.history.add(LLMMessage.userChat(maid, message));
        this.onHistoryUpdated();
    }

    public void addAssistantHistory(String message) {
        this.history.add(LLMMessage.assistantChat(maid, message));
        this.onHistoryUpdated();
    }

    public void addAssistantHistory(String message, List<ToolCall> toolCalls) {
        this.history.add(LLMMessage.assistantChat(maid, message, toolCalls));
        this.onHistoryUpdated();
    }

    public void addToolHistory(String message, String toolCallId) {
        this.history.add(LLMMessage.toolChat(maid, message, toolCallId));
        this.onHistoryUpdated();
    }

    public EntityMaid getMaid() {
        return maid;
    }

    public Optional<CharacterSetting> getSetting() {
        String modelId = this.maid.getModelId();
        return SettingReader.getSetting(modelId);
    }

    protected void onHistoryUpdated() {
    }
}
