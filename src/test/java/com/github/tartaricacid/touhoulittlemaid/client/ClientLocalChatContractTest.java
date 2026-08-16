package com.github.tartaricacid.touhoulittlemaid.client;

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
 * 客户端本地提示不得经 {@code LocalPlayer.displayClientMessage}。
 *
 * <p><b>实测的代价</b>（2026-07-30，jcmd 连续采样渲染线程，9 个连续样本同一栈、
 * CPU 时间完全不变，连续阻塞 4.5 秒）：语音识别结果一打出来，渲染线程就卡在一次到 Mojang 的
 * 同步 HTTPS 上。链路是原版的：</p>
 *
 * <pre>
 * displayClientMessage -> ChatListener.handleSystemMessage -> Minecraft.isBlocked
 *   -> PlayerSocialManager.isBlocked  { pendingBlockListRefresh.join(); service.isBlockedPlayer(id); }
 *   -> YggdrasilUserApiService.forceFetchBlockList   ← 阻塞 HTTPS
 * </pre>
 *
 * <p>{@code hideMatchedNames} 默认开启，且 {@code NIL_UUID} 不短路，所以「不带尖括号的文案」
 * 并不能规避。取到名单后会缓存——但对访问 Mojang 不畅的玩家，失败不缓存，于是每次识别都卡。</p>
 *
 * <p>这条按「客户端 AI/声音相关源文件」枚举，因此以后新加的客户端提示会自动被纳入。
 * 服务端的 {@code ServerPlayer.displayClientMessage} 不在此列：那是发包，不碰渲染线程。</p>
 *
 * <h3>2026-08-17 两处收紧（各由一次真实漏网逼出来）</h3>
 * <ol>
 *   <li><b>扫描面加 {@code client/gui}</b>：AI 聊天屏回显玩家自己那行也走这条链，
 *       而它在 GUI 目录里，原先的 SCOPE 根本够不着。</li>
 *   <li><b>判据加 {@code sendSystemMessage}</b>：26.1.2 删掉 {@code displayClientMessage} 后，
 *       宿主机械改写成了 {@code sendSystemMessage}，而 javap 实证
 *       {@code LocalPlayer.sendSystemMessage} 的字节码就是
 *       {@code getChatListener().handleSystemMessage(component, true)}——<b>同一条链，换了个名字</b>。
 *       判据要按「谁把文本送进了 ChatListener」这个成因定，不是按某个方法名的写法定。</li>
 * </ol>
 */
class ClientLocalChatContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path SRC = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid"));
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /** 只查真正在客户端跑的那几处：整类标了 CLIENT，或用的是 LocalPlayer */
    private static final List<Path> SCOPE = List.of(
            Path.of("client", "sound"),
            // 聊天屏回显玩家自己那行同样命中 guessChatUUID 的 <...> 解析
            Path.of("client", "gui", "entity", "maid", "ai"),
            Path.of("ai", "manager", "entity"),
            Path.of("ai", "service", "tts", "player2"),
            Path.of("ai", "service", "stt")
    );

    @Test
    void noClientSideCodeTalksThroughTheChatListener() throws IOException {
        List<String> offenders = new ArrayList<>();
        int scanned = 0;
        for (Path dir : SCOPE) {
            Path full = SRC.resolve(dir);
            if (!Files.isDirectory(full)) {
                continue;
            }
            try (Stream<Path> files = Files.walk(full)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String source = activeSource(file);
                    scanned++;
                    boolean clientSide = source.contains("EnvType.CLIENT") || source.contains("LocalPlayer");
                    // 两个名字同一条链：26.1.2 的 LocalPlayer.sendSystemMessage 字节码
                    // 就是 getChatListener().handleSystemMessage(component, true)
                    if (clientSide && (source.contains("displayClientMessage")
                            || source.contains("player.sendSystemMessage")
                            || source.contains("Player.sendSystemMessage"))) {
                        offenders.add(file.getFileName().toString());
                    }
                }
            }
        }
        assertTrue(scanned > 0, "一个文件都没扫到，这条断言已失去看守对象");
        assertTrue(offenders.isEmpty(),
                "这些客户端文件把本地提示送进了 ChatListener（displayClientMessage / "
                        + "LocalPlayer.sendSystemMessage 是同一条链），会让渲染线程等一次 Mojang 屏蔽名单请求，"
                        + "应改用 ClientLocalChat.show：" + String.join(", ", offenders));
    }

    /** 助手本身必须绕开 ChatListener，且不能顺手把无障碍朗读丢了 */
    @Test
    void theHelperBypassesTheListenerButKeepsNarration() throws IOException {
        String source = activeSource(SRC.resolve(Path.of("client", "ClientLocalChat.java")));
        // 26.1.2 把 ChatComponent.addMessage 私有化，按来源拆成 addServerSystemMessage /
        // addClientSystemMessage 两个公开入口。本地提示要的是**客户端来源**那一个：
        // 它与原版未被屏蔽分支走的 addServerSystemMessage 只差 GuiMessageSource 标记，
        // 渲染相同、同样不记聊天日志，而语义更准确。
        assertTrue(source.contains("gui.getChat().addClientSystemMessage"),
                "必须直接写聊天队列（客户端来源那一支），这正是绕开 ChatListener 的落点");
        assertTrue(!source.contains("addServerSystemMessage"),
                "本机自己打的提示不是服务器发来的系统消息，别用那一支");
        assertTrue(!source.contains("displayClientMessage"),
                "助手自己不得再走 displayClientMessage，否则等于没绕开");
        assertTrue(source.contains("saySystemChatQueued"),
                "绕开 ChatListener 不等于可以丢掉朗读器：无障碍不是可选项");
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
}
