package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai;

import com.github.tartaricacid.touhoulittlemaid.ai.service.Site;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * 表单文本输入字段，用于 TTS / STT 编辑器中的通用输入行
 */
public class FormField {
    public static final String URL = "url";
    public static final String SECRET_ID = "secret_id";
    public static final String SECRET_KEY = "secret_key";
    public static final String MODEL = "model";
    public static final String MODELS = "models";
    public static final String APP_KEY = "app_key";
    public static final String ENG_SER_VICE_TYPE = "eng_ser_vice_type";
    public static final String HOT_WORD = "hot_word";
    public static final String REF_AUDIO_PATH = "ref_audio_path";
    public static final String PROMPT_TEXT = "prompt_text";

    public final String label;
    public final boolean editable;
    public final boolean secret;

    public String value;
    public EditBox box;

    /**
     * 服务端说这一项已经配好了，但没有把明文发下来（下行只有哨兵）。
     *
     * <p>输入框因此从**空**开始：既不能显示哨兵那串内部标记，也不能显示星号——
     * 星号的个数会泄漏密钥长度，而且会让人以为里面有内容可以就地编辑。</p>
     */
    private final boolean secretAlreadySet;

    /** 管理员点过「清除」。它必须与「没碰这个框」区分开，否则永远删不掉一个密钥。 */
    private boolean cleared;

    public FormField(String label, String value, boolean editable, boolean secret) {
        this.label = label;
        this.secretAlreadySet = secret && Site.SECRET_KEPT.equals(value);
        this.value = this.secretAlreadySet ? "" : value;
        this.editable = editable;
        this.secret = secret;
    }

    public boolean secretAlreadySet() {
        return this.secretAlreadySet;
    }

    public boolean cleared() {
        return this.cleared;
    }

    /** 点「清除」：框内清空，并记下这是一次显式清除。 */
    public void clearSecret() {
        this.cleared = true;
        this.value = "";
        if (this.box != null) {
            this.box.setValue("");
        }
    }

    public void syncFromBox() {
        if (this.box != null) {
            this.value = this.box.getValue();
        }
    }

    /**
     * 提交给服务端的值。**三态在这里收敛成一个字符串。**
     *
     * <ul>
     *   <li>已配置且没动过 → 哨兵：服务端把原值填回去（这是默认行为，也是不丢密钥的关键）</li>
     *   <li>点过「清除」 → 空串：服务端如实清空</li>
     *   <li>填了新值 → 新值</li>
     * </ul>
     */
    public String value() {
        String current = this.box != null ? this.box.getValue() : this.value;
        if (this.secret && this.secretAlreadySet && !this.cleared && current.isEmpty()) {
            return Site.SECRET_KEPT;
        }
        return current;
    }

    public MutableComponent i18nName() {
        return Component.translatable("ai.touhou_little_maid.chat.settings.hub.%s".formatted(this.label));
    }

    /**
     * 密钥框的占位提示：区分「已配置」与「未配置」。
     *
     * <p>这两种状态在界面上必须看得出来，否则管理员无法判断服务端到底有没有密钥——
     * 而下行只有哨兵，框里又都是空的，不给提示就完全分不出。</p>
     */
    public MutableComponent secretPlaceholder() {
        if (this.cleared) {
            return Component.translatable("ai.touhou_little_maid.chat.settings.hub.secret_cleared");
        }
        return Component.translatable(this.secretAlreadySet
                ? "ai.touhou_little_maid.chat.settings.hub.secret_configured"
                : "ai.touhou_little_maid.chat.settings.hub.secret_unset");
    }
}
