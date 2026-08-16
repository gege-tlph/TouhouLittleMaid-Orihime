package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings;

import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor.TTSSiteEditorScreen;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.system.TTSSystemSite;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.ai.TTSSiteButton;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.FlatColorButton;
import com.github.tartaricacid.touhoulittlemaid.client.sound.VoicePreviewClient;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache;
import com.github.tartaricacid.touhoulittlemaid.util.Rectangle;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.ScreenUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import static com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.Translations.SAVE_NAME;

import java.util.ArrayList;
import java.util.List;

/**
 * TTS 站点列表标签页，支持编辑站点配置
 */
public class AIChatSettingsTTSSiteScreen extends AIChatSettingsHubScreen {
    private static final int ROW_HEIGHT = 26;
    /** 规则带占用的高度：三行（开关 / 代理 / 默认音色+试听）+ 底部间距 */
    private static final int RULE_STRIP_HEIGHT = 76;

    private EditBox proxyInput;
    private FlatColorButton previewButton;
    private FlatColorButton saveButton;
    private String previewSite = "";
    private String previewModel = "";

    public AIChatSettingsTTSSiteScreen(@Nullable Screen parent, SharedState state, boolean insufficientPermissions) {
        super(parent, state, insufficientPermissions);
        this.listScrollOffset = state.ttsListScrollOffset;
    }

    @Override
    protected Type getType() {
        return Type.TTS_SITE;
    }

    @Override
    protected void initContent() {
        int contentX = this.getContentX();
        int contentWidth = this.getContentWidth();
        // 无权限时整个内容区只有一行红字提示，规则带与列表都不摆
        if (this.insufficientPermissions) {
            this.listArea = new Rectangle(contentX, this.getContentY(), contentWidth, 0);
            return;
        }
        int listTop = this.getContentY() + RULE_STRIP_HEIGHT;
        this.addWorldRules(contentX, this.getContentY() + 2, contentWidth);
        this.listArea = new Rectangle(contentX, listTop, contentWidth, this.getFooterTop() - listTop - 8);

        List<TTSSite> sites = new ArrayList<>(this.state.ttsSites.values());
        int visibleCount = this.getVisibleListCount(ROW_HEIGHT);
        int maxOffset = Math.max(0, sites.size() - visibleCount);
        if (this.listScrollOffset > maxOffset) {
            this.listScrollOffset = maxOffset;
        }

        int endIndex = Math.min(sites.size(), this.listScrollOffset + visibleCount);
        for (int i = this.listScrollOffset; i < endIndex; i++) {
            int rowY = (int) this.listArea.y + (i - this.listScrollOffset) * ROW_HEIGHT;
            this.addRenderableWidget(new TTSSiteButton(sites.get(i), this, contentX, rowY, contentWidth));
        }
    }

    @Override
    protected void persistTransientState() {
        this.state.ttsListScrollOffset = this.listScrollOffset;
        // 切页前把正在编辑的文本冲进共享暂存，跨页保存才带得上它
        this.stageStringIfChanged(AIConfig.TTS_PROXY_ADDRESS, this.proxyInput);
    }

    /**
     * 站点是逐个保存的（各走 {@code SaveTTSSitePacket}），AI 规则不是——这个按钮统一提交
     * 规则带（开关 / 代理 / 默认音色）。未保存时按钮带「*」，在 extractRenderState 里每帧刷。
     */
    @Override
    protected void addFooterButtons() {
        super.addFooterButtons();
        this.saveButton = this.addRenderableWidget(new FlatColorButton(
                this.getContentX(), this.getFooterY(), 80, 20, SAVE_NAME, b -> this.saveWorldRules()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xc0101010, 0xc0101010);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.renderListScrollbar(graphics, this.state.ttsSites.size(), this.getVisibleListCount(ROW_HEIGHT));
        this.renderInsufficientPermissions(graphics);

        // 试听四态里的两个动态态在这里画：请求中的按钮字样每帧刷新，失败的行内红字限时显示
        if (this.previewButton != null) {
            this.previewButton.setMessage(VoicePreviewClient.buttonLabel(this.previewSite, this.previewModel));
            // 一次只允许一个在途请求：忙的时候按钮要看起来点不动，而不是默默吞掉点击
            this.previewButton.active = VoicePreviewClient.isPendingFor(this.previewSite, this.previewModel)
                    || !VoicePreviewClient.isBusy();
        }
        // 试听失败红字占页脚上方一行；保存绿字在页脚按钮行内，两者互不相撞
        Component previewError = VoicePreviewClient.inlineError();
        if (previewError != null) {
            graphics.textWithWordWrap(this.font, previewError, this.getContentX(),
                    this.getFooterTop() - 12, this.getContentWidth(), 0xFFFF5555);
        }
        this.renderSavedFlash(graphics);
        if (this.saveButton != null) {
            this.saveButton.setMessage(this.hasUnsavedRules() ? SAVE_NAME.copy().append("*") : SAVE_NAME);
        }
        // 弹出层必须画在最后：它要压在滚动条、提示字这些子类自绘物之上
        this.renderHubOverlays(graphics, mouseX, mouseY);
    }

    public void openTTSSiteEditor(String siteId) {
        TTSSite site = this.state.ttsSites.get(siteId);
        if (site == null) {
            return;
        }
        ScreenUtil.setScreen(new TTSSiteEditorScreen(this, site));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalDelta, double verticalDelta) {
        if (this.handleListScroll(mouseX, mouseY, verticalDelta, this.state.ttsSites.size(), this.getVisibleListCount(ROW_HEIGHT))) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalDelta, verticalDelta);
    }

    /**
     * 规则带：这一类服务的总开关与网络设置，与下面的站点列表是<b>两条保存路径</b>。
     * 带内所有控件只改 {@link #ruleSession()} 的暂存值，点「保存」才提交。
     * 试听按钮吃的是**暂存**的默认音色——先听后存，这正是要的顺序。
     */
    private void addWorldRules(int x, int ruleY, int contentWidth) {
        ServerRulesClientCache.Session session = this.ruleSession();
        this.addRuleToggle(x, ruleY, contentWidth, "config.touhou_little_maid.global_ai.tts_enable", AIConfig.TTS_ENABLED);
        this.proxyInput = this.addRuleInput(ruleY + ROW_PITCH,
                Component.translatable("ai.touhou_little_maid.chat.settings.hub.row.proxy"),
                Component.translatable("config.touhou_little_maid.global_ai.tts_proxy_address.tooltip"),
                this.proxyInput, session.getString(AIConfig.TTS_PROXY_ADDRESS));

        // 世界默认音色。右侧留 70px 给「试听」按钮——选音色靠耳朵，不靠名字。
        // 这条带里没有语种：语种是纯女仆属性，T 屏语种按钮是唯一编辑点（世界默认语种已删）
        List<SitePair> pairs = sitePairs(this.state.ttsSites);
        String currentSite = session.getString(AIConfig.DEFAULT_TTS_SITE);
        String currentModel = session.getString(AIConfig.DEFAULT_TTS_MODEL);
        String builtinId = TTSSystemSite.API_TYPE;
        int pickerY = ruleY + ROW_PITCH * 2;
        this.addRowLabel(pickerY, Component.translatable("ai.touhou_little_maid.chat.settings.hub.default_voice"),
                Component.translatable("ai.touhou_little_maid.chat.settings.hub.default_voice.tooltip"));
        MutableComponent label = Component.literal(pairLabel(pairs, currentSite, currentModel, builtinId));
        this.addRenderableWidget(new FlatColorButton(this.rowControlX(), pickerY, this.rowControlWidth() - 70, 20, label,
                b -> this.openDefaultPicker(session, pairs, currentSite, currentModel, builtinId, pickerY)));
        this.addPreviewButton(x + contentWidth - 66, pickerY, currentSite, currentModel);
    }

    /** 默认音色弹出列表：首项「内置兜底（系统）」显式可选——与具体选「系统」的差别是前者跟着内置走 */
    private void openDefaultPicker(ServerRulesClientCache.Session session, List<SitePair> pairs,
                                   String currentSite, String currentModel, String builtinId, int pickerY) {
        List<HubPopupEntry> entries = new ArrayList<>();
        entries.add(new HubPopupEntry(
                Component.translatable("ai.touhou_little_maid.chat.settings.hub.builtin_default", siteDisplayName(builtinId)),
                StringUtils.isBlank(currentSite), () -> {
            session.set(AIConfig.DEFAULT_TTS_SITE, "");
            session.set(AIConfig.DEFAULT_TTS_MODEL, "");
            this.init();
        }));
        for (SitePair pair : pairs) {
            entries.add(new HubPopupEntry(Component.literal(pair.label()),
                    pair.siteId().equals(currentSite) && pair.modelId().equals(currentModel), () -> {
                session.set(AIConfig.DEFAULT_TTS_SITE, pair.siteId());
                session.set(AIConfig.DEFAULT_TTS_MODEL, pair.modelId());
                this.init();
            }));
        }
        this.openHubPopup(this.rowControlX(), pickerY, this.rowControlWidth() - 70, entries);
    }

    /**
     * 试听当前默认音色。选音色靠耳朵，不靠名字——这颗按钮就是那只耳朵。
     * 请求中/成功/失败/不可用四态由 {@link VoicePreviewClient} 统一管理，
     * 失败在本屏走行内红字（extractRenderState 里画），聊天栏在 GUI 后面看不见。
     */
    private void addPreviewButton(int x, int y, String siteId, String modelId) {
        String effectiveSite = StringUtils.isBlank(siteId) ? TTSSystemSite.API_TYPE : siteId;
        this.previewButton = this.addRenderableWidget(new FlatColorButton(x, y, 66, 20,
                VoicePreviewClient.buttonLabel(effectiveSite, modelId),
                b -> VoicePreviewClient.request(effectiveSite, modelId, VoicePreviewClient.Origin.SETTINGS)));
        this.previewSite = effectiveSite;
        this.previewModel = modelId;
    }

    private void saveWorldRules() {
        this.stageStringIfChanged(AIConfig.TTS_PROXY_ADDRESS, this.proxyInput);
        this.ruleSession().save();
        this.flashSaved();
        this.init();
    }

    /** 保存按钮的未保存标记：暂存有改动，或输入框内容与暂存不一致 */
    private boolean hasUnsavedRules() {
        ServerRulesClientCache.Session session = this.ruleSession();
        if (session.isDirty()) {
            return true;
        }
        return this.proxyInput != null
                && !this.proxyInput.getValue().trim().equals(session.getString(AIConfig.TTS_PROXY_ADDRESS));
    }
}
