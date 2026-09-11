package com.github.tartaricacid.touhoulittlemaid.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * YSM 轮盘动画同步这条链路的硬约束。
 *
 * <p><b>为什么需要这条测试</b>：{@code PayloadRegistrationInvariantTest} 只管「payload 声明了有没有
 * 注册」，管不到<b>有没有人发它</b>。而本链路此前的缺陷恰恰在发送侧——
 * {@code rouletteAnimDirty} 三处出现全是<b>写</b>（字段声明 + 两个 setter 里置真），
 * <b>没有任何地方读它</b>：包类、codec、注册面即便全部齐备，服务端也一次都不会发。
 * 编译、启动、进世界、单人档全部正常（单人档两端是同一个实例，字段直接可见），
 * 只有专服上轮盘动画从来不播。</p>
 *
 * <p><b>{@code rouletteAnimDirty} 不得成为 wire field</b> 是第二条硬约束。它在两侧角色相反：
 * 服务端是出站触发器（读到即发送并置回 false），客户端是渲染端脏标记（收包后置回 true 供
 * OpenYSM 的 predicate 消费）。一旦上线，收包会覆盖掉接收侧自己刚置的值，两个语义直接打架。</p>
 */
class YsmRouletteSyncWiringContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path SRC = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid"));
    private static final Path ENTITY_MAID = SRC.resolve(Path.of("entity", "passive", "EntityMaid.java"));
    private static final Path TRACK_EVENT = SRC.resolve(Path.of("event", "MaidTrackEvent.java"));
    private static final Path PACKAGE = SRC.resolve(Path.of("network", "message", "SyncYsmMaidDataPackage.java"));
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /**
     * <b>本类的主菜</b>：服务端 tick 必须真的消费那个脏标记并发包。
     *
     * <p>断言落在 {@code tick()} 的方法体上，不是整份文件——两个 setter 里的
     * {@code rouletteAnimDirty = true} 在全文件里同样命中，用全文搜索这条断言恒绿。</p>
     */
    @Test
    void theServerTickConsumesTheDirtyFlagAndSends() throws IOException {
        String tick = methodBody(activeSource(ENTITY_MAID), "public void tick(", ENTITY_MAID);

        assertTrue(tick.contains("rouletteAnimDirty"),
                "tick() 里没有读 rouletteAnimDirty——出站触发器缺失，专服上轮盘动画永远不播");
        assertTrue(tick.contains("rouletteAnimDirty = false"),
                "tick() 读了脏标记却没清，会每 tick 重复发包");
        assertTrue(tick.contains("SyncYsmMaidDataPackage"),
                "tick() 里没有构造同步包");
        assertTrue(tick.contains("sendToPlayersTrackingEntity"),
                "同步包必须广播给全部追踪者，只发给主人会让旁观者看不到轮盘动画");
        assertTrue(tick.contains("isClientSide()"),
                "出站触发器必须限定在服务端，否则客户端也会尝试发 S2C 包");
    }

    /** 脏标记不得出现在报文里——它在两侧的语义相反。 */
    @Test
    void theDirtyFlagIsNotAWireField() throws IOException {
        String source = activeSource(PACKAGE);
        int header = source.indexOf("public record SyncYsmMaidDataPackage");
        assertTrue(header >= 0, "找不到 record 声明（改名了就同步更新本测试）");
        String components = source.substring(header, source.indexOf('{', header));

        assertFalse(components.contains("Dirty"),
                "rouletteAnimDirty 被写进了报文：收包会覆盖接收侧自己置的渲染脏标记");
        // 下限断言：识别依据一旦失效（record 改成 class、组件改名），上面那条否定断言会自动成立。
        assertTrue(components.contains("rouletteAnim"),
                "record 组件里连 rouletteAnim 都没有，判据已失效——先修判据");
    }

    /**
     * 后进入追踪范围的玩家要补一次全量。
     *
     * <p>tick 侧那个触发器只在状态<b>变化</b>时发一次；新登录 / 跨维度 / 走近的玩家错过了那一次，
     * 没有这条初始同步，她的轮盘动画对这些人就永远不播——而这恰恰是专服上最常见的进入方式。</p>
     */
    @Test
    void aPlayerWhoStartsTrackingGetsAFullSync() throws IOException {
        String body = methodBody(activeSource(TRACK_EVENT), "public static void onTrackingPlayer(", TRACK_EVENT);

        assertTrue(body.contains("SyncYsmMaidDataPackage"),
                "开始追踪女仆时没有补发轮盘状态，后进入范围的玩家永远看不到");
        assertTrue(body.contains("isYsmModel()"),
                "初始同步必须只对 YSM 模型的女仆发，否则每个玩家每只女仆都白发一个包");
    }

    /**
     * 活性下限：本测试认的这三个符号必须真的还在源码里。
     *
     * <p>没有这条，任何一次改名都会让上面三条断言变成「在一段找不到的文本里找不到东西」，
     * 而 {@code methodBody} 之外的 {@code contains} 断言会静默失去覆盖。</p>
     */
    @Test
    void theSymbolsThisTestRecognisesStillExist() throws IOException {
        assertTrue(activeSource(ENTITY_MAID).contains("rouletteAnimPlaying"),
                "EntityMaid 里已无 rouletteAnimPlaying，本测试的识别依据整体失效");
        assertEquals(1, countOccurrences(activeSource(PACKAGE), "sync_ysm_maid_data"),
                "报文 id 应恰好声明一次；变了就说明协议动过，需与 OpenYSM 侧重新对齐");
    }

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        for (int i = haystack.indexOf(needle); i >= 0; i = haystack.indexOf(needle, i + needle.length())) {
            count++;
        }
        return count;
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

    /**
     * 按**方法声明**截取。与 {@code PayloadRegistrationInvariantTest} 同款，理由也相同：
     * 用裸名字 {@code indexOf} 会截到调用点，得到一段毫不相干的方法体。
     */
    private static String methodBody(String source, String signature, Path path) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, path.getFileName() + " 里找不到方法：" + signature + "（改名了就同步更新本测试）");
        int brace = source.indexOf('{', start);
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
