package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai;

import cn.sh1rocu.touhoulittlemaid.mixin.accessor.ScreenAccessor;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.ChatClientInfo;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatManager;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatSerializable;
import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.ClientAvailableSitesSync;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.DefaultLLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMMessage;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.Role;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.SupportLanguage;
import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.system.TTSSystemSite;
import com.github.tartaricacid.touhoulittlemaid.client.ClientLocalChat;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.FlatColorButton;
import com.github.tartaricacid.touhoulittlemaid.client.sound.VoicePreviewClient;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.network.message.SendUserChatPackage;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.OpenAIConfigPacket;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.SaveMaidAIDataPackage;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.ScreenUtil;
import com.google.common.collect.Lists;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.github.tartaricacid.touhoulittlemaid.ai.manager.entity.MaidAIChatSerializable.NO_TTS_SITE;

public class AIChatScreen extends Screen {
    private static final int POPUP_ROW_HEIGHT = 16;
    // 六颗图标按钮的字形在 20px 方钮里既不居中也偏小，逐颗量出的微调值。
    // 数值本身没有推导，是照着屏幕挪出来的——改字形或改钮宽都要重新量。
    private static final float CONFIG_ICON_SCALE = 1.12f;
    private static final float TTS_ICON_SCALE = 1.25f;
    private static final float LLM_SUMMARY_TEXT_SCALE = 0.70f;
    private static final float TTS_SUMMARY_TEXT_SCALE = 0.65f;
    private static final float TOKEN_TEXT_SCALE = 0.70f;

    private final EntityMaid maid;
    private final MaidAIChatManager manager;

    private int currentTokens = 0;
    private int maxTokens = Integer.MAX_VALUE;

    private EditBox input;

    private FlatColorButton configButton;
    private FlatColorButton historyButton;
    private FlatColorButton settingButton;

    private FlatColorButton llmButton;
    private FlatColorButton ttsButton;
    private FlatColorButton langButton;

    private PopupType openPopup;
    private int popupScrollOffset;
    private @Nullable PopupGeometry popupGeometry;

    private int inputHistoryIndex;

    /**
     * 用来禁止快捷键打开时，会把快捷键输入到聊天框里的问题<br>
     * 开屏后前几帧不处理输入
     */
    private int tickCounter = 0;

    public AIChatScreen(EntityMaid maid) {
        super(Component.literal("Maid AI Chat Screen"));
        this.maid = maid;
        this.manager = maid.getAiChatManager();
    }

    public void updateTokens(int current, int max) {
        this.currentTokens = current;
        this.maxTokens = max;
    }

    @Override
    protected void init() {
        String currentInput = this.input != null ? this.input.getValue() : StringUtils.EMPTY;
        this.clearWidgets();

        int posX = this.width / 2;
        int posY = this.height / 2;

        int inputX = posX - 165;
        int inputY = posY + 58;
        int inputWidth = 330;

        // 添加聊天输入框
        this.addInputWidget(inputX, inputY, inputWidth, currentInput);
        // 模型存在判定
        this.ensureValidSelections();

        // 添加上侧配置按钮
        int y = inputY - 28, size = 18, gap = 2;
        this.addLeftButtons(inputX - 8, y, size, gap);
        this.addRightButtons(inputX + inputWidth - size * 3 + gap * 2, y, size, gap);
        // 刷新下拉框相关内容的状态
        this.refreshSelectorButtons();
    }

    private void addInputWidget(int inputX, int inputY, int inputWidth, String currentInput) {
        this.input = new EditBox(Screens.getMinecraft(this).fontFilterFishy, inputX, inputY, inputWidth, 20, Component.translatable("chat.editBox"));
        this.input.setMaxLength(128);
        this.input.setBordered(false);
        this.input.setValue(currentInput);
        this.input.setCanLoseFocus(false);

        this.addWidget(this.input);
        this.setInitialFocus(this.input);
        this.inputHistoryIndex = 0;
    }

    private void addLeftButtons(int leftX, int y, int size, int gap) {
        this.historyButton = this.addRenderableWidget(new FlatColorButton(leftX, y, size, size, Component.literal("🕑"), b -> {
            HistoryAIChatScreen screen = new HistoryAIChatScreen(this, this.maid);
            ScreenUtil.setScreen(screen);
        }).setTextTransform(1.0f, 0.5f, 0.5f)
                .setTooltips("ai.touhou_little_maid.chat.button.history.tip"));

        leftX = leftX + size + gap;
        this.settingButton = this.addRenderableWidget(new FlatColorButton(leftX, y, size, size, Component.literal("✎"), b -> {
            SettingEditScreen editScreen = new SettingEditScreen(this, this.maid);
            ScreenUtil.setScreen(editScreen);
        }).setTextTransform(1.0f, 0.5f, 1.0f)
                .setTooltips("ai.touhou_little_maid.chat.button.setting.tip"));

        leftX = leftX + size + gap;
        this.configButton = this.addRenderableWidget(new FlatColorButton(leftX, y, size, size, Component.literal("⚙"), b -> {
            OpenAIConfigPacket.sendToServer();
        }).setTextTransform(CONFIG_ICON_SCALE, 0.0f, 0.25f)
                .setTooltips("ai.touhou_little_maid.chat.button.config.tip"));
    }

    private void addRightButtons(int rightX, int y, int size, int gap) {
        this.llmButton = this.addRenderableWidget(new FlatColorButton(rightX, y, size, size, Component.literal("✦"),
                b -> this.togglePopup(PopupType.LLM)).setTextTransform(1.0f, 0.75f, 0.5f)
                .setTooltips("ai.touhou_little_maid.chat.button.llm.tip"));

        rightX = rightX + size + gap;
        this.ttsButton = this.addRenderableWidget(new FlatColorButton(rightX, y, size, size, Component.literal("🔊"),
                b -> this.togglePopup(PopupType.TTS)).setTextTransform(TTS_ICON_SCALE, 0.5f, 0.0f)
                .setTooltips("ai.touhou_little_maid.chat.button.tts.tip"));

        rightX = rightX + size + gap;
        this.langButton = this.addRenderableWidget(new FlatColorButton(rightX, y, size, size, Component.literal("🌐"),
                b -> this.togglePopup(PopupType.LANGUAGE)).setTextTransform(1.0f, 0.75f, 0.5f)
                .setTooltips("ai.touhou_little_maid.chat.button.language.tip"));
    }

    /**
     * 合成语种按钮在系统站点上是个**无效开关**，必须在它自己身上说出来。
     *
     * <p>{@code TTSSystemClient.play} 把 {@code TTSConfig}（含语种与音色）整个丢掉，
     * 原版朗读器接口也不接受这两个参数——语音完全由操作系统决定。而这个按钮看得见、点得动、
     * 存得下：<b>一个能设置却不起作用的选项，比没有这个选项更糟</b>。站点编辑屏那两行说明只有
     * 打开编辑屏的人看得到，而绝大多数玩家是在这里改语种的，所以这一处才是要紧的。</p>
     *
     * <p>不禁用按钮：玩家可能正准备换到别的站点，那时这个设置又有效了。</p>
     */
    private void refreshLanguageTooltip() {
        if (this.effectiveTtsSiteIsSystem()) {
            this.langButton.setTooltips(List.of(
                    Component.translatable("ai.touhou_little_maid.chat.button.language.tip"),
                    Component.translatable("ai.touhou_little_maid.chat.settings.hub.system_tts_language_hint")
                            .withStyle(ChatFormatting.YELLOW)));
        } else {
            this.langButton.setTooltips("ai.touhou_little_maid.chat.button.language.tip");
        }
    }

    /** 与 {@link #resolvedDefaultTtsSiteId()} 同一套镜像解析：跟随默认时要看默认解析成了谁 */
    private boolean effectiveTtsSiteIsSystem() {
        if (MaidAIChatSerializable.isNoTTSSite(this.manager.ttsSite)) {
            return false;
        }
        String siteId = StringUtils.isBlank(this.manager.ttsSite)
                ? this.resolvedDefaultTtsSiteId()
                : this.manager.ttsSite;
        return TTSSystemSite.API_TYPE.equals(siteId);
    }

    private void togglePopup(PopupType type) {
        if (this.openPopup == type) {
            // 类型相同，说明是二次点击，关闭
            this.openPopup = null;
        } else {
            this.openPopup = type;
        }
        this.refreshSelectorButtons();
    }

    private void refreshSelectorButtons() {
        this.llmButton.setSelect(this.openPopup == PopupType.LLM);
        this.ttsButton.setSelect(this.openPopup == PopupType.TTS);
        this.langButton.setSelect(this.openPopup == PopupType.LANGUAGE);

        this.llmButton.active = !ClientAvailableSitesSync.getClientLLMSites().isEmpty();
        this.ttsButton.active = !this.getPopupEntries(PopupType.TTS).isEmpty();
        this.langButton.active = !SupportLanguage.SUPPORTED_LANGUAGES.isEmpty();
        this.refreshLanguageTooltip();

        if (this.openPopup == null) {
            this.popupGeometry = null;
            this.popupScrollOffset = 0;
        } else {
            this.popupGeometry = this.getPopupGeometry(this.openPopup);
            this.popupScrollOffset = this.getInitialPopupOffset(this.openPopup);
        }
    }

    private void applyPopupSelection(PopupType type, PopupEntry entry) {
        if (entry.header()) {
            return;
        }

        switch (type) {
            case LLM -> {
                this.manager.llmSite = entry.site();
                this.manager.llmModel = entry.model();
            }
            case TTS -> {
                this.manager.ttsSite = entry.site();
                this.manager.ttsModel = entry.model();
            }
            case LANGUAGE -> this.manager.ttsLanguage = entry.language();
        }

        // 验证结果，并向服务端发送确认信息
        this.ensureValidSelections();
        ClientPlayNetworking.send(new SaveMaidAIDataPackage(this.maid.getId(), this.manager));

        // 关闭下拉框
        this.openPopup = null;
        this.refreshSelectorButtons();
    }

    private List<PopupEntry> getPopupEntries(PopupType type) {
        List<PopupEntry> entries = Lists.newArrayList();
        if (Objects.requireNonNull(type) == PopupType.LLM) {
            var llmSites = ClientAvailableSitesSync.getClientLLMSites();
            entries.add(this.followDefaultEntry(this.defaultLlmLabel(llmSites), StringUtils.isBlank(this.manager.llmSite)));
            this.addSiteModelEntries(entries, llmSites, this.manager.llmSite, this.manager.llmModel);
            return entries;
        }

        if (type == PopupType.TTS) {
            var ttsSites = ClientAvailableSitesSync.getClientTTSSites();
            entries.add(this.followDefaultEntry(this.defaultTtsLabel(ttsSites), StringUtils.isBlank(this.manager.ttsSite)));

            boolean selected = MaidAIChatSerializable.isNoTTSSite(this.manager.ttsSite);
            MutableComponent noneName = Component.translatable("ai.touhou_little_maid.chat.site.none.name");
            entries.add(new PopupEntry(noneName, false, selected, NO_TTS_SITE, StringUtils.EMPTY, null));

            // 高亮用真实存储值：空值属于上面的「跟随默认」项，不再折算成 system
            this.addSiteModelEntries(entries, ttsSites, this.manager.ttsSite, this.manager.ttsModel);
            return entries;
        }

        SupportLanguage.SUPPORTED_LANGUAGES.forEach(language -> {
            Component name = SupportLanguage.getLanguageName(language);
            boolean selected = language.equals(this.manager.ttsLanguage);
            PopupEntry entry = new PopupEntry(name, false, selected, null, null, language);
            entries.add(entry);
        });
        return entries;
    }

    private void addSiteModelEntries(List<PopupEntry> entries, Map<String, Map<String, String>> sites, String selectedSite, String selectedModel) {
        sites.forEach((site, models) -> {
            // 按站点添加主分类
            // 分组头给玩家看的是站点名字，不是内部 id（deepseek/system 直接当标题是又一处裸 id）
            MutableComponent siteName = Component.literal(displaySiteName(site));
            PopupEntry entry = new PopupEntry(siteName, true, false, site, null, null);
            entries.add(entry);

            // 如果模型为空，添加占位符
            if (models == null || models.isEmpty()) {
                boolean selected = site.equals(selectedSite) && StringUtils.isBlank(selectedModel);
                MutableComponent name = Component.literal("*");
                PopupEntry popupEntry = new PopupEntry(name, false, selected, site, StringUtils.EMPTY, null);
                entries.add(popupEntry);
                return;
            }

            // 否则正常添加模型
            models.forEach((modelId, modelName) -> {
                MutableComponent name = Component.literal(modelName);
                boolean selected = site.equals(selectedSite) && modelId.equals(selectedModel);
                PopupEntry popupEntry = new PopupEntry(name, false, selected, site, modelId, null);
                entries.add(popupEntry);
            });
        });
    }

    /**
     * 计算下拉列表第一次打开时的初始滚动偏移。
     * 目标是让当前已选中的条目尽量出现在可视范围内，
     * 如果选中项正好属于某个分组，则优先把分组标题也一起显示出来。
     */
    private int getInitialPopupOffset(PopupType type) {
        // 依据当前窗口大小，决定下拉列表的长度
        int popupMaxVisible = this.getPopupMaxVisible();

        List<PopupEntry> entries = this.getPopupEntries(type);
        if (entries.size() <= popupMaxVisible) {
            return 0;
        }
        int selectedIndex = this.getSelectedPopupIndex(entries);
        if (selectedIndex < 0) {
            return 0;
        }
        // 默认以当前选中项作为锚点，让弹窗打开时直接定位到它附近。
        int anchorIndex = selectedIndex;
        // 如果选中项前一行是分组标题，就把标题也带上，避免只看到子项看不到分组名。
        if (0 < selectedIndex && entries.get(selectedIndex - 1).header()) {
            anchorIndex = selectedIndex - 1;
        }
        return Math.max(0, Math.min(anchorIndex, entries.size() - popupMaxVisible));
    }

    private int getPopupMaxVisible() {
        return this.height / 2 / POPUP_ROW_HEIGHT;
    }

    /**
     * 在线性列表中查找第一个被选中的条目下标；找不到就返回 -1。
     */
    private int getSelectedPopupIndex(List<PopupEntry> entries) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).selected()) {
                return i;
            }
        }
        return -1;
    }

    private FlatColorButton getPopupAnchor(PopupType type) {
        return switch (type) {
            case LLM -> this.llmButton;
            case TTS -> this.ttsButton;
            case LANGUAGE -> this.langButton;
        };
    }

    /**
     * 空的站点选择 = 「跟随默认」，是**合法状态**——这里不再把它填成具体站点。
     *
     * <p>旧实现开屏就把空值补成默认站点，于是玩家第一次点任何弹出项时，连同这份「补出来的选择」
     * 一起保存了——「没设过」就此变成「设过了」，这正是跟随语义做不出来的根源。
     * 覆盖指向的站点/模型失效时也不在这里改写：解析链负责回落，界面负责把「已回落」显示出来，
     * 静默改写存储会让回落变成永久降级。</p>
     */
    private void ensureValidSelections() {
        if (MaidAIChatSerializable.isNoTTSSite(this.manager.ttsSite)) {
            this.manager.ttsModel = StringUtils.EMPTY;
        }

        // 语言存在判定（沿用旧行为）
        if (SupportLanguage.SUPPORTED_LANGUAGES.isEmpty() || StringUtils.isBlank(this.manager.ttsLanguage)) {
            this.manager.ttsLanguage = "en_us";
            return;
        }
        if (!SupportLanguage.SUPPORTED_LANGUAGES.contains(this.manager.ttsLanguage)) {
            this.manager.ttsLanguage = "en_us";
        }
    }

    @Override
    public void resize(int pWidth, int pHeight) {
        String chatText = this.input.getValue();
        super.resize(pWidth, pHeight);
        this.input.setValue(chatText);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        // 不渲染背景
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        if (this.input != null) {
            int x = this.input.getX();
            int y = this.input.getY();
            int w = this.input.getInnerWidth();
            int h = this.input.getHeight();
            String value = this.input.getValue();

            graphics.fill(x - 8, y - 8, x + w + 8, y + h - 6, 0xBF090909);
            this.input.extractRenderState(graphics, mouseX, mouseY, partialTicks);

            if (StringUtils.isEmpty(value)) {
                MutableComponent text = Component.translatable("ai.touhou_little_maid.chat.input.tip")
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
                graphics.centeredText(this.font, text, x + w / 2, y + (h - 22) / 2, 0xFFFFFFFF);
            }
        }

        this.renderSelectionSummaries(graphics);
        this.renderTokenUsage(graphics);

        for (Renderable renderable : ((ScreenAccessor) this).tlm$getRenderables()) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        }

        if (this.openPopup != null) {
            this.renderPopup(graphics, mouseX, mouseY);
        } else {
            this.historyButton.renderToolTip(graphics, this, mouseX, mouseY);
            this.settingButton.renderToolTip(graphics, this, mouseX, mouseY);
            this.configButton.renderToolTip(graphics, this, mouseX, mouseY);
            this.llmButton.renderToolTip(graphics, this, mouseX, mouseY);
            this.ttsButton.renderToolTip(graphics, this, mouseX, mouseY);
            this.langButton.renderToolTip(graphics, this, mouseX, mouseY);
        }
    }

    /**
     * 底部概要给玩家看的是名字，不是内部 id。原实现把站点 id（"system"）和空值哨兵（"*"）
     * 原样印在屏上——玩家读到的每一个字都是断言，内部标识不该混在其中。
     */
    private static String displaySiteName(String siteId) {
        if (StringUtils.isBlank(siteId)) {
            return "—";
        }
        String key = "ai.touhou_little_maid.chat.site.%s.name".formatted(siteId);
        return I18n.exists(key) ? I18n.get(key) : siteId;
    }

    /** 系统语音这类站点没有「模型」可言，此时只显示站点名，而不是拼一个「 / *」出来 */
    private static String joinSiteAndModel(String siteName, String modelName) {
        if (StringUtils.isBlank(modelName) || "*".equals(modelName)) {
            return siteName;
        }
        return siteName + " / " + modelName;
    }

    private void renderSelectionSummaries(GuiGraphicsExtractor graphics) {
        int left = this.input.getX() - 6;
        int right = this.input.getX() + this.input.getInnerWidth() + 6;
        int summaryY = this.input.getY() + 16;
        int halfWidth = (right - left) / 2;

        // 三态：跟随默认（空值，显示解析出的默认）· 已覆盖（具体值且在目录里）· 已回落（具体值但目录里没有）
        var llmCatalog = ClientAvailableSitesSync.getClientLLMSites();
        String llmText;
        String llmState;
        if (StringUtils.isBlank(this.manager.llmSite)) {
            llmText = this.defaultLlmLabel(llmCatalog);
            llmState = "follow";
        } else if (llmCatalog.containsKey(this.manager.llmSite)) {
            llmText = joinSiteAndModel(displaySiteName(this.manager.llmSite),
                    ClientAvailableSitesSync.getLLMModelName(this.manager.llmSite, this.manager.llmModel));
            llmState = "override";
        } else {
            llmText = this.defaultLlmLabel(llmCatalog);
            llmState = "fallback";
        }
        MutableComponent llmSummary = Component.translatable("ai.touhou_little_maid.chat.summary.llm",
                withStateSuffix(llmText, llmState));
        this.drawScaledSummary(graphics, llmSummary.getString(), left, summaryY, halfWidth,
                LLM_SUMMARY_TEXT_SCALE, false);

        var ttsCatalog = ClientAvailableSitesSync.getClientTTSSites();
        String ttsModelSummary;
        String ttsState;
        if (MaidAIChatSerializable.isNoTTSSite(this.manager.ttsSite)) {
            ttsModelSummary = Component.translatable("ai.touhou_little_maid.chat.site.none.name").getString();
            ttsState = "override";
        } else if (StringUtils.isBlank(this.manager.ttsSite)) {
            ttsModelSummary = this.defaultTtsLabel(ttsCatalog);
            ttsState = "follow";
        } else if (ttsCatalog.containsKey(this.manager.ttsSite)) {
            ttsModelSummary = joinSiteAndModel(displaySiteName(this.manager.ttsSite),
                    ClientAvailableSitesSync.getTTSModelName(this.manager.ttsSite, this.manager.ttsModel));
            ttsState = "override";
        } else {
            ttsModelSummary = this.defaultTtsLabel(ttsCatalog);
            ttsState = "fallback";
        }
        MutableComponent ttsSummary = Component.translatable("ai.touhou_little_maid.chat.summary.tts",
                withStateSuffix(ttsModelSummary, ttsState), SupportLanguage.getLanguageName(this.manager.ttsLanguage));
        this.drawScaledSummary(graphics, ttsSummary.getString(), right, summaryY, halfWidth,
                TTS_SUMMARY_TEXT_SCALE, true);
    }

    /**
     * 客户端侧镜像解析默认值，只为渲染标签；权威解析在服务端 {@code MaidAIChatData}。
     * 规则里的默认站点不在目录（没配或被禁）时，标签落到内置兜底——与服务端的滑落一致。
     */
    private String defaultLlmLabel(Map<String, Map<String, String>> catalog) {
        String rule = ServerRuleConfig.get(AIConfig.DEFAULT_LLM_SITE);
        String siteId = StringUtils.isNotBlank(rule) && catalog.containsKey(rule)
                ? rule : DefaultLLMSite.DEEPSEEK.id();
        String model = firstOrRuleModel(catalog.get(siteId),
                siteId.equals(rule) ? ServerRuleConfig.get(AIConfig.DEFAULT_LLM_MODEL) : StringUtils.EMPTY);
        return joinSiteAndModel(displaySiteName(siteId), model);
    }

    private String defaultTtsLabel(Map<String, Map<String, String>> catalog) {
        String rule = ServerRuleConfig.get(AIConfig.DEFAULT_TTS_SITE);
        String siteId = StringUtils.isNotBlank(rule) && catalog.containsKey(rule)
                ? rule : TTSSystemSite.API_TYPE;
        String model = firstOrRuleModel(catalog.get(siteId),
                siteId.equals(rule) ? ServerRuleConfig.get(AIConfig.DEFAULT_TTS_MODEL) : StringUtils.EMPTY);
        return joinSiteAndModel(displaySiteName(siteId), model);
    }

    private static String firstOrRuleModel(@Nullable Map<String, String> models, String ruleModel) {
        if (models == null || models.isEmpty()) {
            return StringUtils.EMPTY;
        }
        if (StringUtils.isNotBlank(ruleModel) && models.containsKey(ruleModel)) {
            return models.get(ruleModel);
        }
        return models.values().iterator().next();
    }

    private static String withStateSuffix(String text, String state) {
        return text + " · " + I18n.get("ai.touhou_little_maid.chat.summary.state." + state);
    }

    private PopupEntry followDefaultEntry(String defaultLabel, boolean selected) {
        MutableComponent label = Component.translatable("ai.touhou_little_maid.chat.popup.follow_default", defaultLabel);
        return new PopupEntry(label, false, selected, StringUtils.EMPTY, StringUtils.EMPTY, null);
    }

    private void drawScaledSummary(GuiGraphicsExtractor graphics, String text, int anchorX, int y, int maxWidth,
                                   float scale, boolean alignRight) {
        int scaledMaxWidth = Math.round(maxWidth / scale);
        String trimmed = this.trimToWidth(text, scaledMaxWidth);
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale);
        int scaledX = Math.round(anchorX / scale);
        if (alignRight) {
            scaledX -= this.font.width(trimmed);
        }
        graphics.text(this.font, trimmed, scaledX, Math.round(y / scale), 0xFFADADAD);
        graphics.pose().popMatrix();
    }

    /** 与 defaultTtsLabel 同一套镜像解析，但返回 id——网络请求要 id，标签要名字 */
    private String resolvedDefaultTtsSiteId() {
        var catalog = ClientAvailableSitesSync.getClientTTSSites();
        String rule = ServerRuleConfig.get(AIConfig.DEFAULT_TTS_SITE);
        return StringUtils.isNotBlank(rule) && catalog.containsKey(rule) ? rule : TTSSystemSite.API_TYPE;
    }

    private String resolvedDefaultTtsModelId() {
        var catalog = ClientAvailableSitesSync.getClientTTSSites();
        String siteId = this.resolvedDefaultTtsSiteId();
        var models = catalog.get(siteId);
        if (models == null || models.isEmpty()) {
            return StringUtils.EMPTY;
        }
        String rule = ServerRuleConfig.get(AIConfig.DEFAULT_TTS_MODEL);
        if (siteId.equals(ServerRuleConfig.get(AIConfig.DEFAULT_TTS_SITE))
                && StringUtils.isNotBlank(rule) && models.containsKey(rule)) {
            return rule;
        }
        return models.keySet().iterator().next();
    }

    /** 可试听：不是分组头、不是「不说话」。「跟随默认」也可试——试的就是当前默认那一把嗓子 */
    private boolean isPreviewableEntry(PopupEntry entry) {
        return !entry.header() && !NO_TTS_SITE.equals(entry.site());
    }

    private String previewSiteOf(PopupEntry entry) {
        if (StringUtils.isBlank(entry.site())) {
            return this.resolvedDefaultTtsSiteId();
        }
        return entry.site();
    }

    private String previewModelOf(PopupEntry entry) {
        if (StringUtils.isBlank(entry.site())) {
            return this.resolvedDefaultTtsModelId();
        }
        return StringUtils.defaultString(entry.model());
    }

    private void renderTokenUsage(GuiGraphicsExtractor graphics) {
        int left = this.input.getX() - 6;
        int right = this.input.getX() + this.input.getInnerWidth() + 6;
        int tokenY = this.input.getY() - 14;
        float scale = TOKEN_TEXT_SCALE;

        String currentStr = formatTokenCount(this.currentTokens);
        String text;
        if (this.maxTokens == Integer.MAX_VALUE) {
            text = "Token: %s / ∞".formatted(currentStr);
        } else {
            String maxStr = formatTokenCount(this.maxTokens);
            double percent = this.maxTokens > 0 ? (double) this.currentTokens / this.maxTokens * 100 : 0;
            text = "Token: %s / %s (%.1f%%)".formatted(currentStr, maxStr, percent);
        }

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale);
        int scaledX = Math.round((left + right) / 2.0f / scale) - this.font.width(text) / 2;
        int scaledY = Math.round(tokenY / scale);
        graphics.text(this.font, text, scaledX, scaledY, 0xFFADADAD);
        graphics.pose().popMatrix();
    }

    private static String formatTokenCount(int count) {
        if (count < 1000) {
            return String.valueOf(count);
        }
        if (count < 1_000_000) {
            return "%.1fK".formatted(count / 1000.0);
        }
        if (count < 1_000_000_000) {
            return "%.1fM".formatted(count / 1_000_000.0);
        }
        return "%.1fG".formatted(count / 1_000_000_000.0);
    }

    private String trimToWidth(String text, int maxWidth) {
        if (maxWidth <= 0 || this.font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "…";
        int trimWidth = Math.max(0, maxWidth - this.font.width(ellipsis));
        return this.font.plainSubstrByWidth(text, trimWidth) + ellipsis;
    }

    @Override
    public void tick() {
        tickCounter++;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.openPopup != null) {
            // 执行正常下拉框按钮点击
            if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.tryClickPopup(event.x(), event.y())) {
                return true;
            }

            // 如果悬浮于下拉框按钮上，正常触发开启与关闭
            if (this.isPopupTriggerHovered(event.x(), event.y())) {
                return super.mouseClicked(event, doubleClick);
            }

            // 否者关闭下拉框按钮
            this.openPopup = null;
            this.refreshSelectorButtons();
        }

        // 输入框点击
        if (this.input.mouseClicked(event, doubleClick)) {
            this.setFocused(this.input);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean isPopupTriggerHovered(double mouseX, double mouseY) {
        return this.isButtonHovered(this.llmButton, mouseX, mouseY)
               || this.isButtonHovered(this.ttsButton, mouseX, mouseY)
               || this.isButtonHovered(this.langButton, mouseX, mouseY);
    }

    private boolean isButtonHovered(FlatColorButton button, double mouseX, double mouseY) {
        return button != null && button.isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        // GUI 刚打开的 5 tick 内，不允许输入，否则会把按键录入
        if (this.tickCounter < 5) {
            return false;
        }
        return super.charTyped(event);
    }

    @Override
    protected void insertText(String text, boolean overwrite) {
        if (overwrite) {
            this.input.setValue(text);
        } else {
            this.input.insertText(text);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER) {
            this.sendDoneMessage();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_UP) {
            return this.recallHistory(-1);
        }
        if (event.key() == GLFW.GLFW_KEY_DOWN) {
            return this.recallHistory(1);
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalDelta, double verticalDelta) {
        if (this.openPopup == null) {
            return super.mouseScrolled(mouseX, mouseY, horizontalDelta, verticalDelta);
        }

        if (this.popupGeometry != null && this.popupGeometry.contains(mouseX, mouseY)) {
            int maxOffset = Math.max(0, this.popupGeometry.totalCount() - this.popupGeometry.visibleCount());
            if (verticalDelta < 0 && this.popupScrollOffset < maxOffset) {
                this.popupScrollOffset++;
                return true;
            }
            if (verticalDelta > 0 && this.popupScrollOffset > 0) {
                this.popupScrollOffset--;
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalDelta, verticalDelta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void sendDoneMessage() {
        String value = this.input.getValue();
        LocalPlayer player = Screens.getMinecraft(this).player;
        if (StringUtils.isNotBlank(value) && player != null) {
            ChatClientInfo clientInfo = ChatClientInfo.fromMaid(this.maid);
            ClientPlayNetworking.send(new SendUserChatPackage(this.maid.getId(), value, clientInfo));
            String format = "<%s> %s".formatted(player.getScoreboardName(), value);
            // ⚠️ 不能用 player.sendSystemMessage：javap 实证它在 26.1.2 直接走
            // ChatListener.handleSystemMessage，而本行格式恰是 "<玩家名> 文本"，
            // 正好命中 guessChatUUID 的 <...> 解析 → Minecraft.isBlocked →
            // PlayerSocialManager.pendingBlockListRefresh.join()，在渲染线程上硬等
            // Mojang 屏蔽名单。这是 C2③ 那条链在 GUI 层的同一处实例。
            ClientLocalChat.show(Component.literal(format).withStyle(ChatFormatting.GRAY));
        }
        this.onClose();
    }

    private void renderPopup(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.popupGeometry == null || this.popupGeometry.entries().isEmpty()) {
            return;
        }

        List<PopupEntry> entries = this.popupGeometry.entries();

        int x = this.popupGeometry.x();
        int y = this.popupGeometry.y();
        int width = this.popupGeometry.width();
        int height = this.popupGeometry.height();
        int visible = this.popupGeometry.visibleCount();
        int endIndex = Math.min(entries.size(), this.popupScrollOffset + visible);

        // 背景
        graphics.fill(x, y, x + width, y + height, 0xF0101010);

        // 渲染每个条目
        for (int i = this.popupScrollOffset; i < endIndex; i++) {
            PopupEntry entry = entries.get(i);
            int renderIndex = i - this.popupScrollOffset;
            int top = y + renderIndex * POPUP_ROW_HEIGHT;
            boolean hover = x <= mouseX && mouseX < x + width && top <= mouseY && mouseY < top + POPUP_ROW_HEIGHT;

            // 如果是分组标题
            if (entry.header()) {
                this.drawPopupText(graphics, entry.label().getString(), x + 6, top + 4, width - 12, 0xFFF2F2F2);
                continue;
            }

            if (entry.selected()) {
                // 如果是选中条目
                graphics.fill(x + 1, top + 1, x + width - 1, top + POPUP_ROW_HEIGHT - 1, 0x1F55ff55);
                graphics.fill(x + 1, top + 1, x + 3, top + POPUP_ROW_HEIGHT - 1, 0xFF55ff55);
            } else if (hover) {
                // 如果是悬浮条目
                graphics.fill(x + 1, top + 1, x + width - 1, top + POPUP_ROW_HEIGHT - 1, 0xFF3C3C3C);
            }

            int textColor = entry.selected() ? 0xFF55ff55 : hover ? 0xFFF3EFE0 : 0xFF989898;
            this.drawPopupText(graphics, entry.label().getString(), x + 12, top + 4, width - 28, textColor);

            // 音色行右缘的试听热区：选音色靠耳朵。
            // 一次只允许一个在途请求，所以别的行在等待期间必须**看起来**就是点不动的——
            // 画成正常的 ▶ 却默默吞掉点击，是最难受的那种失灵。
            if (this.openPopup == PopupType.TTS && this.isPreviewableEntry(entry)) {
                boolean pending = VoicePreviewClient.isPendingFor(this.previewSiteOf(entry), this.previewModelOf(entry));
                boolean blocked = !pending && VoicePreviewClient.isBusy();
                String glyph = pending ? "…" : "▶";
                int glyphColor = pending ? 0xFFFFAA00
                        : blocked ? 0xFF3A3A3A
                        : (hover ? 0xFFF3EFE0 : 0xFF6A6A6A);
                graphics.text(this.font, glyph, x + width - 14, top + 4, glyphColor, false);
            }
        }

        // 渲染滚动条
        if (entries.size() > visible) {
            int barX = x + width - 4;
            int thumbHeight = Math.max(10, visible * visible * POPUP_ROW_HEIGHT / entries.size());
            int scrollRange = Math.max(1, entries.size() - visible);
            int thumbOffset = (height - thumbHeight) * this.popupScrollOffset / scrollRange;
            graphics.fill(barX - 1, y + thumbOffset + 1, barX + 2, y + thumbOffset + thumbHeight - 1, 0xFF55ff55);
        }
    }

    private void drawPopupText(GuiGraphicsExtractor graphics, String text, int x, int y, int maxWidth, int color) {
        graphics.text(this.font, this.trimToWidth(text, maxWidth), x, y, color, false);
    }

    private boolean tryClickPopup(double mouseX, double mouseY) {
        if (this.popupGeometry == null || !this.popupGeometry.contains(mouseX, mouseY) || this.popupGeometry.entries().isEmpty()) {
            return false;
        }

        List<PopupEntry> entries = this.popupGeometry.entries();

        int x = this.popupGeometry.x();
        int y = this.popupGeometry.y();
        int w = this.popupGeometry.width();
        int visibleCount = this.popupGeometry.visibleCount();

        // 如果点击的是右侧滚动条，跳过
        if (entries.size() > visibleCount) {
            int scrollbarLeft = x + w - 6;
            if (mouseX >= scrollbarLeft) {
                return true;
            }
        }

        // 如果点击的索引超出范围，跳过
        int index = this.popupScrollOffset + (int) ((mouseY - y) / POPUP_ROW_HEIGHT);
        if (index < 0 || index >= entries.size()) {
            return true;
        }

        // 执行点击应用
        // 音色行右缘 18px 是试听热区——点它出声，不改选择
        PopupEntry entry = entries.get(index);
        if (!entry.header()) {
            if (this.openPopup == PopupType.TTS && this.isPreviewableEntry(entry) && mouseX >= x + w - 18) {
                VoicePreviewClient.request(this.previewSiteOf(entry), this.previewModelOf(entry),
                        VoicePreviewClient.Origin.CHAT);
                return true;
            }
            this.applyPopupSelection(this.openPopup, entry);
        }
        return true;
    }

    @Nullable
    private PopupGeometry getPopupGeometry(@Nullable PopupType type) {
        if (type == null) {
            return null;
        }

        FlatColorButton anchor = this.getPopupAnchor(type);
        List<PopupEntry> entries = this.getPopupEntries(type);
        if (anchor == null || entries.isEmpty()) {
            return null;
        }

        int popupMaxVisible = this.getPopupMaxVisible();
        int visible = Math.min(popupMaxVisible, entries.size());

        int width = this.getPopupWidth(type, anchor, entries);
        int height = visible * POPUP_ROW_HEIGHT;

        int x = anchor.getX() + anchor.getWidth() - width;
        int y = anchor.getY() - height - 4;

        // 防止超出屏幕边界
        if (x < 8) {
            x = 8;
        }
        if (x + width > this.width - 8) {
            x = this.width - 8 - width;
        }
        if (y < 8) {
            y = 8;
        }
        return new PopupGeometry(x, y, width, visible, entries);
    }

    private int getPopupWidth(PopupType type, FlatColorButton anchor, List<PopupEntry> entries) {
        int contentWidth = anchor.getWidth();
        for (PopupEntry entry : entries) {
            int rowWidth = this.font.width(entry.label());
            rowWidth += entry.header() ? 12 : 26;
            contentWidth = Math.max(contentWidth, rowWidth);
        }
        int minWidth = type == PopupType.LANGUAGE ? 96 : 150;
        int maxWidth = type == PopupType.LANGUAGE ? 152 : 220;
        return Math.max(minWidth, Math.min(maxWidth, contentWidth));
    }

    private boolean recallHistory(int direction) {
        List<String> history = this.getSentInputHistory();
        if (history.isEmpty()) {
            return false;
        }

        if (this.inputHistoryIndex < 0 || this.inputHistoryIndex > history.size()) {
            this.inputHistoryIndex = 0;
        }

        // 上翻历史
        if (direction < 0) {
            if (this.inputHistoryIndex < history.size() - 1) {
                this.inputHistoryIndex++;
                this.input.setValue(history.get(this.inputHistoryIndex));
                this.input.moveCursorToEnd(false);
                return true;
            }
            return false;
        }

        // 下翻历史
        if (0 < this.inputHistoryIndex) {
            this.inputHistoryIndex--;
            this.input.setValue(history.get(this.inputHistoryIndex));
            this.input.moveCursorToEnd(false);
            return true;
        }
        return false;
    }

    private List<String> getSentInputHistory() {
        // 第 0 个为当前输入项
        List<String> output = Lists.newArrayList(StringUtils.EMPTY);
        output.addAll(this.manager.getHistory()
                .getDeque().stream()
                .filter(msg -> msg.role() == Role.USER)
                .map(LLMMessage::message)
                .toList());
        return output;
    }

    public EntityMaid getMaid() {
        return maid;
    }

    /**
     * 下拉框的范围信息，包括位置、宽度和可见条目数量；
     * <p>
     * 提供一个方法用于渲染、或判断鼠标是否在范围内。
     */
    private record PopupGeometry(int x, int y, int width, int visibleCount, List<PopupEntry> entries) {
        private int height() {
            return this.visibleCount * POPUP_ROW_HEIGHT;
        }

        private int totalCount() {
            return this.entries.size();
        }

        private boolean contains(double mouseX, double mouseY) {
            return this.x <= mouseX && mouseX < this.x + this.width
                   && this.y <= mouseY && mouseY < this.y + this.height();
        }
    }

    /**
     * 用于渲染的下拉框条目
     */
    private record PopupEntry(Component label, boolean header, boolean selected,
                              String site, String model, String language) {
    }

    /**
     * 下拉框类型
     */
    private enum PopupType {
        LLM,
        TTS,
        LANGUAGE
    }
}
