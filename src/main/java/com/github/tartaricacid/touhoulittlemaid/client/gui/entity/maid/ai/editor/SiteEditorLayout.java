package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.editor;

/**
 * 站点编辑器的横向布局：底部按钮一排，以及密钥行右端那颗「清除」。
 *
 * <p>这些数字原先由 LLM / TTS / STT 三个编辑屏各写一份字面值，而它们本该完全相同。
 * 结果就是各自算错：往基准只有「添加 / 保存 / 返回」的那一排里塞进「检查配置」时，
 * 取了 {@code BASE_WIDTH - 302}（= 98）而「添加」占到 108，**两颗按钮重叠 10 像素**；
 * 同期加的「清除」压在密钥输入框的可点区域上，重叠带里的点击还会被输入框先吃掉。</p>
 *
 * <p>纯 int 常量，不碰任何 Minecraft 类型——{@code SiteEditorLayoutTest} 因此能在
 * 无客户端的 JUnit 环境里把「任意两颗按钮不得重叠」断言成机械规则。屏幕布局做不到
 * 端到端自动验收（构造 Screen 要真实客户端），但**算术这一层能钉死，就不该靠眼睛看**。</p>
 */
public final class SiteEditorLayout {
    /** 三个编辑屏共用的面板宽度 */
    public static final int PANEL_WIDTH = 400;
    /** 面板左右留白，内容区 = PANEL_WIDTH - MARGIN * 2 */
    public static final int MARGIN = 12;
    public static final int CONTENT_WIDTH = PANEL_WIDTH - MARGIN * 2;

    /** 按钮之间的最小间隙，布局断言按它检查 */
    public static final int GAP = 4;

    // ---- 底部按钮一排（「添加」「保存」「返回」的取值与 origin/1.21.1 逐字相同，不得挪动）----

    public static final int ADD_MODEL_X = MARGIN;
    public static final int ADD_MODEL_WIDTH = 96;

    /**
     * 「检查配置」：夹在「添加」与「保存」之间那 92 像素的空档里。
     *
     * <p>宽度 84 而不是照抄邻居的 96——空档放不下 96，这正是它当初重叠的原因。</p>
     */
    public static final int CHECK_CONFIG_X = 112;
    public static final int CHECK_CONFIG_WIDTH = 84;

    public static final int SAVE_X = PANEL_WIDTH - 200;
    public static final int SAVE_WIDTH = 90;

    public static final int BACK_X = PANEL_WIDTH - 102;
    public static final int BACK_WIDTH = 90;

    // ---- 密钥行 ----

    public static final int SECRET_CLEAR_WIDTH = 66;
    public static final int SECRET_CLEAR_X = PANEL_WIDTH - MARGIN - SECRET_CLEAR_WIDTH;

    /**
     * 显示「清除」时密钥输入框必须让出位置。
     *
     * <p>不让位不只是难看：输入框比按钮先 {@code addWidget}，于是重叠带里的点击由输入框
     * 拿走，按钮下半截按不动。</p>
     */
    public static final int SECRET_WIDTH_WITH_CLEAR = SECRET_CLEAR_X - MARGIN - GAP - 2;

    private SiteEditorLayout() {
    }
}
