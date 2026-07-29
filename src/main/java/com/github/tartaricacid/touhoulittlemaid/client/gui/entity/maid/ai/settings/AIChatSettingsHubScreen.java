package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTApiType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.AIChatScreen;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.ai.SideButton;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.ai.SideGroupWidget;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.FlatColorButton;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.OpenMaidAIChatPacket;
import com.github.tartaricacid.touhoulittlemaid.util.Rectangle;
import com.google.common.collect.Maps;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.neoforged.neoforge.common.ModConfigSpec;
import com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.Translations.*;
import static net.minecraft.network.chat.CommonComponents.GUI_BACK;

/**
 * 设置界面的骨架：左侧标签页导航 + 右侧内容区。
 * 各标签页（LLM / TTS / 来源与麦克风 / 用量）由子类实现 {@link #initContent()}。
 */
public abstract class AIChatSettingsHubScreen extends Screen {
    protected static final int BASE_WIDTH = 400;
    protected static final int BASE_HEIGHT = 230;
    protected static final int SIDE_WIDTH = 100;

    protected static final int CONTENT_X_OFFSET = SIDE_WIDTH + 5;
    protected static final int CONTENT_WIDTH = BASE_WIDTH - CONTENT_X_OFFSET;

    protected final @Nullable Screen parent;
    protected final boolean insufficientPermissions;
    /**
     * 标签页切换时保持各子页面的临时状态（滚动位置、输入值等）
     */
    protected final SharedState state;

    protected int startX;
    protected int startY;

    /**
     * 子类用于列表区域和滚动偏移的共享字段
     */
    protected Rectangle listArea;
    protected int listScrollOffset;

    protected AIChatSettingsHubScreen(@Nullable Screen parent, SharedState state, boolean insufficientPermissions) {
        super(Component.literal("AI Chat Settings Hub"));
        this.parent = parent;
        this.state = state;
        this.insufficientPermissions = insufficientPermissions;
    }

    protected AIChatSettingsHubScreen(@Nullable Screen parent, Map<String, LLMSite> llmSites,
                                      Map<String, TTSSite> ttsSites, boolean insufficientPermissions) {
        this(parent, SharedState.create(llmSites, ttsSites), insufficientPermissions);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        // 重建控件时弹出列表里记的坐标与回调都过期了，一律关掉
        this.closeHubPopup();
        this.startX = (this.width - BASE_WIDTH) / 2;
        this.startY = (this.height - BASE_HEIGHT) / 2;

        // 侧栏顺序 = 权限边界：玩家的那一组永远在最上面，管理员的两组只在有权限时出现。
        // 原先四个标签按服务排（LLM/TTS/STT），权限边界横切在中间且界面上完全看不见——
        // 没权限的玩家照样看得见前两个、点得进去，进去才撞上一行红字。
        int sideY = this.addVoiceInputSideButtons(this.startY + 5);
        if (!this.state.sttOnly && !this.insufficientPermissions) {
            sideY = this.addServiceSideButtons(sideY);
            this.addUsageSideButtons(sideY);
        }

        this.initContent();
        this.addFooterButtons();

        this.children().stream()
                .filter(w -> w instanceof SideButton)
                .forEach(w -> ((SideButton) w).updateSelect(this.getType()));
    }

    protected abstract Type getType();

    protected abstract void initContent();

    protected void persistTransientState() {
    }

    protected int getContentX() {
        return this.startX + CONTENT_X_OFFSET;
    }

    protected int getContentY() {
        return this.startY + 5;
    }

    protected int getContentWidth() {
        return CONTENT_WIDTH;
    }

    protected int getFooterY() {
        return this.startY + BASE_HEIGHT - 24;
    }

    protected int getFooterTop() {
        return this.startY + BASE_HEIGHT - 28;
    }

    /**
     * 行式布局的三个常量与三个助手：**所有设置屏的表单行都走这一套**，别再各排各的。
     *
     * <p>行距 {@link #ROW_PITCH}，开关自带文案占满整行；文本框 / 选择器行则是
     * 「标签左置（{@link #LABEL_WIDTH}）+ 控件占余宽」。曾经的排法是标签悬在控件上方 12px、
     * 行距却只给 22px——标签全部压在上一行控件身上，实机一屏糊成一团。</p>
     */
    protected static final int LABEL_WIDTH = 90;
    protected static final int ROW_PITCH = 24;

    protected int rowControlX() {
        return this.getContentX() + LABEL_WIDTH + 4;
    }

    protected int rowControlWidth() {
        return this.getContentWidth() - LABEL_WIDTH - 4;
    }

    /** 行首标签：与 20px 高的控件垂直居中，亮灰——旧的 0x777777 在暗底上根本读不清 */
    protected void addRowLabel(int y, Component text) {
        this.addRowLabel(y, text, null);
    }

    /** 带悬停说明的行首标签：紧凑标签装不下的解释性长文案（代理格式、继承语义）挂在这里 */
    protected void addRowLabel(int y, Component text, @Nullable Component tooltip) {
        int x = this.getContentX() + 2;
        this.addRenderableOnly((net.minecraft.client.gui.components.Renderable)
                (graphics, mouseX, mouseY, partialTick) -> {
                    graphics.drawString(this.font, text, x, y + 6, 0xFFC6C6C6, false);
                    if (tooltip != null && mouseX >= x && mouseX < x + LABEL_WIDTH
                            && mouseY >= y && mouseY < y + 20) {
                        graphics.setTooltipForNextFrame(this.font, java.util.List.of(tooltip),
                                java.util.Optional.empty(), mouseX, mouseY);
                    }
                });
    }

    /** 通用开关行：文案自己带「开/关」，点一下就翻转（本机配置版，翻转动作由调用方给） */
    protected FlatColorButton addToggleRow(int x, int y, int width, Component label,
                                           boolean current, Runnable onFlip) {
        MutableComponent text = label.copy()
                .append(": ")
                .append(Component.translatable(current
                        ? "ai.touhou_little_maid.chat.settings.hub.enabled"
                        : "ai.touhou_little_maid.chat.settings.hub.disabled"));
        return this.addRenderableWidget(new FlatColorButton(x, y, width, 20, text, b -> onFlip.run()));
    }

    /**
     * 规则暂存：所有规则控件读写**同一个** Session，「保存」才一次性提交。
     *
     * <p>曾经每个控件各自 createSession + 立即 save：值确实写去了服务端，但界面 init()
     * 重读的是还没等到回包的旧缓存——于是「点两遍才切换」，而且改动绕过保存按钮直接生效。
     * 长命 Session 的 pending 就是本地回显，单击立即可见；save() 之前服务端什么都不知道。</p>
     *
     * <p>暂存挂在 {@link SharedState} 上**跨标签页共享**：带着未保存改动切页不再静默丢弃，
     * 在任何一页点「保存」都会把攒着的全部改动一起提交（save 发的是整个 changed 集）。
     * 离开整个设置界面才作废——「返回=放弃未保存」这半句语义保留。</p>
     */
    protected ServerRulesClientCache.Session ruleSession() {
        if (this.state.ruleSession == null) {
            this.state.ruleSession = ServerRulesClientCache.createSession();
        }
        return this.state.ruleSession;
    }

    /** 切页前把输入框内容冲进暂存（只在真的改了时记 dirty），否则跨页保存带不上正在编辑的文本 */
    protected void stageStringIfChanged(ModConfigSpec.ConfigValue<String> rule, @Nullable EditBox box) {
        if (box == null) {
            return;
        }
        String value = box.getValue().trim();
        if (!value.equals(this.ruleSession().getString(rule))) {
            this.ruleSession().set(rule, value);
        }
    }

    /**
     * 世界规则的一条开关：点一下翻转**暂存值**，点「保存」才提交。
     *
     * <p>站点是逐个保存的（各自走 {@code Save*SitePacket}），世界规则不是——它们攒在
     * {@link ServerRulesClientCache.Session} 里一起提交，所以这两类东西虽然摆在同一屏，
     * <b>保存路径是两条</b>，生效时机在专服上也不同。屏上必须把后者说清楚，
     * 否则管理员会以为改完开关就生效了。</p>
     */
    protected FlatColorButton addRuleToggle(int x, int y, int width, String labelKey,
                                            ModConfigSpec.ConfigValue<Boolean> rule) {
        boolean current = this.ruleSession().getBoolean(rule);
        FlatColorButton button = this.addToggleRow(x, y, width, Component.translatable(labelKey), current, () -> {
            this.ruleSession().set(rule, !current);
            this.init();
        });
        String tooltipKey = labelKey + ".tooltip";
        if (net.minecraft.client.resources.language.I18n.exists(tooltipKey)) {
            button.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.translatable(tooltipKey)));
        }
        return button;
    }


    /**
     * 世界默认（站点, 模型）对选择器：v1 用循环按钮。
     *
     * <p>与被否掉的语音来源循环按钮不同：当前值直接写在按钮上、候选全部来自已启用站点、
     * 没有逐项配置状态要展示——循环在这里丢失的信息为零。列表化留给后续。</p>
     */
    protected record SitePair(String siteId, String modelId, String label) {
    }

    protected static <T extends com.github.tartaricacid.touhoulittlemaid.ai.service.Site> java.util.List<SitePair> sitePairs(
            Map<String, T> sites) {
        java.util.List<SitePair> pairs = new java.util.ArrayList<>();
        sites.forEach((id, site) -> {
            if (!site.enabled()) {
                return;
            }
            String siteName = siteDisplayName(id);
            if (site instanceof com.github.tartaricacid.touhoulittlemaid.ai.service.SupportModelSelect select
                    && !select.models().isEmpty()) {
                select.models().forEach((modelId, modelName) ->
                        pairs.add(new SitePair(id, modelId, siteName + " / " + modelName)));
            } else {
                // 系统语音这类没有模型概念的站点：一对，模型留空
                pairs.add(new SitePair(id, "", siteName));
            }
        });
        return pairs;
    }

    protected static String siteDisplayName(String siteId) {
        String key = "ai.touhou_little_maid.chat.site.%s.name".formatted(siteId);
        return net.minecraft.client.resources.language.I18n.exists(key)
                ? net.minecraft.client.resources.language.I18n.get(key) : siteId;
    }

    protected static String pairLabel(java.util.List<SitePair> pairs, String siteId, String modelId, String builtinSiteId) {
        for (SitePair pair : pairs) {
            if (pair.siteId().equals(siteId) && pair.modelId().equals(modelId)) {
                return pair.label();
            }
        }
        // 空值 = 内置兜底：把兜底显示出来而不是显示空白
        return siteDisplayName(org.apache.commons.lang3.StringUtils.isBlank(siteId) ? builtinSiteId : siteId);
    }

    protected static int pairIndex(java.util.List<SitePair> pairs, String siteId, String modelId) {
        for (int i = 0; i < pairs.size(); i++) {
            if (pairs.get(i).siteId().equals(siteId) && pairs.get(i).modelId().equals(modelId)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 文本项的一行：标签左置 + 有边框输入框（自绘无边框输入框在暗底上就是一片看不见的空当）。
     * <b>跨 init 保留输入内容</b>：改窗口大小或点一下开关都会重建控件，
     * 若每次都从 session 重读，管理员正在输入的东西就没了。
     */
    protected EditBox addRuleInput(int y, Component compactLabel,
                                   @Nullable EditBox previous, String fallback) {
        return this.addRuleInput(y, compactLabel, null, previous, fallback);
    }

    protected EditBox addRuleInput(int y, Component compactLabel, @Nullable Component tooltip,
                                   @Nullable EditBox previous, String fallback) {
        this.addRowLabel(y, compactLabel, tooltip);
        EditBox box = new EditBox(this.font, this.rowControlX(), y, this.rowControlWidth(), 20, compactLabel);
        box.setMaxLength(512);
        box.setValue(previous != null ? previous.getValue() : fallback);
        this.addRenderableWidget(box);
        return box;
    }

    // ==================== 通用弹出列表（默认模型 / 默认音色 / 麦克风选择器共用） ====================
    //
    // 循环按钮被否掉的理由：候选一多就得像拨转盘一样逐格点过去，点过了还要绕整圈。
    // 弹出列表点开即列全部候选、当前项高亮，是 T 屏弹出层的同一套交互语法。

    /** 弹出列表的一行：文案 + 是否当前项 + 点中后干什么 */
    protected record HubPopupEntry(Component label, boolean selected, Runnable onPick) {
    }

    private static final int POPUP_ROW_HEIGHT = 14;
    private @Nullable java.util.List<HubPopupEntry> popupEntries;
    private int popupX;
    private int popupY;
    private int popupWidth;
    private int popupVisible;
    private int popupScroll;

    /** 从锚点控件（x, anchorY, 高 20）弹出；下方放不下三行就改为向上弹 */
    protected void openHubPopup(int x, int anchorY, int width, java.util.List<HubPopupEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        this.popupEntries = entries;
        this.popupScroll = 0;
        this.popupX = x;
        this.popupWidth = width;
        int spaceBelow = this.getFooterTop() - 4 - (anchorY + 22);
        int spaceAbove = anchorY - 2 - this.getContentY();
        int full = entries.size() * POPUP_ROW_HEIGHT;
        if (spaceBelow >= Math.min(full, POPUP_ROW_HEIGHT * 3)) {
            this.popupVisible = Math.min(entries.size(), Math.max(1, spaceBelow / POPUP_ROW_HEIGHT));
            this.popupY = anchorY + 22;
        } else {
            this.popupVisible = Math.min(entries.size(), Math.max(1, spaceAbove / POPUP_ROW_HEIGHT));
            this.popupY = anchorY - 2 - this.popupVisible * POPUP_ROW_HEIGHT;
        }
    }

    protected void closeHubPopup() {
        this.popupEntries = null;
    }

    /**
     * 覆盖层（弹出列表）绘制。**每个子类 render 的最后一行必须调用它**——
     * 弹出层要压在滚动条、提示字这些子类自绘物之上，而那些都画在 super.render 之后。
     */
    protected void renderHubOverlays(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.popupEntries == null) {
            return;
        }
        int bottom = this.popupY + this.popupVisible * POPUP_ROW_HEIGHT;
        graphics.fill(this.popupX - 1, this.popupY - 1, this.popupX + this.popupWidth + 1, bottom + 1, 0xFF3A3A3A);
        graphics.fill(this.popupX, this.popupY, this.popupX + this.popupWidth, bottom, 0xF8101010);
        for (int i = 0; i < this.popupVisible; i++) {
            int index = this.popupScroll + i;
            if (index >= this.popupEntries.size()) {
                break;
            }
            HubPopupEntry entry = this.popupEntries.get(index);
            int rowY = this.popupY + i * POPUP_ROW_HEIGHT;
            boolean hovered = mouseX >= this.popupX && mouseX < this.popupX + this.popupWidth
                    && mouseY >= rowY && mouseY < rowY + POPUP_ROW_HEIGHT;
            if (hovered) {
                graphics.fill(this.popupX, rowY, this.popupX + this.popupWidth, rowY + POPUP_ROW_HEIGHT, 0x2FF3EFE0);
            }
            if (entry.selected()) {
                graphics.fill(this.popupX + 1, rowY + 1, this.popupX + 3, rowY + POPUP_ROW_HEIGHT - 1, 0xFF55FF55);
            }
            String label = this.font.plainSubstrByWidth(entry.label().getString(), this.popupWidth - 14);
            graphics.drawString(this.font, label, this.popupX + 7, rowY + 3,
                    entry.selected() ? 0xFFFFFFFF : 0xFFCCCCCC, false);
        }
        if (this.popupEntries.size() > this.popupVisible) {
            int trackHeight = this.popupVisible * POPUP_ROW_HEIGHT - 2;
            int thumbHeight = Math.max(8, trackHeight * this.popupVisible / this.popupEntries.size());
            int scrollRange = Math.max(1, this.popupEntries.size() - this.popupVisible);
            int thumbOffset = (trackHeight - thumbHeight) * this.popupScroll / scrollRange;
            graphics.fill(this.popupX + this.popupWidth - 3, this.popupY + 1 + thumbOffset,
                    this.popupX + this.popupWidth - 1, this.popupY + 1 + thumbOffset + thumbHeight, 0xFF55FF55);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        if (this.popupEntries != null) {
            int bottom = this.popupY + this.popupVisible * POPUP_ROW_HEIGHT;
            boolean inside = event.x() >= this.popupX && event.x() < this.popupX + this.popupWidth
                    && event.y() >= this.popupY && event.y() < bottom;
            if (inside && event.button() == org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                int index = this.popupScroll + (int) ((event.y() - this.popupY) / POPUP_ROW_HEIGHT);
                if (index < this.popupEntries.size()) {
                    Runnable onPick = this.popupEntries.get(index).onPick();
                    this.closeHubPopup();
                    onPick.run();
                    return true;
                }
            }
            // 点外面 = 只关弹层，这一击不再落到底下的控件上
            this.closeHubPopup();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalDelta, double verticalDelta) {
        if (this.popupEntries != null) {
            int maxOffset = Math.max(0, this.popupEntries.size() - this.popupVisible);
            this.popupScroll = Math.max(0, Math.min(maxOffset, this.popupScroll + (verticalDelta < 0 ? 1 : -1)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalDelta, verticalDelta);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (this.popupEntries != null && event.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            this.closeHubPopup();
            return true;
        }
        return super.keyPressed(event);
    }

    // ==================== 保存的正反馈 ====================

    private @Nullable Component savedFlashText;
    private long savedFlashUntil;

    /**
     * 保存成功后的绿字：`*` 熄灭太含蓄。
     *
     * <p>没有专服变体：本枢纽的规则全是 AI 规则（§17 v2 实例级），**任何服务器形态保存即生效**；
     * 曾经的「重载后生效」字样连同它的歧义（哪个 reload？）一起退场。</p>
     */
    protected void flashSaved() {
        this.savedFlashText = Component.translatable("ai.touhou_little_maid.chat.settings.hub.saved");
        this.savedFlashUntil = System.currentTimeMillis() + 2000;
    }

    /**
     * 画在页脚按钮行内、保存按钮右侧的空当——那是**所有页面都恒空**的位置。
     * 曾放在页脚上方一行：那个槽在 LLM 页是新建按钮、在语音输入页是代理行，两处都被压字。
     */
    protected void renderSavedFlash(GuiGraphics graphics) {
        if (this.savedFlashText == null || System.currentTimeMillis() > this.savedFlashUntil) {
            return;
        }
        graphics.drawString(this.font, this.savedFlashText,
                this.getContentX() + 86, this.getFooterY() + 6, 0xFF55FF55, false);
    }

    protected int getVisibleListCount(int rowHeight) {
        return Math.max(1, (int) (this.listArea.h / rowHeight));
    }

    /**
     * 在列表区域右侧绘制滚动条
     */
    protected void renderListScrollbar(GuiGraphics graphics, int totalCount, int visibleCount) {
        if (this.listArea == null || totalCount <= visibleCount) {
            return;
        }
        int trackTop = (int) this.listArea.y;
        int trackHeight = (int) this.listArea.h - 5;
        int thumbHeight = Math.max(12, visibleCount * trackHeight / totalCount);
        int scrollRange = Math.max(1, totalCount - visibleCount);
        int thumbOffset = (trackHeight - thumbHeight) * this.listScrollOffset / scrollRange;
        graphics.fill(
                (int) this.listArea.right() + 2,
                trackTop + thumbOffset,
                (int) this.listArea.right() + 4,
                trackTop + thumbOffset + thumbHeight,
                0xFF55FF55
        );
    }

    /**
     * 在 LLM 和 TTS 站点权限不足时绘制提示文本
     */
    protected void renderInsufficientPermissions(GuiGraphics graphics) {
        if (this.insufficientPermissions) {
            MutableComponent text = Component.translatable("ai.touhou_little_maid.chat.settings.hub.insufficient_permissions");
            graphics.drawWordWrap(font, text, getContentX() + 20, getContentY() + 20, getContentWidth() - 60, 0xFFFF5555);
        }
    }

    /**
     * 处理列表区域的滚轮滚动，返回 true 表示已消费事件
     */
    protected boolean handleListScroll(double mouseX, double mouseY, double delta, int totalCount, int visibleCount) {
        // 弹出列表打开时滚轮归它，别让底下的站点列表跟着滚
        if (this.popupEntries != null) {
            return false;
        }
        if (this.listArea == null || !this.listArea.contains(mouseX, mouseY)) {
            return false;
        }
        int maxOffset = Math.max(0, totalCount - visibleCount);
        if (delta < 0 && this.listScrollOffset < maxOffset) {
            this.listScrollOffset++;
            this.init();
            return true;
        }
        if (delta > 0 && this.listScrollOffset > 0) {
            this.listScrollOffset--;
            this.init();
            return true;
        }
        return false;
    }

    protected void switchTo(Type type) {
        if (this.minecraft == null) {
            return;
        }
        this.persistTransientState();
        this.minecraft.setScreen(this.createTabScreen(type));
    }

    protected AIChatSettingsHubScreen createTabScreen(Type type) {
        return switch (type) {
            case LLM_SITE -> new AIChatSettingsLLMSiteScreen(this.parent, this.state, this.insufficientPermissions);
            case TTS_SITE -> new AIChatSettingsTTSSiteScreen(this.parent, this.state, this.insufficientPermissions);
            case STT_CONFIG -> new AIChatSettingsSTTConfigScreen(this.parent, this.state, this.insufficientPermissions);
            case USAGE -> new AIChatSettingsUsageScreen(this.parent, this.state, this.insufficientPermissions);
        };
    }

    /**
     * 服务端同步站点数据后，用新数据重新打开当前标签页
     */
    public void reopenSelf(Map<String, LLMSite> llmSites, Map<String, TTSSite> ttsSites) {
        if (this.minecraft == null) {
            return;
        }
        this.persistTransientState();
        this.state.llmSites.clear();
        this.state.llmSites.putAll(llmSites);
        this.state.ttsSites.clear();
        this.state.ttsSites.putAll(ttsSites);
        this.minecraft.setScreen(this.createTabScreen(this.getType()));
    }

    /**
     * 栏一「语音输入设置」——人人可见，作用对象是本机。
     *
     * <p>原先这一组下面还有一个「语音输入站点」标签。它现在从侧栏撤下，
     * 改由来源列表每一行的齿轮进入——每家的密钥表单本就是那一档自己的事，
     * 摆成同级标签会让人以为它和「语音输入设置」是并列的两件事。</p>
     */
    private int addVoiceInputSideButtons(int sideY) {
        this.addRenderableOnly(new SideGroupWidget(this.startX, sideY, VOICE_INPUT_NAME));

        sideY += 20;
        this.addRenderableWidget(new SideButton(Type.STT_CONFIG, this.startX, sideY, STT_CONFIG_NAME, b -> this.switchTo(Type.STT_CONFIG)));

        return sideY;
    }

    /** 栏二「AI 服务配置」——仅有权限者可见：用哪一家、凭据是什么。 */
    private int addServiceSideButtons(int sideY) {
        sideY += 30;
        this.addRenderableOnly(new SideGroupWidget(this.startX, sideY, SERVICE_NAME));

        sideY += 20;
        this.addRenderableWidget(new SideButton(Type.LLM_SITE, this.startX, sideY, SITE_LLM_NAME, b -> this.switchTo(Type.LLM_SITE)));

        sideY += 20;
        this.addRenderableWidget(new SideButton(Type.TTS_SITE, this.startX, sideY, SITE_TTS_NAME, b -> this.switchTo(Type.TTS_SITE)));

        return sideY;
    }

    /**
     * 栏三「用量管理」——仅有权限者可见：花多少钱、配额多少。
     *
     * <p>收录判据是**只改变花多少钱、不改变功能**。按这条，「自动生成人设」不进这里——
     * 关掉它女仆就不再自动有人设，那是功能变化。守不住这条判据，这一栏会慢慢变成 LLM 杂项的垃圾桶。</p>
     */
    private void addUsageSideButtons(int sideY) {
        sideY += 30;
        this.addRenderableOnly(new SideGroupWidget(this.startX, sideY, USAGE_NAME));

        sideY += 20;
        this.addRenderableWidget(new SideButton(Type.USAGE, this.startX, sideY, USAGE_QUOTA_NAME, b -> this.switchTo(Type.USAGE)));
    }

    protected void addFooterButtons() {
        int x = this.getContentX();
        int y = this.getFooterY();
        this.addRenderableWidget(new FlatColorButton(x + this.getContentWidth() - 80, y, 80, 20, GUI_BACK, b -> this.onClose()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        // 离开时丢弃在途试听：留着它会让下一个屏的试听按钮白白灰 10 秒
        com.github.tartaricacid.touhoulittlemaid.client.sound.VoicePreviewClient.forget();
        if (this.parent instanceof AIChatScreen chatScreen && chatScreen.getMaid().isAlive()) {
            ClientPlayNetworking.send(new OpenMaidAIChatPacket(chatScreen.getMaid()));
        } else {
            Screens.getClient(this).setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static AIChatSettingsHubScreen openDefault(
            @Nullable Screen parent,
            Map<String, LLMSite> llmSites,
            Map<String, TTSSite> ttsSites,
            boolean insufficientPermissions
    ) {
        // 无权限者的侧栏只有「语音输入设置」一栏——落点必须落在栏里存在的标签上，
        // 落到 LLM 标签会出现「高亮的标签在侧栏里根本找不到」的鬼状态
        if (insufficientPermissions) {
            return new AIChatSettingsSTTConfigScreen(parent, SharedState.create(llmSites, ttsSites), true);
        }
        return new AIChatSettingsLLMSiteScreen(parent, llmSites, ttsSites, insufficientPermissions);
    }

    public static AIChatSettingsHubScreen openSTTConfig(@Nullable Screen parent) {
        SharedState state = SharedState.create(Map.of(), Map.of());
        state.sttOnly = true;
        return new AIChatSettingsSTTConfigScreen(parent, state, false);
    }

    public static final class SharedState {
        public final Map<String, LLMSite> llmSites;
        public final Map<String, TTSSite> ttsSites;
        public final Map<String, STTSite> sttSites;

        public boolean sttEnabled;
        public STTApiType sttType;
        public String sttMicrophone;
        public int maidCanChatDistance;
        public String sttProxyAddress;

        // 临时变量
        public int llmListScrollOffset;
        public int ttsListScrollOffset;
        private boolean sttOnly;
        /** 世界规则暂存，四个标签页共享——切页不丢未保存改动，哪页点保存都提交全部 */
        ServerRulesClientCache.Session ruleSession;

        private SharedState(Map<String, LLMSite> llmSites,
                            Map<String, TTSSite> ttsSites,
                            Map<String, STTSite> sttSites,
                            boolean sttEnabled,
                            STTApiType sttType,
                            String sttMicrophone,
                            int maidCanChatDistance,
                            String sttProxyAddress
        ) {
            this.llmSites = llmSites;
            this.ttsSites = ttsSites;
            this.sttSites = sttSites;
            this.sttEnabled = sttEnabled;
            this.sttType = sttType;
            this.sttMicrophone = sttMicrophone;
            this.maidCanChatDistance = maidCanChatDistance;
            this.sttProxyAddress = sttProxyAddress;
            this.llmListScrollOffset = 0;
            this.ttsListScrollOffset = 0;
            this.sttOnly = false;
        }

        private static SharedState create(Map<String, LLMSite> llmSites, Map<String, TTSSite> ttsSites) {
            return new SharedState(
                    Maps.newLinkedHashMap(llmSites),
                    Maps.newLinkedHashMap(ttsSites),
                    Maps.newLinkedHashMap(AvailableSites.STT_SITES),
                    AIConfig.STT_ENABLED.get(),
                    AIConfig.STT_TYPE.get(),
                    AIConfig.STT_MICROPHONE.get(),
                    AIConfig.MAID_CAN_CHAT_DISTANCE.get(),
                    AIConfig.STT_PROXY_ADDRESS.get()
            );
        }
    }

    public enum Type {
        LLM_SITE,
        TTS_SITE,
        STT_CONFIG,
        USAGE
    }
}
