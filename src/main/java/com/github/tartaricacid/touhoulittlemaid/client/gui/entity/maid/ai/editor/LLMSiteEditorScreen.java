package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor;

import cn.sh1rocu.touhoulittlemaid.mixin.accessor.ScreenAccessor;
import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.LLMSite;
import com.github.tartaricacid.touhoulittlemaid.ai.service.llm.openai.LLMOpenAISite;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings.AIChatSettingsHubScreen;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings.AIChatSettingsLLMSiteScreen;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.FlatColorButton;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.SaveLLMSitePacket;
import com.github.tartaricacid.touhoulittlemaid.util.Rectangle;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.CheckSiteConfigPackage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;

import static com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.Translations.*;
import static net.minecraft.network.chat.CommonComponents.GUI_BACK;

public class LLMSiteEditorScreen extends Screen implements SiteCheckResultDisplay {
    private static final int LABEL_COLOR = 0xFF777777;

    private static final int BASE_WIDTH = SiteEditorLayout.PANEL_WIDTH;
    private static final int BASE_HEIGHT = 230;
    /**
     * 判词占住按钮的时长。比保存校验那 2 秒长：判词只有一个词，完整原因在悬停提示里，
     * 得留够时间让人把鼠标移上去读。**暂定值**，觉得别扭就调。
     */
    private static final long CHECK_RESULT_MS = 8000;

    private final AIChatSettingsLLMSiteScreen parent;
    private final LLMSite sourceSite;
    private final String siteDisplayName;

    /**
     * 是否是新增站点的状态，此状态可以自定义 site id
     */
    private final boolean createMode;
    /**
     * 是否能够设置模型的 “深度思考” 属性，部分国内 api 目前还不支持新版 openai api 格式
     */
    private final boolean supportsReasoning;
    /**
     * 模型列表
     */
    private final List<ModelRow> modelRows = Lists.newArrayList();

    private int startX;
    private int startY;

    private EditBox siteIdInput;
    private EditBox urlInput;
    private EditBox secretInput;
    /** 服务端说密钥已配好，但没把明文发下来 */
    private boolean secretAlreadySet;
    /** 管理员点过「清除」，与「没碰这个框」必须区分 */
    private boolean secretCleared;

    /**
     * 模型列表框
     */
    private int modelScrollOffset;
    private Rectangle modelArea;

    /**
     * 保存时的提示信息
     */
    private long tipTimestamp = -1;
    private Component statusMessage = Component.empty();

    /**
     * 「检查配置」的服务端回执：按钮自己临时变成状态灯，版面里不塞整句话。
     *
     * <p>状态存在字段里而不是直接写进按钮，是因为 {@link #init()} 会重建全部控件
     * （缩放窗口、增删模型行都会触发）。存字段才能让状态活过重建。</p>
     */
    private @Nullable Component checkVerdict;
    private @Nullable Component checkDetail;
    private int checkResultColor = 0xFFFFFFFF;
    private long checkResultTimestamp = -1;
    private @Nullable FlatColorButton checkButton;

    public LLMSiteEditorScreen(AIChatSettingsLLMSiteScreen parent, LLMSite sourceSite, boolean createMode, boolean supportsReasoning) {
        super(Component.literal("LLM OpenAI Site Editor"));

        this.parent = parent;
        this.sourceSite = sourceSite;
        this.createMode = createMode;
        this.supportsReasoning = supportsReasoning;

        String nameKey = sourceSite.getNameKey();
        this.siteDisplayName = I18n.exists(nameKey) ? I18n.get(nameKey) : sourceSite.id();

        if (!this.createMode && this.sourceSite instanceof LLMOpenAISite site) {
            // 如果非创建模式，那么需要预先填充 models 字段
            site.modelEntries().forEach((id, entry) -> {
                ModelRow modelRow = new ModelRow(entry.name(), entry.isReasoning());
                this.modelRows.add(modelRow);
            });
        }
    }

    public LLMSiteEditorScreen(AIChatSettingsLLMSiteScreen parent, LLMSite sourceSite, boolean createMode) {
        this(parent, sourceSite, createMode, false);
    }

    @Override
    protected void init() {
        // 输入框数值读取，这样在改变窗口时，数值不会丢失
        String siteIdValue = this.getEditBoxInitValue(this.siteIdInput, this.sourceSite.id());
        String urlValue = this.getEditBoxInitValue(this.urlInput, this.sourceSite.url());
        // 服务端下行的密钥是哨兵而不是明文，框里必须从空开始——既不能显示那串内部标记，
        // 也不能用星号占位（个数会泄漏长度，还会让人以为能就地编辑）。
        String incomingSecret = this.sourceSite instanceof LLMOpenAISite site ? site.secretKey() : StringUtils.EMPTY;
        this.secretAlreadySet = Site.SECRET_KEPT.equals(incomingSecret);
        String secretValue = this.getEditBoxInitValue(this.secretInput,
                this.secretAlreadySet ? StringUtils.EMPTY : incomingSecret);

        this.clearWidgets();
        this.startX = (this.width - BASE_WIDTH) / 2;
        this.startY = (this.height - BASE_HEIGHT) / 2;

        int left = this.startX + SiteEditorLayout.MARGIN;
        int contentWidth = SiteEditorLayout.CONTENT_WIDTH;

        // 站点 ID，仅在新建模式下可修改
        this.siteIdInput = this.addInput(left, this.startY + 30, 124, SITE_ID_NAME, siteIdValue);
        this.siteIdInput.active = this.createMode;

        // URL
        this.urlInput = this.addInput(left + 132, this.startY + 30, contentWidth - 132, URL_NAME, urlValue);

        // 秘钥，隐藏显示。要显示「清除」时必须让出它的位置：输入框比按钮先注册，
        // 铺满整行会把重叠带里的点击抢走，按钮下半截按不动。
        boolean showsClear = this.secretAlreadySet && !this.secretCleared;
        int secretWidth = showsClear ? SiteEditorLayout.SECRET_WIDTH_WITH_CLEAR : contentWidth;
        this.secretInput = this.addInput(left, this.startY + 65, secretWidth, SECRET_KEY_NAME, secretValue);
        // 将秘钥输入框的字符显示为 ·，但末尾两个字符正常显示
        this.secretInput.addFormatter((text, pos) -> FormattedCharSequence.forward("·".repeat(text.length()), Style.EMPTY));

        // 模型列表
        this.modelArea = new Rectangle(left, this.startY + 104, contentWidth, BASE_HEIGHT - 103 - 34);
        this.createRows();

        // 底部按钮
        int bottomY = this.startY + BASE_HEIGHT - 24;

        // 「清除」必须是一个显式动作：框里留空表示「不改」，两者不能用同一种操作表达，
        // 否则管理员永远删不掉一个已配好的密钥。
        if (showsClear) {
            // y/高度对齐密钥框的可见底衬（renderInputField 画在 startY+67，高 19）
            this.addRenderableWidget(new FlatColorButton(this.startX + SiteEditorLayout.SECRET_CLEAR_X,
                    this.startY + 67, SiteEditorLayout.SECRET_CLEAR_WIDTH, 19,
                    Component.translatable("ai.touhou_little_maid.chat.settings.hub.secret_clear"), b -> {
                this.secretCleared = true;
                this.secretInput.setValue(StringUtils.EMPTY);
                this.init();
            }));
        }

        this.addRenderableWidget(new FlatColorButton(this.startX + SiteEditorLayout.ADD_MODEL_X, bottomY,
                SiteEditorLayout.ADD_MODEL_WIDTH, 20, ADD_MODEL_NAME, b -> {
            this.modelRows.add(new ModelRow(StringUtils.EMPTY, false));
            int visibleCount = this.getVisibleModelCount();
            this.modelScrollOffset = Math.max(0, this.modelRows.size() - visibleCount);
            this.init();
        }));

        // 「检查配置」而不是「测试连接」：它检查地址与密钥填没填、主机连不连得上，
        // **不验证密钥是否正确**。叫成后者就是一个说谎的标签，而它仍然有用——
        // 把「地址/网络不通」与「密钥不对」分开，这两种故障的处置完全不同。
        this.checkButton = this.addRenderableWidget(new FlatColorButton(
                this.startX + SiteEditorLayout.CHECK_CONFIG_X, bottomY,
                SiteEditorLayout.CHECK_CONFIG_WIDTH, 20, CHECK_CONFIG_NAME,
                b -> ClientPlayNetworking.send(new CheckSiteConfigPackage(
                        CheckSiteConfigPackage.LLM, this.sourceSite.id()))));
        this.applyCheckVerdict();
        this.addRenderableWidget(new FlatColorButton(this.startX + SiteEditorLayout.SAVE_X, bottomY,
                SiteEditorLayout.SAVE_WIDTH, 20, SAVE_NAME, b -> this.saveSite()));
        this.addRenderableWidget(new FlatColorButton(this.startX + SiteEditorLayout.BACK_X, bottomY,
                SiteEditorLayout.BACK_WIDTH, 20, GUI_BACK, b -> this.onClose()));
    }

    private String getEditBoxInitValue(@Nullable EditBox editBox, String initValue) {
        if (editBox != null) {
            return editBox.getValue();
        }
        if (this.createMode) {
            return StringUtils.EMPTY;
        }
        return initValue;
    }

    private EditBox addInput(int x, int y, int width, Component title, String value) {
        EditBox box = new EditBox(this.font, x + 6, y + 8, width - 12, 16, title);
        box.setMaxLength(512);
        box.setBordered(false);
        box.setValue(value);
        this.addWidget(box);
        return box;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xc0101010, 0xc0101010);

        // 居中标题
        graphics.drawCenteredString(this.font, llmEditorTitle(this.siteDisplayName),
                this.startX + BASE_WIDTH / 2, this.startY + 4, 0xFFF3EFE0);

        // 判词到期即还原，靠每帧这一次调用，不另设计时器
        this.applyCheckVerdict();

        this.renderInputField(graphics, this.siteIdInput, mouseX, mouseY, partialTick);
        this.renderInputField(graphics, this.urlInput, mouseX, mouseY, partialTick);
        this.renderInputField(graphics, this.secretInput, mouseX, mouseY, partialTick);
        this.renderSecretPlaceholder(graphics);

        this.renderModelArea(graphics, mouseX, mouseY, partialTick);

        for (Renderable renderable : ((ScreenAccessor) this).tlm$getRenderables()) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }

        // 提示显示 2 秒
        if (System.currentTimeMillis() - this.tipTimestamp < 2000) {
            int x = this.startX + BASE_WIDTH - 155;
            int y = this.startY + BASE_HEIGHT - 35;
            graphics.drawCenteredString(this.font, this.statusMessage, x, y, 0xFFFF7777);
        }

        if (this.checkButton != null) {
            this.checkButton.renderToolTip(graphics, this, mouseX, mouseY);
        }
    }

    @Override
    public void showSiteCheckResult(Component verdict, Component detail, int argb) {
        this.checkVerdict = verdict;
        this.checkDetail = detail;
        this.checkResultColor = argb;
        this.checkResultTimestamp = System.currentTimeMillis();
        this.applyCheckVerdict();
    }

    /**
     * 把当前判词贴到按钮上；已过期或从未检查过则还原成常态标签。
     *
     * <p>每帧调一次（外加 {@link #init()} 重建后一次），因此还原不需要额外的计时器。</p>
     */
    private void applyCheckVerdict() {
        if (this.checkButton == null) {
            return;
        }
        boolean live = this.checkVerdict != null
                && System.currentTimeMillis() - this.checkResultTimestamp < CHECK_RESULT_MS;
        if (live) {
            this.checkButton.setMessage(this.checkVerdict);
            this.checkButton.setMessageColor(this.checkResultColor);
            this.checkButton.setTooltips(List.of(this.checkDetail));
        } else {
            this.checkButton.setMessage(CHECK_CONFIG_NAME);
            this.checkButton.setMessageColor(0);
            this.checkButton.clearTooltips();
        }
    }

    /**
     * 密钥框空着时用灰字说明它是「已配置」还是「未配置」。
     *
     * <p>下行只有哨兵、框里一律是空的，不给这行字管理员就分不出服务端到底有没有密钥。</p>
     */
    private void renderSecretPlaceholder(GuiGraphics graphics) {
        if (this.secretInput == null || !this.secretInput.getValue().isEmpty()) {
            return;
        }
        String key = this.secretCleared ? "secret_cleared" : (this.secretAlreadySet ? "secret_configured" : "secret_unset");
        graphics.drawString(this.font,
                Component.translatable("ai.touhou_little_maid.chat.settings.hub." + key),
                this.secretInput.getX(), this.secretInput.getY(), 0xFF808080, false);
    }

    private void renderInputField(GuiGraphics graphics, EditBox box, int mouseX, int mouseY, float partialTick) {
        int x = box.getX() - 6;
        int y = box.getY() - 6;
        int width = box.getInnerWidth() + 12;
        int height = box.getHeight() + 3;

        graphics.drawString(this.font, box.getMessage(), x + 2, y - 10, LABEL_COLOR, false);
        graphics.fill(x, y, x + width, y + height, 0xAA111111);
        box.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderModelArea(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = (int) this.modelArea.x;
        int top = (int) this.modelArea.y;

        // 标题
        graphics.drawString(this.font, MODELS_NAME, left + 2, top - 12, LABEL_COLOR);

        // 确定滚动起始值
        int visibleCount = this.getVisibleModelCount();
        int maxOffset = Math.max(0, this.modelRows.size() - visibleCount);
        if (this.modelScrollOffset > maxOffset) {
            this.modelScrollOffset = maxOffset;
        }

        // 渲染模型列表
        graphics.enableScissor(left, top - 2, (int) this.modelArea.right(), (int) this.modelArea.bottom() + 2);
        int startIndex = this.modelScrollOffset;
        int endIndex = Math.min(this.modelRows.size(), startIndex + visibleCount);
        for (int i = startIndex; i < endIndex; i++) {
            ModelRow row = this.modelRows.get(i);
            int rowY = top + 2 + (i - startIndex) * 22;
            if (row.nameBox != null) {
                int rowLeft = (int) this.modelArea.x;
                graphics.fill(rowLeft, rowY - 4, rowLeft + row.nameBox.getInnerWidth(), rowY + 20 - 4, 0xAA111111);
                row.nameBox.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        graphics.disableScissor();

        // 渲染滚动条：仅当模型总数超过可见区域能容纳的数量时才显示
        if (this.modelRows.size() > visibleCount) {
            // 滚动条轨道的起始 Y 坐标
            int trackTop = top - 1;
            // 滚动条轨道的总高度
            int trackHeight = (int) this.modelArea.h - 9;
            // 滑块高度：按可见行数占总行数的比例缩放，但最小不低于 12px，防止滑块太小难以点击
            int thumbHeight = Math.max(12, visibleCount * trackHeight / this.modelRows.size());
            // 可滚动的最大偏移量（总行数 - 可见行数），至少为 1 防止除零
            int scrollRange = Math.max(1, this.modelRows.size() - visibleCount);
            // 根据当前滚动偏移量，按比例计算滑块在轨道中的 Y 偏移
            int thumbOffset = (trackHeight - thumbHeight) * this.modelScrollOffset / scrollRange;
            // 在模型区域的右侧绘制一个 2px 宽的绿色滑块
            graphics.fill((int) this.modelArea.right() - 4,
                    trackTop + thumbOffset,
                    (int) this.modelArea.right() - 2,
                    trackTop + thumbOffset + thumbHeight,
                    0xFF55FF55);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalDelta, double verticalDelta) {
        if (this.modelArea.contains(mouseX, mouseY)) {
            int visibleCount = this.getVisibleModelCount();
            int maxOffset = Math.max(0, this.modelRows.size() - visibleCount);
            if (verticalDelta < 0 && this.modelScrollOffset < maxOffset) {
                this.modelScrollOffset++;
                this.init();
                return true;
            }
            if (verticalDelta > 0 && this.modelScrollOffset > 0) {
                this.modelScrollOffset--;
                this.init();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalDelta, verticalDelta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public AIChatSettingsHubScreen getParentHub() {
        return this.parent;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    private void saveSite() {
        LLMSite site = this.buildSite();
        if (site == null) {
            return;
        }
        SaveLLMSitePacket message = this.createMode ? SaveLLMSitePacket.create(site) : SaveLLMSitePacket.update(site);
        ClientPlayNetworking.send(message);
    }

    private LLMSite buildSite() {
        if (!(this.sourceSite instanceof LLMOpenAISite site)) {
            return null;
        }

        String siteId = StringUtils.trim(this.siteIdInput.getValue());
        if (StringUtils.isBlank(siteId)) {
            this.showStatus(SITE_ID_IS_EMPTY);
            return null;
        }
        if (this.createMode && this.parent.hasLLMSite(siteId)) {
            this.showStatus(SITE_ID_ALREADY_EXISTS);
            return null;
        }

        String url = StringUtils.trim(this.urlInput.getValue());
        if (StringUtils.isBlank(url)) {
            this.showStatus(URL_IS_EMPTY);
            return null;
        }

        List<ModelRow> rows = this.modelRows;
        boolean hasModel = rows.stream().anyMatch(row -> StringUtils.isNotBlank(row.name()));
        if (this.createMode && !hasModel) {
            this.showStatus(MODEL_IS_EMPTY);
            return null;
        }

        // 秘钥可以为空（部分本地模型没有秘钥）。
        // 但「已配置且没动过」必须回传哨兵，否则服务端会把它当成一次清空——
        // 那正是「只改了 URL 却让 LLM 失效」那条回归。
        String secretKey = this.secretInput.getValue();
        if (this.secretAlreadySet && !this.secretCleared && secretKey.isEmpty()) {
            secretKey = Site.SECRET_KEPT;
        }

        // 普通 OpenAI 模型
        List<LLMOpenAISite.ModelEntry> models = Lists.newArrayList();
        HashSet<String> seen = Sets.newHashSet();
        for (ModelRow row : rows) {
            String modelName = row.name();
            if (StringUtils.isBlank(modelName) || !seen.add(modelName)) {
                continue;
            }
            models.add(new LLMOpenAISite.ModelEntry(modelName, row.reasoning()));
        }
        return new LLMOpenAISite(siteId, site.icon(), url, site.enabled(), secretKey, site.headers(), models);
    }


    /**
     * 创建模型列表
     */
    private void createRows() {
        int visibleCount = this.getVisibleModelCount();
        int maxOffset = Math.max(0, this.modelRows.size() - visibleCount);
        if (this.modelScrollOffset > maxOffset) {
            this.modelScrollOffset = maxOffset;
        }

        int startIndex = this.modelScrollOffset;
        int endIndex = Math.min(this.modelRows.size(), startIndex + visibleCount);

        int left = (int) this.modelArea.x;
        int top = (int) this.modelArea.y;
        int right = (int) this.modelArea.right();

        for (int i = startIndex; i < endIndex; i++) {
            int rowY = top + 3 + (i - startIndex) * 22;
            int inputWidth = this.supportsReasoning ? BASE_WIDTH - 100 : BASE_WIDTH - 38;
            ModelRow row = this.modelRows.get(i);

            String preValue = row.name();
            row.nameBox = new EditBox(this.font, left + 6, rowY + 2, inputWidth - 12, 16, Component.empty());
            row.nameBox.setMaxLength(512);
            row.nameBox.setBordered(false);
            row.nameBox.setValue(preValue);
            this.addWidget(row.nameBox);

            if (this.supportsReasoning) {
                String key = row.reasoning ? "ai.touhou_little_maid.chat.settings.hub.model.reasoning" : "ai.touhou_little_maid.chat.settings.hub.model.normal";
                row.toggleButton = this.addRenderableWidget(new FlatColorButton(right - 86, rowY - 4, 60, 18, Component.translatable(key), b -> {
                    row.reasoning = !row.reasoning;
                    this.init();
                }));
            } else {
                row.toggleButton = null;
            }

            final int index = i;
            row.deleteButton = this.addRenderableWidget(new FlatColorButton(right - 24, rowY - 4, 18, 18, Component.literal("✕"), b -> {
                this.modelRows.remove(index);
                this.init();
            }));
        }
    }

    private int getVisibleModelCount() {
        return (int) Math.max(1, (this.modelArea.h - 4) / 22);
    }

    private void showStatus(Component message) {
        this.statusMessage = message;
        this.tipTimestamp = System.currentTimeMillis();
    }

    private static final class ModelRow {
        /**
         * nameBox 创建前的初始值缓冲，一旦 nameBox 存在就不再使用
         */
        private final String initialName;

        private boolean reasoning;
        private EditBox nameBox;
        private FlatColorButton toggleButton;
        private FlatColorButton deleteButton;

        private ModelRow(String name, boolean reasoning) {
            this.initialName = name;
            this.reasoning = reasoning;
        }

        private String name() {
            return this.nameBox != null ? this.nameBox.getValue() : this.initialName;
        }

        private boolean reasoning() {
            return this.reasoning;
        }
    }
}
