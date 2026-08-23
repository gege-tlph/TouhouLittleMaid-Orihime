package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor;

import com.github.tartaricacid.touhoulittlemaid.ai.service.tts.TTSSite;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.FormField;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.layout.FieldDescriptor;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.layout.TTSSiteFormLayout;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings.AIChatSettingsHubScreen;
import com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings.AIChatSettingsTTSSiteScreen;
import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.FlatColorButton;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.CheckSiteConfigPackage;
import com.github.tartaricacid.touhoulittlemaid.network.message.ai.SaveTTSSitePacket;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.I18nUtil;
import com.github.tartaricacid.touhoulittlemaid.util.Rectangle;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.ScreenUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.Translations.*;
import static net.minecraft.network.chat.CommonComponents.GUI_BACK;

/**
 * TTS 站点编辑器：固定字段区 + 可滚动模型列表。
 * 表单字段定义委托给 {@link TTSSiteFormLayout} 的各子类。
 */
public class TTSSiteEditorScreen extends Screen implements SiteCheckResultDisplay {
    private static final int LABEL_COLOR = 0xFF777777;
    private static final int BASE_WIDTH = SiteEditorLayout.PANEL_WIDTH;
    private static final int BASE_HEIGHT = 230;
    /** 与 {@link LLMSiteEditorScreen} 同理：留够时间让人移上去读悬停里的完整原因。**暂定值** */
    private static final long CHECK_RESULT_MS = 8000;
    private static final int FIELD_ROW_HEIGHT = 35;
    private static final int MODEL_ROW_HEIGHT = 22;

    private final AIChatSettingsTTSSiteScreen parent;
    private final TTSSiteFormLayout layout;
    private final String siteDisplayName;
    /** 站点 id：用于「检查配置」把请求指向服务端的哪一个站点 */
    private final String siteId;

    /**
     * 固定字段列表（不滚动）
     */
    private final List<FormField> fields = Lists.newArrayList();
    /**
     * 模型行列表（可滚动）
     */
    private final List<ModelRow> modelRows = Lists.newArrayList();

    private int startX;
    private int startY;

    /**
     * 模型列表滚动区域
     */
    private Rectangle modelArea;
    private int modelScrollOffset;

    /**
     * 保存时的提示信息
     */
    private long tipTimestamp = -1;
    private Component statusMessage = Component.empty();

    /**
     * 「检查配置」的服务端回执：按钮自己临时变成状态灯。存字段而不是直接写按钮，
     * 是因为 {@link #init()} 会重建全部控件，存字段才能让状态活过重建。
     */
    private @Nullable Component checkVerdict;
    private @Nullable Component checkDetail;
    private int checkResultColor = 0xFFFFFFFF;
    private long checkResultTimestamp = -1;
    private @Nullable FlatColorButton checkButton;

    public TTSSiteEditorScreen(AIChatSettingsTTSSiteScreen parent, TTSSite sourceSite) {
        super(Component.literal("TTS Site Editor"));
        this.parent = parent;
        this.layout = sourceSite.formLayout();

        String nameKey = sourceSite.getNameKey();
        this.siteDisplayName = I18nUtil.getOrDefault(nameKey, sourceSite.id());
        this.siteId = sourceSite.id();

        this.initStateFromLayout();
    }

    private void initStateFromLayout() {
        this.fields.clear();
        this.modelRows.clear();

        for (FieldDescriptor desc : this.layout.getFieldDescriptors()) {
            this.fields.add(new FormField(desc.label(), desc.value(), desc.editable(), desc.secret()));
        }

        if (this.layout.supportsModelRows()) {
            Map<String, String> initialModels = this.layout.getInitialModels();
            initialModels.forEach((id, name) -> this.modelRows.add(new ModelRow(id, name)));
        }
    }

    @Override
    @SuppressWarnings("all")
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T pWidget) {
        return super.addRenderableWidget(pWidget);
    }

    @Override
    protected void init() {
        // 在缩放窗口时，更新输入框的值
        this.fields.forEach(FormField::syncFromBox);
        this.modelRows.forEach(ModelRow::syncFromBox);

        this.clearWidgets();

        this.startX = (this.width - BASE_WIDTH) / 2;
        this.startY = (this.height - BASE_HEIGHT) / 2;

        int left = this.startX + SiteEditorLayout.MARGIN;
        int contentWidth = SiteEditorLayout.CONTENT_WIDTH;
        int bottomY = this.startY + BASE_HEIGHT - 24;

        // 固定字段区（不滚动）
        int fieldY = this.startY + 28;
        int fieldMaxSize = this.fields.size();
        // 奇数，那么最后一个占一整行，否则是均分，左右各一个
        boolean isOdd = fieldMaxSize % 2 == 1;
        for (int i = 0; i < fieldMaxSize; i++) {
            FormField field = this.fields.get(i);
            // 最后一行，奇数，占一整行
            if (isOdd && i == fieldMaxSize - 1) {
                // 偶数且在中间位置，跳过到下一行
                this.createFieldWidget(field, left, fieldY, contentWidth);
                fieldY += FIELD_ROW_HEIGHT;
            } else {
                boolean isLeft = i % 2 == 0;
                int fieldWidth = (contentWidth - 6) / 2;
                this.createFieldWidget(field, left + (isLeft ? 0 : fieldWidth + 6), fieldY, fieldWidth);
                if (!isLeft) {
                    fieldY += FIELD_ROW_HEIGHT;
                }
            }
        }

        // 额外组件
        fieldY += layout.extraInit(left, fieldY, contentWidth, this);

        // 可滚动模型区
        if (this.layout.supportsModelRows()) {
            int modelTop = fieldY + 14;
            int modelBottom = this.startY + BASE_HEIGHT - 48;
            this.modelArea = new Rectangle(left, modelTop, contentWidth, modelBottom - modelTop);
            this.createModelRows(left, contentWidth);

            this.addRenderableWidget(new FlatColorButton(this.startX + SiteEditorLayout.ADD_MODEL_X, bottomY,
                    SiteEditorLayout.ADD_MODEL_WIDTH, 20, ADD_MODEL_NAME, b -> {
                this.modelRows.add(new ModelRow(StringUtils.EMPTY, StringUtils.EMPTY));
                int visibleCount = this.getVisibleModelCount();
                this.modelScrollOffset = Math.max(0, this.modelRows.size() - visibleCount);
                this.init();
            }));
        }

        // 底部按钮
        // 「检查配置」而不是「测试连接」：它检查地址与密钥填没填、主机连不连得上，
        // **不验证密钥是否正确**。叫成后者就是一个说谎的标签，而它仍然有用——
        // 把「地址/网络不通」与「密钥不对」分开，这两种故障的处置完全不同。
        // 没有可配置项的站点（系统朗读器）不显示这两颗：检查必然停在「还没填地址」，
        // 保存写回的是一份逐字相同的副本。只能骗人的按钮不该出现，见 TTSSiteFormLayout#isConfigurable
        if (this.layout.isConfigurable()) {
            this.checkButton = this.addRenderableWidget(new FlatColorButton(
                    this.startX + SiteEditorLayout.CHECK_CONFIG_X, bottomY,
                    SiteEditorLayout.CHECK_CONFIG_WIDTH, 20, CHECK_CONFIG_NAME,
                    b -> ClientPlayNetworking.send(new CheckSiteConfigPackage(
                            CheckSiteConfigPackage.TTS, this.siteId))));
            this.applyCheckVerdict();
            this.addRenderableWidget(new FlatColorButton(this.startX + SiteEditorLayout.SAVE_X, bottomY,
                    SiteEditorLayout.SAVE_WIDTH, 20, SAVE_NAME, b -> this.saveSite()));
        } else {
            this.checkButton = null;
        }
        this.addRenderableWidget(new FlatColorButton(this.startX + SiteEditorLayout.BACK_X, bottomY,
                SiteEditorLayout.BACK_WIDTH, 20, GUI_BACK, b -> this.onClose()));
    }

    private void createFieldWidget(FormField field, int left, int y, int width) {
        EditBox box = new EditBox(this.font, left + 6, y + 14, width - 12, 16, field.i18nName());
        box.setMaxLength(512);
        box.setBordered(false);
        box.active = field.editable;
        box.setValue(field.value);
        if (field.secret) {
            box.addFormatter((text, pos) -> FormattedCharSequence.forward("·".repeat(text.length()), Style.EMPTY));
        }
        this.addWidget(box);
        field.box = box;
    }

    private void createModelRows(int left, int contentWidth) {
        int visibleCount = this.getVisibleModelCount();
        int maxOffset = Math.max(0, this.modelRows.size() - visibleCount);
        if (this.modelScrollOffset > maxOffset) {
            this.modelScrollOffset = maxOffset;
        }

        int top = (int) this.modelArea.y;
        int gap = 6;
        int inputWidth = contentWidth - 18 - gap;
        int idWidth = (inputWidth - gap) * 2 / 3;
        int nameWidth = inputWidth - gap - idWidth;

        int startIndex = this.modelScrollOffset;
        int endIndex = Math.min(this.modelRows.size(), startIndex + visibleCount);

        for (int i = startIndex; i < endIndex; i++) {
            int rowY = top + 2 + (i - startIndex) * MODEL_ROW_HEIGHT;
            ModelRow row = this.modelRows.get(i);

            // 先捕获值，再创建 box（避免返回新 box 的空值）
            String preId = row.id();
            String preName = row.name();

            row.idBox = new EditBox(this.font, left + 6, rowY + 2, idWidth, 16, Component.literal("Model Id"));
            row.idBox.setMaxLength(512);
            row.idBox.setBordered(false);
            row.idBox.setValue(preId);
            this.addWidget(row.idBox);

            row.nameBox = new EditBox(this.font, left + 16 + idWidth + gap, rowY + 2, nameWidth - 26, 16, Component.literal("Display Name"));
            row.nameBox.setMaxLength(512);
            row.nameBox.setBordered(false);
            row.nameBox.setValue(preName);
            this.addWidget(row.nameBox);

            final int index = i;
            this.addRenderableWidget(new FlatColorButton(left - 6 + inputWidth + gap, rowY - 4, 18, 18, Component.literal("✕"), b -> {
                this.modelRows.remove(index);
                this.init();
            }));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xc0101010, 0xc0101010);

        // 居中标题
        graphics.centeredText(this.font, ttsEditorTitle(this.siteDisplayName),
                this.startX + BASE_WIDTH / 2, this.startY + 4, 0xFFF3EFE0);

        // 判词到期即还原，靠每帧这一次调用，不另设计时器
        this.applyCheckVerdict();

        // 固定字段
        for (FormField field : this.fields) {
            this.renderInputField(graphics, field.box, mouseX, mouseY, partialTick);
            this.renderSecretPlaceholder(graphics, field);
        }

        this.renderHints(graphics);

        // 模型区
        if (this.modelArea != null) {
            this.renderModelArea(graphics, mouseX, mouseY, partialTick);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        // 保存提示
        if (System.currentTimeMillis() - this.tipTimestamp < 2000) {
            int x = this.startX + BASE_WIDTH - 155;
            int y = this.startY + BASE_HEIGHT - 35;
            graphics.centeredText(this.font, this.statusMessage, x, y, 0xFFADADAD);
        }

        if (this.checkButton != null) {
            this.checkButton.renderToolTip(graphics, this, mouseX, mouseY);
        }
    }

    /**
     * 画站点级说明，位置就是字段区的开头。
     *
     * <p>系统站点一个字段都没有，空白页无法告诉管理员「本来就没有可配置项」；更要紧的是它必须写明
     * <b>聊天界面里的合成语种对这个站点不生效</b>——那个设置看得见、点得动、存得下，而
     * {@code TTSSystemClient} 把 {@code TTSConfig} 整个丢掉了。<b>一个能设置却不起作用的选项，
     * 比没有这个选项更糟</b>，所以这不是装饰性文案。</p>
     */
    private void renderHints(GuiGraphicsExtractor graphics) {
        List<Component> hints = this.layout.hints();
        if (hints.isEmpty()) {
            return;
        }
        int left = this.startX + SiteEditorLayout.MARGIN;
        int y = this.startY + 32;
        for (Component hint : hints) {
            for (FormattedCharSequence line : this.font.split(hint, SiteEditorLayout.CONTENT_WIDTH)) {
                graphics.text(this.font, line, left, y, LABEL_COLOR, false);
                y += 11;
            }
            y += 4;
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

    /** 与 {@link LLMSiteEditorScreen} 同法：判词贴按钮，到期即还原 */
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
     * 密钥框空着的时候，用灰字说明它到底是「已配置」还是「未配置」。
     *
     * <p>下行只有哨兵、框里一律是空的，不给这行字管理员就完全分不出服务端有没有密钥。
     * <b>不用星号占位</b>：星号个数会泄漏长度，还会让人以为里面有内容能就地改。</p>
     */
    private void renderSecretPlaceholder(GuiGraphicsExtractor graphics, FormField field) {
        if (!field.secret || field.box == null || !field.box.getValue().isEmpty()) {
            return;
        }
        graphics.text(this.font, field.secretPlaceholder(),
                field.box.getX(), field.box.getY(), 0xFF808080, false);
    }

    private void renderInputField(GuiGraphicsExtractor graphics, EditBox box, int mouseX, int mouseY, float partialTick) {
        if (box == null) {
            return;
        }

        int x = box.getX() - 6;
        int y = box.getY() - 6;
        int width = box.getWidth() + 12;
        int height = box.getHeight() + 3;

        graphics.text(this.font, box.getMessage(), x + 2, y - 12, LABEL_COLOR, false);
        graphics.fill(x, y, x + width, y + height, 0xAA111111);
        box.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void renderModelArea(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = (int) this.modelArea.x;
        int top = (int) this.modelArea.y;

        // 主标题
        graphics.text(this.font, this.layout.modelsTitle(), left + 2, top - 14, LABEL_COLOR, false);

        int visibleCount = this.getVisibleModelCount();
        int startIndex = this.modelScrollOffset;
        int endIndex = Math.min(this.modelRows.size(), startIndex + visibleCount);

        graphics.enableScissor(left, top - 2, (int) this.modelArea.right(), (int) this.modelArea.bottom() + 2);
        for (int i = startIndex; i < endIndex; i++) {
            ModelRow row = this.modelRows.get(i);
            if (row.idBox != null && row.nameBox != null) {
                graphics.fill(row.idBox.getX() - 6, row.idBox.getY() - 6,
                        row.idBox.getX() + row.idBox.getInnerWidth() + 8,
                        row.idBox.getY() + row.idBox.getHeight() - 3,
                        0xAA111111
                );

                graphics.fill(row.nameBox.getX() - 6, row.nameBox.getY() - 6,
                        row.nameBox.getX() + row.nameBox.getInnerWidth() + 8,
                        row.nameBox.getY() + row.nameBox.getHeight() - 3,
                        0xAA111111
                );

                row.idBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
                row.nameBox.extractRenderState(graphics, mouseX, mouseY, partialTick);
            }
        }
        graphics.disableScissor();

        // 模型区滚动条
        if (this.modelRows.size() > visibleCount) {
            // 滚动条轨道的起始 Y 坐标
            int trackTop = top - 2;
            // 滚动条轨道的总高度
            int trackHeight = (int) this.modelArea.h - 8;
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
        if (this.modelArea != null && this.modelArea.contains(mouseX, mouseY)) {
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
        ScreenUtil.setScreen(this.parent);
    }

    private int getVisibleModelCount() {
        return Math.max(1, (int) ((this.modelArea.h - 4) / MODEL_ROW_HEIGHT));
    }

    private void saveSite() {
        Map<String, String> models = this.buildModels();
        TTSSite site = this.layout.buildSite(this::getFieldValue, models, this::showStatus);
        if (site == null) {
            return;
        }
        ClientPlayNetworking.send(SaveTTSSitePacket.update(site));
    }

    private Map<String, String> buildModels() {
        Map<String, String> models = Maps.newLinkedHashMap();
        for (ModelRow row : this.modelRows) {
            String id = StringUtils.trimToEmpty(row.id());
            String name = StringUtils.trimToEmpty(row.name());
            // 需要 id 和名称都不为空，并且 id 不能重复
            if (StringUtils.isBlank(id) || StringUtils.isBlank(name) || models.containsKey(id)) {
                continue;
            }
            models.put(id, name);
        }
        return models;
    }

    private String getFieldValue(String label) {
        return this.fields.stream()
                .filter(field -> field.label.equals(label))
                .findFirst()
                .map(FormField::value)
                .orElse(StringUtils.EMPTY);
    }

    private void showStatus(Component message) {
        this.statusMessage = message;
        this.tipTimestamp = System.currentTimeMillis();
    }

    private static final class ModelRow {
        private String id;
        private String name;
        private EditBox idBox;
        private EditBox nameBox;

        private ModelRow(String id, String name) {
            this.id = id;
            this.name = name;
        }

        private void syncFromBox() {
            if (this.idBox != null) {
                this.id = this.idBox.getValue();
            }
            if (this.nameBox != null) {
                this.name = this.nameBox.getValue();
            }
        }

        private String id() {
            return this.idBox != null ? this.idBox.getValue() : this.id;
        }

        private String name() {
            return this.nameBox != null ? this.nameBox.getValue() : this.name;
        }
    }
}
