package com.github.tartaricacid.touhoulittlemaid.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 声明了 payload 却没在 {@code NetworkHandler} 注册——本仓库最贵的一类缺陷，至今犯过两次
 * （{@code SyncFluidAmountPackage} 与 YSM 那两个），却一直没有门。
 *
 * <p><b>为什么贵</b>：未注册的 payload 不会撞上空 codec，而是落到 vanilla 给未知 id 的兜底解码器
 * {@code DiscardedPayload}；它的 encoder 由 {@code LambdaMetafactory} 生成，对外来类型 checkcast
 * 直接抛 {@code ClassCastException} → {@code EncoderException} → 而 {@code Packet.isSkippable()}
 * 默认为假 → {@code Connection.exceptionCaught} 断开连接。**单人档同样走这条序列化管线**，
 * 所以症状是「一发这个包玩家就掉线」，而编译、启动、单测全都测不出来。</p>
 *
 * <p>本测试按源码扫描：{@code network/message/**} 下每个声明了 {@code Type<X> TYPE} 的 payload，
 * 其类名必须出现在 {@code NetworkHandler} 的注册方法体里。走源码而不是反射，是因为注册发生在
 * 运行时初始化中，纯 JUnit 环境里没有 Fabric 的注册表可查。</p>
 */
class PayloadRegistrationInvariantTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path NETWORK = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "network"));
    private static final Path HANDLER = NETWORK.resolve("NetworkHandler.java");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    /** 声明自己是一个 payload：`Type<Xxx> TYPE = new Type<>(...)` */
    private static final Pattern DECLARES_TYPE = Pattern.compile("Type<[^>]+>\\s+TYPE\\s*=");

    @Test
    void everyDeclaredPayloadIsRegistered() throws IOException {
        String handler = activeSource(HANDLER);
        String s2c = methodBody(handler, "void registerS2CPackets(");
        String c2s = methodBody(handler, "void registerC2SPackets(");
        String receivers = methodBody(handler, "void registerClientReceivers(");

        List<String> unregistered = new ArrayList<>();
        List<String> s2cWithoutReceiver = new ArrayList<>();
        try (Stream<Path> files = Files.walk(NETWORK.resolve("message"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String source = activeSource(file);
                if (!DECLARES_TYPE.matcher(source).find()) {
                    continue;
                }
                String name = file.getFileName().toString().replace(".java", "");
                boolean inS2C = s2c.contains(name + ".TYPE");
                boolean inC2S = c2s.contains(name + ".TYPE");
                if (!inS2C && !inC2S) {
                    unregistered.add(name);
                    continue;
                }
                // S2C 还必须有客户端接收器，否则包到得了客户端却没人处理
                if (inS2C && !receivers.contains(name + ".TYPE")) {
                    s2cWithoutReceiver.add(name);
                }
            }
        }

        assertTrue(unregistered.isEmpty(),
                "这些 payload 声明了 TYPE 却没在 NetworkHandler 注册——一旦被发送就会踢掉玩家："
                        + String.join(", ", unregistered));
        assertTrue(s2cWithoutReceiver.isEmpty(),
                "这些 S2C payload 注册了编解码却没有客户端接收器，发过去无人处理："
                        + String.join(", ", s2cWithoutReceiver));
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
     * 按**方法声明**截取，不是按名字在全文件里找第一处——{@code registerPackets()} 里就有这三个方法的
     * 调用，且排在声明之前；用裸名字 {@code indexOf} 会截到调用点后面那对括号，得到一段毫不相干的
     * 方法体，于是每个 payload 都被判成未注册。本仓库在契约测试上栽过同一个跟头，故加签名前缀。
     */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue(start >= 0, "NetworkHandler 里找不到方法：" + signature + "（改名了就同步更新本测试）");
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
