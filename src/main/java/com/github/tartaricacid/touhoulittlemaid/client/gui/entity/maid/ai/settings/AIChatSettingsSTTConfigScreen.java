package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings;

import com.github.tartaricacid.touhoulittlemaid.ai.manager.site.AvailableSites;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTApiType;
import com.github.tartaricacid.touhoulittlemaid.ai.service.stt.STTSite;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor.STTSiteEditorScreen;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.ai.MaidChatDistanceSlider;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.FlatColorButton;
import com.github.tartaricacid.touhoulittlemaid.client.sound.record.MicrophoneManager;
import com.github.tartaricacid.touhoulittlemaid.config.AiClientConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.ScreenUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.Translations.SAVE_NAME;

/**
 * 「语音输入设置」标签页：总开关、来源列表、麦克风选择、识别距离、代理地址。
 *
 * <p>纵向预算（内容区总高约 197px）：开关行 → 来源标题 + 四行来源 → 麦克风 → 距离 → 代理，
 * 全部按 hub 的行式布局排。曾经的排法把代理输入框顶到页脚背后直接看不见。</p>
 *
 * <p>⚠️ 本页的配置全部落在**个人 AI 配置**（{@code touhou_little_maid-ai.toml}，
 * 由 {@link AiClientConfig} 管），不是世界规则——所以保存要做两件事，见 {@link #saveChanges()}。</p>
 */
public class AIChatSettingsSTTConfigScreen extends AIChatSettingsHubScreen {
    private static final int SOURCE_ROW_PITCH = 20;
    private static final int SOURCE_ROW_HEIGHT = 18;

    private String[] microphoneNames = new String[0];

    private MaidChatDistanceSlider distanceSlider;
    private EditBox proxyInput;
    private FlatColorButton saveButton;

    public AIChatSettingsSTTConfigScreen(@Nullable Screen parent, SharedState state, boolean insufficientPermissions) {
        super(parent, state, insufficientPermissions);
    }

    @Override
    protected Type getType() {
        return Type.STT_CONFIG;
    }

    @Override
    protected void init() {
        this.syncInputsToState();
        super.init();
    }

    @Override
    protected void initContent() {
        int x = this.getContentX();
        int width = this.getContentWidth();
        int y = this.getContentY() + 2;

        this.addToggleRow(x, y, width,
                Component.translatable("config.touhou_little_maid.global_ai.stt_enable"),
                this.state.sttEnabled, () -> {
                    this.state.sttEnabled = !this.state.sttEnabled;
                    this.init();
                }).setTooltip(Tooltip.create(
                Component.translatable("config.touhou_little_maid.global_ai.stt_enable.tooltip")));

        // 来源列表：一行一档 + 状态 + 齿轮进第二层。
        //
        // 原先是一个循环按钮，问题不只是点起来麻烦——**所有档位的状态都看不见**：
        // 哪一家配好了密钥、哪一家没有，玩家只能选中之后按下说话键才知道。
        // 而每家的密钥字段形态还不一样（腾讯要两个），所以密钥根本没法平铺在这一屏，
        // 只能藏在每行自己的第二层里，那正是齿轮的位置。
        this.addRowLabel(y + 20, Component.translatable("config.touhou_little_maid.global_ai.stt_type"),
                Component.translatable("config.touhou_little_maid.global_ai.stt_type.tooltip"));
        int rowY = y + 38;
        for (STTApiType type : STTApiType.values()) {
            this.addSourceRow(type, x, rowY, width);
            rowY += SOURCE_ROW_PITCH;
        }

        int microphoneY = rowY + 4;
        this.addRowLabel(microphoneY, Component.translatable("ai.touhou_little_maid.chat.settings.hub.row.microphone"),
                Component.translatable("config.touhou_little_maid.global_ai.stt_microphone.tooltip"));
        Component microphoneName = Component.literal(this.normalizeMicrophone(this.state.sttMicrophone));
        this.addRenderableWidget(new FlatColorButton(this.rowControlX(), microphoneY, this.rowControlWidth(), 20,
                microphoneName, b -> this.openMicrophonePicker(microphoneY)));

        int distanceY = microphoneY + ROW_PITCH;
        this.addRowLabel(distanceY, Component.translatable("ai.touhou_little_maid.chat.settings.hub.row.distance"),
                Component.translatable("config.touhou_little_maid.global_ai.maid_can_chat_distance.tooltip"));
        this.distanceSlider = new MaidChatDistanceSlider(this.rowControlX(), distanceY,
                this.rowControlWidth(), 20, this.state);
        this.addRenderableWidget(this.distanceSlider);

        this.proxyInput = this.addRuleInput(distanceY + ROW_PITCH,
                Component.translatable("ai.touhou_little_maid.chat.settings.hub.row.proxy"),
                Component.translatable("config.touhou_little_maid.global_ai.stt_proxy_address.tooltip"),
                null, this.state.sttProxyAddress);
    }

    /** 麦克风弹出列表：设备多的机器同样受够了转盘式循环 */
    private void openMicrophonePicker(int anchorY) {
        if (this.microphoneNames.length == 0) {
            return;
        }
        String current = this.normalizeMicrophone(this.state.sttMicrophone);
        List<HubPopupEntry> entries = new ArrayList<>();
        for (String name : this.microphoneNames) {
            entries.add(new HubPopupEntry(Component.literal(name), name.equals(current), () -> {
                this.state.sttMicrophone = name;
                this.init();
            }));
        }
        this.openHubPopup(this.rowControlX(), anchorY, this.rowControlWidth(), entries);
    }

    /**
     * 一行来源：选中标记 + 名字（左）+ 状态（右）+ 齿轮。
     *
     * <p>名字与状态自绘——按钮的居中文案和右缘状态字曾直接撞在一起。
     * <b>点行只选中，编辑只走齿轮</b>——「未配置的行点击顺便进编辑」被用户实测否掉：
     * 激活与进编辑绑在一起，选个来源会被莫名带进表单。未配置状态右侧灰字已经可见。</p>
     */
    private void addSourceRow(STTApiType type, int x, int y, int width) {
        boolean selected = this.state.sttType == type;
        STTSite site = this.state.sttSites.get(type.getName());
        boolean configured = site != null && site.hasUsableCredentials();

        int rowWidth = width - 24;
        this.addRenderableWidget(new FlatColorButton(x, y, rowWidth, SOURCE_ROW_HEIGHT, Component.empty(), b -> {
            this.state.sttType = type;
            this.init();
        }));
        this.addRenderableWidget(new FlatColorButton(x + width - 22, y, 22, SOURCE_ROW_HEIGHT, Component.literal("⚙"), b -> {
            this.state.sttType = type;
            if (site != null) {
                this.openSourceConfig(site);
            }
        }));

        Component name = Component.translatable("ai.touhou_little_maid.chat.site.%s.name".formatted(type.getName()));
        Component status = Component.translatable(configured
                ? "ai.touhou_little_maid.chat.settings.hub.source.configured"
                : "ai.touhou_little_maid.chat.settings.hub.source.unset");
        int nameColor = selected ? 0xFFFFFFFF : 0xFFAAAAAA;
        int statusColor = configured ? 0xFF55FF55 : 0xFF999999;
        this.addRenderableOnly((Renderable) (graphics, mouseX, mouseY, partialTick) -> {
            if (selected) {
                graphics.fill(x + 1, y + 1, x + 3, y + SOURCE_ROW_HEIGHT - 1, 0xFF55FF55);
            }
            graphics.text(this.font, name, x + 8, y + 5, nameColor, false);
            int statusX = x + rowWidth - 6 - this.font.width(status);
            graphics.text(this.font, status, statusX, y + 5, statusColor, false);
        });
    }

    /**
     * 进第二层：**直接打开那一档自己的表单**，返回/保存都回到本屏。
     *
     * <p>这也是本地站点编辑的**唯一入口**——它原先是侧栏上与「语音输入设置」并列的一个标签
     * （{@code AIChatSettingsSTTSiteScreen}，本刀已删）；那一层的内容与本屏四行完全重复，
     * 而它的返回键还会把整个 UI 关掉。层级就两层：来源行 → 表单。</p>
     */
    private void openSourceConfig(STTSite site) {
        ScreenUtil.setScreen(new STTSiteEditorScreen(this, site, this::saveLocalSTTSite));
    }

    /** 表单保存回调：本地站点全量落盘（STT 站点从不与服务端同步） */
    public void saveLocalSTTSite(STTSite site) {
        site.setEnabled(true);
        this.state.sttSites.put(site.id(), site);
        AvailableSites.STT_SITES.clear();
        AvailableSites.STT_SITES.putAll(this.state.sttSites);
        AvailableSites.saveSTTSitesOnly();
    }

    /**
     * 页脚与其它三页同构：保存 + 返回。曾多一颗「保存并退出」——被用户实测判多余
     * （保存了想走点返回就是），且它把「已保存」绿字的页脚空当也挤没了。
     */
    @Override
    protected void addFooterButtons() {
        super.addFooterButtons();
        this.saveButton = this.addRenderableWidget(new FlatColorButton(this.getContentX(), this.getFooterY(), 80, 20,
                SAVE_NAME, b -> this.saveChanges()));
    }

    /**
     * 本页的保存要做**两件事**：落盘本机 AI 配置，以及提交各页共享的规则暂存。
     *
     * <p>后半句原先漏了，而绿字「已保存」照发——于是「在文字模型页翻个开关 → 切到本页 → 保存」
     * 会告诉玩家保存成功，实际上那条规则一个字也没上服务器，退出即丢。
     * 本页是几页里唯一有本机配置的，最容易被当成「与服务器规则无关」，也就最容易漏。
     * 契约是 hub 写死的：<b>在任何一页点保存都提交攒着的全部改动</b>，
     * 并由 {@code HubSharedStagingContractTest} 按屏枚举钉着。</p>
     */
    private void saveChanges() {
        this.syncInputsToState();

        AIConfig.STT_ENABLED.set(this.state.sttEnabled);
        AIConfig.STT_TYPE.set(this.state.sttType);
        AIConfig.STT_MICROPHONE.set(this.state.sttMicrophone);
        AIConfig.MAID_CAN_CHAT_DISTANCE.set(this.state.maidCanChatDistance);
        AIConfig.STT_PROXY_ADDRESS.set(this.state.sttProxyAddress);

        // 这五项已拆到专属的 -ai.toml（AiClientConfig），别再动 CommonConfig 那份全局文件
        if (AiClientConfig.CONFIG != null) {
            AiClientConfig.CONFIG.save();
        }
        this.ruleSession().save();
        // 本机配置保存即生效；AI 规则那半在服务端也是保存即激活，故不带 reload 提示
        this.flashSaved();
        this.init();
    }

    @Override
    protected void persistTransientState() {
        this.syncInputsToState();
    }

    private void syncInputsToState() {
        this.microphoneNames = MicrophoneManager.getAllMicrophoneName();
        // 麦克风可能已被拔出，需要校验并回退到可用设备
        this.state.sttMicrophone = this.normalizeMicrophone(this.state.sttMicrophone);
        if (this.distanceSlider != null) {
            this.state.maidCanChatDistance = this.distanceSlider.getDistanceValue();
        }
        if (this.proxyInput != null) {
            this.state.sttProxyAddress = StringUtils.trimToEmpty(this.proxyInput.getValue());
        }
    }

    private String normalizeMicrophone(String current) {
        if (this.microphoneNames.length == 0) {
            return StringUtils.EMPTY;
        }
        for (String name : this.microphoneNames) {
            if (name.equals(current)) {
                return current;
            }
        }
        return this.microphoneNames[0];
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xc0101010, 0xc0101010);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        this.renderSavedFlash(graphics);
        // 本页也要显示 *：带着别页的未保存改动切过来时，玩家得看得出还有东西没提交
        if (this.saveButton != null) {
            this.saveButton.setMessage(this.ruleSession().isDirty() ? SAVE_NAME.copy().append("*") : SAVE_NAME);
        }
        // 弹出层必须画在最后：它要压在上面这些子类自绘物之上
        this.renderHubOverlays(graphics, mouseX, mouseY);
    }
}
