package com.github.tartaricacid.touhoulittlemaid.client.gui.entity.maid.ai.settings;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 设置枢纽弹出列表的健壮性不变量。
 *
 * <p>这些条目原先只在人工验收矩阵（设计文 §16 H 表）里，靠人点着试。它们全是**接线**，
 * 而接线漏掉在本仓库是有案底的失败形态（「纸面接口」已实证 9 枚）：加第五个标签页却忘了
 * 画覆盖层，弹出列表就永久不可见——编译、启动、单测全绿，功能从不出现。故钉成断言。</p>
 *
 * <p>Screen 需要一个真实的 Minecraft 客户端实例才能构造，纯 JUnit 环境里造不出来，
 * 因此与仓库既有的几个契约测试同法，在源码层断言。**真正的视觉与手感仍须人工验收**——
 * 本测试只保证「该调的都调了」，不保证「看起来对」。</p>
 */
class SettingsPopupWiringContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path SETTINGS_DIR = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "client", "gui", "entity", "maid", "ai", "settings"));
    private static final Path HUB = SETTINGS_DIR.resolve("AIChatSettingsHubScreen.java");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /** 缩放窗口 / 点开关都会重建控件，而弹层记着旧坐标与旧回调——重建必须先关掉它 */
    @Test
    void rebuildingWidgetsClosesTheOpenPopup() throws IOException {
        String init = methodBody(activeSource(HUB), "protected void init()");
        int close = init.indexOf("closeHubPopup()");
        int firstWidget = init.indexOf("addVoiceInputSideButtons");
        assertTrue(close >= 0, "init 必须关闭弹出列表：它记着重建前的坐标与回调");
        assertTrue(firstWidget < 0 || close < firstWidget,
                "关弹层必须早于重建控件，否则关闭动作可能被新一轮布局覆盖");
    }

    /** 弹层开着时滚轮归它，底下的站点列表不许跟着滚 */
    @Test
    void anOpenPopupOwnsTheScrollWheel() throws IOException {
        String hub = activeSource(HUB);
        String listScroll = methodBody(hub, "protected boolean handleListScroll(");
        int guard = listScroll.indexOf("popupEntries != null");
        int listAreaUse = listScroll.indexOf("this.listArea");
        assertTrue(guard >= 0, "handleListScroll 必须先判断弹层是否打开");
        assertTrue(guard < listAreaUse, "弹层判定必须早于列表区域判定，否则列表会跟着一起滚");

        String scrolled = methodBody(hub, "public boolean mouseScrolled(");
        assertTrue(scrolled.indexOf("popupEntries != null") >= 0,
                "mouseScrolled 必须把滚轮交给打开着的弹层（翻页）");
    }

    /** 点弹层外面只关闭，这一击不许穿透到底下的控件上（否则会误触相邻按钮） */
    @Test
    void clickingOutsideOnlyClosesAndDoesNotFallThrough() throws IOException {
        String clicked = methodBody(activeSource(HUB), "public boolean mouseClicked(");
        int close = clicked.lastIndexOf("closeHubPopup()");
        int superCall = clicked.indexOf("super.mouseClicked");
        assertTrue(close >= 0 && superCall > close,
                "外部点击必须先关闭弹层再返回，且不得落到 super（那等于穿透误触）");
        String tail = clicked.substring(close, superCall);
        assertTrue(tail.contains("return true"),
                "关闭弹层后必须 return true 吞掉这一击");
    }

    /** Esc 关弹层而不是直接退出整屏 */
    @Test
    void escapeClosesThePopupBeforeLeavingTheScreen() throws IOException {
        String pressed = methodBody(activeSource(HUB), "public boolean keyPressed(");
        int escape = pressed.indexOf("GLFW_KEY_ESCAPE");
        int superCall = pressed.indexOf("super.keyPressed");
        assertTrue(escape >= 0 && escape < superCall,
                "Esc 必须先给弹层一次关闭机会，再考虑退出屏幕");
    }

    /**
     * <b>本类的主菜</b>：每个自绘 render 的标签页都必须画覆盖层。
     *
     * <p>覆盖层要压在滚动条、提示字这些子类自绘物之上，所以它只能由子类在自己 render 的末尾调用——
     * 这就意味着**新增标签页时漏调不会有任何报错**，弹出列表只是静默不可见。</p>
     */
    @Test
    void everyTabScreenDrawsTheOverlay() throws IOException {
        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.list(SETTINGS_DIR)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith("Screen.java")).toList()) {
                if (file.equals(HUB)) {
                    continue;
                }
                String source = activeSource(file);
                if (!source.contains("public void render(")) {
                    continue;
                }
                if (!source.contains("renderHubOverlays")) {
                    missing.add(file.getFileName().toString());
                }
            }
        }
        assertTrue(missing.isEmpty(),
                "这些标签页自绘了 render 却没画弹出层覆盖，弹出列表在它们上面永久不可见："
                        + String.join(", ", missing));
    }

    /**
     * <b>每一个带保存按钮的标签页，其保存路径都必须提交共享的规则暂存。</b>
     *
     * <p>hub 把契约写死了：「在任何一页点保存都会把攒着的全部改动一起提交」。语音输入页曾经违约——
     * 它是四页里唯一另有本机配置的，于是保存方法写成了「只存本机」，却照发绿字「已保存」。
     * 后果不是少存了东西，而是**告诉玩家存好了**：在文字模型页翻个开关、切过来保存、退出，
     * 世界规则改动静默丢失。静默丢数据 + 明确的成功提示，是最坏的组合。</p>
     *
     * <p>这条断言按「有 SAVE_NAME 按钮的屏」枚举，所以第五个标签页加进来时会自动被纳入。</p>
     */
    @Test
    void everySaveButtonCommitsTheSharedRuleSession() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.list(SETTINGS_DIR)) {
            for (Path file : files.filter(p -> p.getFileName().toString().endsWith("Screen.java")).toList()) {
                if (file.equals(HUB)) {
                    continue;
                }
                String source = activeSource(file);
                if (!source.contains("SAVE_NAME")) {
                    continue;
                }
                if (!source.contains("ruleSession().save()")) {
                    offenders.add(file.getFileName().toString());
                }
            }
        }
        assertTrue(offenders.isEmpty(),
                "这些标签页有保存按钮却从不提交共享暂存，会造成「显示已保存但改动丢失」："
                        + String.join(", ", offenders));
    }

    /**
     * <b>客户端不做站点存在性判定</b>——那是服务端的权威，它有权威表也有 UNAVAILABLE 回执。
     *
     * <p>{@code AvailableSites} 在两侧各自由**自己那份** {@code sites/tts.json} 填充
     * （`init()` 挂在 main entrypoint，两侧都跑）。专服上管理员配的站点客户端本地多半没有，
     * 客户端一旦自行判「不存在」就拒发，玩家看到的是「服务端明明有，按钮却按不动」。
     * 而**单人档里两张表是同一张，这个洞在单人档下按定义测不出来**——我就是这样在一次
     * `server_type: local` 的全通过验收之后才发现它的。</p>
     */
    @Test
    void theClientNeverDecidesThatASiteIsMissing() throws IOException {
        Path previewClient = ROOT.resolve(Path.of("src", "main", "java", "com", "github", "tartaricacid",
                "touhoulittlemaid", "client", "sound", "VoicePreviewClient.java"));
        String source = activeSource(previewClient);
        assertTrue(!source.contains("unavailable.missing"),
                "VoicePreviewClient 不得自行判定站点缺失：那个判据属于服务端的 Validation，"
                        + "客户端本地站点表与服务端不是同一张，本地拒发会让专服上的可用站点变成点不动的按钮");
    }

    private static String activeSource(Path path) throws IOException {
        StringBuilder active = new StringBuilder();
        for (String line : Files.readAllLines(path)) {
            if (line.trim().startsWith("//")) {
                continue;
            }
            active.append(line).append('\n');
        }
        return BLOCK_COMMENT.matcher(active.toString()).replaceAll(" ");
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "源码里找不到方法：" + signature);
        int brace = source.indexOf('{', start);
        assertTrue(brace >= 0, "方法没有方法体：" + signature);
        int depth = 0;
        for (int i = brace; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(brace, i + 1);
                }
            }
        }
        throw new IllegalStateException("方法体大括号不配对：" + signature);
    }
}
