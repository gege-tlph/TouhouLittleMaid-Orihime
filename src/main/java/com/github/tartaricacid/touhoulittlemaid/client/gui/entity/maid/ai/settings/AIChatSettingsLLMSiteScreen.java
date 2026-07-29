package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings;

import com.github.tartaricacid.touhoulittlemaid.ai.service.SerializerRegister;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMApiType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAISite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor.LLMSiteEditorScreen;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.ai.LLMSiteButton;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.FlatColorButton;
import com.github.tartaricacid.touhoulittlemaid.util.Rectangle;
import com.google.common.collect.Lists;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * LLM 站点列表标签页，支持新建 / 编辑 / 删除站点
 */
public class AIChatSettingsLLMSiteScreen extends AIChatSettingsHubScreen {
    private static final int ROW_HEIGHT = 26;
    /** 世界规则带占用的高度：三行（开关对 / 代理 / 默认模型）+ 底部间距 */
    private static final int RULE_STRIP_HEIGHT = 76;

    private EditBox proxyInput;
    private FlatColorButton saveButton;


    public AIChatSettingsLLMSiteScreen(@Nullable Screen parent, SharedState state, boolean insufficientPermissions) {
        super(parent, state, insufficientPermissions);
        this.listScrollOffset = state.llmListScrollOffset;
    }

    public AIChatSettingsLLMSiteScreen(
            @Nullable Screen parent,
            Map<String, LLMSite> llmSites,
            Map<String, TTSSite> ttsSites,
            boolean insufficientPermissions
    ) {
        super(parent, llmSites, ttsSites, insufficientPermissions);
    }

    @Override
    protected Type getType() {
        return Type.LLM_SITE;
    }

    @Override
    protected void initContent() {
        int contentX = this.getContentX();
        int contentWidth = this.getContentWidth();
        int createButtonY = this.startY + BASE_HEIGHT - 48;

        // 无权限时整个内容区只有一行红字提示，规则带与列表都不摆
        if (this.insufficientPermissions) {
            this.listArea = new Rectangle(contentX, this.getContentY(), contentWidth, 0);
            return;
        }
        this.addWorldRules(contentX, this.getContentY() + 2, contentWidth);
        int listTop = this.getContentY() + RULE_STRIP_HEIGHT;
        this.listArea = new Rectangle(contentX, listTop, contentWidth, createButtonY - listTop - 4);

        List<LLMSite> sites = Lists.newArrayList(this.state.llmSites.values());
        int visibleCount = this.getVisibleListCount(ROW_HEIGHT);
        int maxOffset = Math.max(0, sites.size() - visibleCount);
        if (this.listScrollOffset > maxOffset) {
            this.listScrollOffset = maxOffset;
        }

        int endIndex = Math.min(sites.size(), this.listScrollOffset + visibleCount);
        for (int i = this.listScrollOffset; i < endIndex; i++) {
            int rowY = (int) this.listArea.y + (i - this.listScrollOffset) * ROW_HEIGHT;
            this.addRenderableWidget(new LLMSiteButton(sites.get(i), this, contentX, rowY, contentWidth));
        }

        this.addLLMCreateButtons(contentX, contentWidth, createButtonY);
    }

    private void addLLMCreateButtons(int btnX, int btnWidth, int btnY) {
        LLMApiType[] values = LLMApiType.values();
        int buttonWidth = (btnWidth - 4 * (values.length - 1)) / values.length;
        for (int i = 0; i < values.length; i++) {
            int x = btnX + i * (buttonWidth + 4);
            LLMApiType apiType = values[i];
            String siteName = I18n.get("ai.touhou_little_maid.chat.site.%s.name".formatted(apiType.getName()));
            MutableComponent text = Component.translatable("ai.touhou_little_maid.chat.settings.hub.create", siteName);
            this.addRenderableWidget(new FlatColorButton(x, btnY, buttonWidth, 20, text, b -> this.openNewLLMSiteEditor(apiType)));
        }
    }

    @Override
    protected void persistTransientState() {
        this.state.llmListScrollOffset = this.listScrollOffset;
        // 切页前把正在编辑的文本冲进共享暂存，跨页保存才带得上它
        this.stageStringIfChanged(AIConfig.LLM_PROXY_ADDRESS, this.proxyInput);
    }

    /**
     * 站点是逐个保存的，世界规则不是——这个按钮统一提交规则带（开关 / 代理 / 默认模型）。
     * 未保存时按钮带「*」，在 render 里每帧刷。
     */
    @Override
    protected void addFooterButtons() {
        super.addFooterButtons();
        this.saveButton = this.addRenderableWidget(new com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.FlatColorButton(
                this.getContentX(), this.getFooterY(), 80, 20,
                com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.Translations.SAVE_NAME,
                b -> this.saveWorldRules()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderListScrollbar(graphics, this.state.llmSites.size(), this.getVisibleListCount(ROW_HEIGHT));
        this.renderInsufficientPermissions(graphics);
        this.renderSavedFlash(graphics);
        if (this.saveButton != null) {
            this.saveButton.setMessage(this.hasUnsavedRules()
                    ? com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.Translations.SAVE_NAME.copy().append("*")
                    : com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.Translations.SAVE_NAME);
        }
        this.renderHubOverlays(graphics, mouseX, mouseY);
    }

    public void openLLMSiteEditor(String siteId) {
        LLMSite site = this.state.llmSites.get(siteId);
        if (!(site instanceof LLMOpenAISite) || this.minecraft == null) {
            return;
        }
        boolean supportsReasoning = "openai".equals(site.id());
        this.minecraft.setScreen(new LLMSiteEditorScreen(this, site, false, supportsReasoning));
    }

    public void openNewLLMSiteEditor(LLMApiType apiType) {
        LLMSite site = this.createDefaultLLMSite(apiType);
        if (!(site instanceof LLMOpenAISite) || this.minecraft == null) {
            return;
        }
        this.minecraft.setScreen(new LLMSiteEditorScreen(this, site, true));
    }

    public boolean hasLLMSite(String siteId) {
        return this.state.llmSites.containsKey(siteId);
    }

    @Nullable
    private LLMSite createDefaultLLMSite(LLMApiType apiType) {
        var serializer = SerializerRegister.getLLMSerializer(apiType.getName());
        if (serializer == null) {
            return null;
        }
        return serializer.defaultSite();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalDelta, double verticalDelta) {
        if (this.handleListScroll(mouseX, mouseY, verticalDelta, this.state.llmSites.size(), this.getVisibleListCount(ROW_HEIGHT))) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalDelta, verticalDelta);
    }
    /**
     * 世界规则带：这一类服务的总开关与网络设置，与下面的站点列表是两条保存路径。
     * 带内所有控件只改 {@link #ruleSession()} 的暂存值，点「保存」才提交——
     * 提交激活时 {@code DefaultAiSnapshot} 会把默认变更广播给在线玩家。
     */
    private void addWorldRules(int x, int ruleY, int contentWidth) {
        ServerRulesClientCache.Session session = this.ruleSession();
        this.addRuleToggle(x, ruleY, (contentWidth - 4) / 2, "config.touhou_little_maid.global_ai.llm_enable", AIConfig.LLM_ENABLED);
        this.addRuleToggle(x + (contentWidth - 4) / 2 + 4, ruleY, (contentWidth - 4) / 2, "config.touhou_little_maid.global_ai.auto_gen_setting_enabled", AIConfig.AUTO_GEN_SETTING_ENABLED);
        this.proxyInput = this.addRuleInput(ruleY + ROW_PITCH,
                Component.translatable("ai.touhou_little_maid.chat.settings.hub.row.proxy"),
                Component.translatable("config.touhou_little_maid.global_ai.llm_proxy_address.tooltip"),
                this.proxyInput, session.getString(AIConfig.LLM_PROXY_ADDRESS));

        // 世界默认模型：跟随默认的女仆全部解析到这里。弹出列表选择（循环按钮 v1 已废）
        var pairs = sitePairs(this.state.llmSites);
        String currentSite = session.getString(AIConfig.DEFAULT_LLM_SITE);
        String currentModel = session.getString(AIConfig.DEFAULT_LLM_MODEL);
        String builtinId = com.github.tartaricacid.touhoulittlemaid.ai.service.llm.DefaultLLMSite.DEEPSEEK.id();
        int pickerY = ruleY + ROW_PITCH * 2;
        this.addRowLabel(pickerY, Component.translatable("ai.touhou_little_maid.chat.settings.hub.default_model"),
                Component.translatable("ai.touhou_little_maid.chat.settings.hub.default_model.tooltip"));
        MutableComponent label = Component.literal(pairLabel(pairs, currentSite, currentModel, builtinId));
        this.addRenderableWidget(new com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.FlatColorButton(
                this.rowControlX(), pickerY, this.rowControlWidth(), 20, label,
                b -> this.openDefaultPicker(session, pairs, currentSite, currentModel, builtinId, pickerY)));
    }

    /** 默认模型弹出列表：首项「内置兜底」显式可选（此前一旦设过默认就再也回不到空值） */
    private void openDefaultPicker(ServerRulesClientCache.Session session, java.util.List<SitePair> pairs,
                                   String currentSite, String currentModel, String builtinId, int pickerY) {
        java.util.List<HubPopupEntry> entries = new java.util.ArrayList<>();
        entries.add(new HubPopupEntry(
                Component.translatable("ai.touhou_little_maid.chat.settings.hub.builtin_default", siteDisplayName(builtinId)),
                org.apache.commons.lang3.StringUtils.isBlank(currentSite), () -> {
            session.set(AIConfig.DEFAULT_LLM_SITE, "");
            session.set(AIConfig.DEFAULT_LLM_MODEL, "");
            this.init();
        }));
        for (SitePair pair : pairs) {
            entries.add(new HubPopupEntry(Component.literal(pair.label()),
                    pair.siteId().equals(currentSite) && pair.modelId().equals(currentModel), () -> {
                session.set(AIConfig.DEFAULT_LLM_SITE, pair.siteId());
                session.set(AIConfig.DEFAULT_LLM_MODEL, pair.modelId());
                this.init();
            }));
        }
        this.openHubPopup(this.rowControlX(), pickerY, this.rowControlWidth(), entries);
    }

    private void saveWorldRules() {
        this.stageStringIfChanged(AIConfig.LLM_PROXY_ADDRESS, this.proxyInput);
        this.ruleSession().save();
        this.flashSaved();
        this.init();
    }

    /** 保存按钮的未保存标记：暂存有改动，或输入框内容与暂存不一致 */
    private boolean hasUnsavedRules() {
        if (this.ruleSession().isDirty()) {
            return true;
        }
        return this.proxyInput != null
                && !this.proxyInput.getValue().trim().equals(this.ruleSession().getString(AIConfig.LLM_PROXY_ADDRESS));
    }

}
