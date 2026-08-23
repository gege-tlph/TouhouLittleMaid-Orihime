package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings;

import com.github.tartaricacid.touhoulittlemaid.client.gui.widget.button.FlatColorButton;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.AIConfig;
import com.github.tartaricacid.touhoulittlemaid.network.client.config.ServerRulesClientCache;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.Translations.SAVE_NAME;

/**
 * 「用量管理」栏：花多少钱、配额多少。**仅有权限者可见。**
 *
 * <p><b>收录判据：只改变花多少钱、不改变功能的项。</b>按这条判据，
 * {@code AUTO_GEN_SETTING_ENABLED} 不进这里——关掉它女仆就不再自动获得人设，那是功能变化，
 * 不只是省钱。这条判据必须长期守住，否则这一栏会慢慢变成所有 LLM 杂项的垃圾桶，
 * 而分栏的意义正是让管理员一眼知道每一栏管什么。</p>
 *
 * <p>这两项是**实例级 AI 规则**，与其它屏同用一套暂存语义：改动进 {@link #ruleSession()}，
 * 「保存」一次性提交，脏时按钮带「*」。</p>
 */
public class AIChatSettingsUsageScreen extends AIChatSettingsHubScreen {
    private EditBox maxTokensInput;
    private EditBox historyCompressInput;
    private FlatColorButton saveButton;
    private long invalidHintUntil;

    public AIChatSettingsUsageScreen(@Nullable Screen parent, SharedState state, boolean insufficientPermissions) {
        super(parent, state, insufficientPermissions);
    }

    @Override
    protected Type getType() {
        return Type.USAGE;
    }

    @Override
    protected void initContent() {
        ServerRulesClientCache.Session session = this.ruleSession();
        int y = this.getContentY() + 2;

        this.maxTokensInput = this.addRuleInput(y,
                Component.translatable("ai.touhou_little_maid.chat.settings.hub.usage.max_tokens_per_player"),
                Component.translatable("ai.touhou_little_maid.chat.settings.hub.usage.max_tokens_per_player.tooltip"),
                this.maxTokensInput, String.valueOf(session.getInt(AIConfig.MAX_TOKENS_PER_PLAYER)));
        this.historyCompressInput = this.addRuleInput(y + ROW_PITCH,
                Component.translatable("ai.touhou_little_maid.chat.settings.hub.usage.history_compress"),
                Component.translatable("ai.touhou_little_maid.chat.settings.hub.usage.history_compress.tooltip"),
                this.historyCompressInput, String.valueOf(session.getInt(AIConfig.MAID_HISTORY_COMPRESS_TOKEN_LIMIT)));
    }

    @Override
    protected void persistTransientState() {
        // 切页前把解析得出的合法值冲进共享暂存（非法输入照旧丢弃）
        this.stageIfChanged(AIConfig.MAX_TOKENS_PER_PLAYER, this.maxTokensInput);
        this.stageIfChanged(AIConfig.MAID_HISTORY_COMPRESS_TOKEN_LIMIT, this.historyCompressInput);
    }

    private void stageIfChanged(ModConfigSpec.ConfigValue<Integer> rule, @Nullable EditBox box) {
        parsePositive(box).ifPresent(value -> {
            if (value != this.ruleSession().getInt(rule)) {
                this.ruleSession().set(rule, value);
            }
        });
    }

    @Override
    protected void addFooterButtons() {
        super.addFooterButtons();
        this.saveButton = this.addRenderableWidget(new FlatColorButton(
                this.getContentX(), this.getFooterY(), 80, 20, SAVE_NAME, b -> this.saveChanges()));
    }

    /**
     * 只写下**解析得出的合法值**，非法输入原样丢弃。
     *
     * <p>这两项都是 {@code defineInRange}，服务端也会再校验一次；这里先拦一道是为了让
     * 管理员在本地就看得出「这个值没被接受」——被丢弃的输入不会写进暂存，
     * 保存按钮上的「*」不会熄灭，那就是「没被接受」的信号。</p>
     */
    private void saveChanges() {
        boolean anyRejected = isRejected(this.maxTokensInput) || isRejected(this.historyCompressInput);
        if (anyRejected) {
            this.invalidHintUntil = System.currentTimeMillis() + 3000;
        }
        this.stageIfChanged(AIConfig.MAX_TOKENS_PER_PLAYER, this.maxTokensInput);
        this.stageIfChanged(AIConfig.MAID_HISTORY_COMPRESS_TOKEN_LIMIT, this.historyCompressInput);
        this.ruleSession().save();
        if (!anyRejected) {
            this.flashSaved();
        }
        this.init();
    }

    /** 非空但解析不出正整数 = 会被丢弃的输入，要给一行红字而不是只靠 * 不熄灭 */
    private static boolean isRejected(@Nullable EditBox box) {
        return box != null && !StringUtils.isBlank(box.getValue()) && parsePositive(box).isEmpty();
    }

    private boolean hasUnsavedRules() {
        ServerRulesClientCache.Session session = this.ruleSession();
        if (session.isDirty()) {
            return true;
        }
        return differs(this.maxTokensInput, session.getInt(AIConfig.MAX_TOKENS_PER_PLAYER))
                || differs(this.historyCompressInput, session.getInt(AIConfig.MAID_HISTORY_COMPRESS_TOKEN_LIMIT));
    }

    private static boolean differs(@Nullable EditBox box, int current) {
        return box != null && !box.getValue().trim().equals(String.valueOf(current));
    }

    private static Optional<Integer> parsePositive(@Nullable EditBox box) {
        if (box == null || StringUtils.isBlank(box.getValue())) {
            return Optional.empty();
        }
        try {
            int value = Integer.parseInt(box.getValue().trim());
            return value > 0 ? Optional.of(value) : Optional.empty();
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, 0xc0101010, 0xc0101010);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (System.currentTimeMillis() < this.invalidHintUntil) {
            graphics.text(this.font,
                    Component.translatable("ai.touhou_little_maid.chat.settings.hub.usage.invalid_number"),
                    this.getContentX(), this.getContentY() + 2 + ROW_PITCH * 2 + 4, 0xFFFF5555, false);
        }
        this.renderSavedFlash(graphics);
        if (this.saveButton != null) {
            this.saveButton.setMessage(this.hasUnsavedRules() ? SAVE_NAME.copy().append("*") : SAVE_NAME);
        }
        this.renderInsufficientPermissions(graphics);
        // 弹出层必须画在最后：它要压在上面这些子类自绘物之上
        this.renderHubOverlays(graphics, mouseX, mouseY);
    }
}
