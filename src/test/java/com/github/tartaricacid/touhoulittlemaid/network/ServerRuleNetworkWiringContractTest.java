package com.github.tartaricacid.touhoulittlemaid.network;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 世界规则网络层的**接线**看管。
 *
 * <p>为什么不是运行期测：{@code PayloadTypeRegistry} 一个读接口都没有（javap 实查
 * fabric-networking-api-v1 6.3.1），S2C 载荷有没有注册进去在运行期查不到；
 * 而它没注册的表现是「服务器一发包就抛」，编译打包启动全部正常——正是本仓库反复栽的静默注册面。
 * 查不到就只能在源码层钉住，总比没人看管强。</p>
 *
 * <p>三条缺一不可，各自对应一种「装了一半」的形态：</p>
 * <ul>
 *   <li>S2C 载荷没注册 → 服务器发不出快照</li>
 *   <li>客户端接收器没注册 → 包发出去了没人接，客户端一直用本地默认值（**最像"功能没做"的一种**）</li>
 *   <li>进服不下发 → 只有改过配置的人才拿得到值，新进来的玩家永远是默认值</li>
 * </ul>
 */
class ServerRuleNetworkWiringContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path ROOT = Path.of("..", "..");
    private static final Path NETWORK_HANDLER = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid", "network", "NetworkHandler.java"));

    @Test
    void syncPayloadIsRegisteredForClientbound() throws IOException {
        assertTrue(activeSource().contains("registerS2CPacket(SyncServerRulesPacket.TYPE"),
                "SyncServerRulesPacket 必须注册进 clientbound，否则服务器发不出世界规则快照");
    }

    @Test
    void syncPayloadHasAClientReceiver() throws IOException {
        assertTrue(activeSource().contains(
                        "ClientPlayNetworking.registerGlobalReceiver(SyncServerRulesPacket.TYPE"),
                "客户端必须注册 SyncServerRulesPacket 的接收器，"
                        + "否则包发到了也没人处理，客户端会一直用本地默认值——看起来就像这个功能没做");
    }

    @Test
    void savePayloadIsRegisteredForServerbound() throws IOException {
        assertTrue(activeSource().contains("registerC2SPacket(SaveServerRulesPacket.TYPE"),
                "SaveServerRulesPacket 必须注册进 serverbound，否则配置菜单一点保存就抛");
    }

    /**
     * 进服即下发。这条与上面三条是不同的失效面：包全都注册好了，但没人在玩家进服时发第一份，
     * 于是只有「有人改过配置」之后进来的玩家才拿得到值。
     */
    @Test
    void everyJoiningPlayerReceivesTheSnapshot() throws IOException {
        String active = activeSource();
        int joinHook = active.indexOf("ServerPlayConnectionEvents.JOIN");
        assertTrue(joinHook >= 0, "必须在 ServerPlayConnectionEvents.JOIN 上挂进服下发");
        assertTrue(active.indexOf("SyncServerRulesPacket.sendTo", joinHook) > joinHook,
                "JOIN 钩子里必须调 SyncServerRulesPacket.sendTo，"
                        + "否则新进服的玩家拿不到当前生效值");
    }

    /** 剥掉行注释、块注释与 javadoc：只剥 // 会让「仅存在于注释里的接线」被判为存在。 */
    private static String activeSource() throws IOException {
        StringBuilder active = new StringBuilder();
        boolean inBlockComment = false;
        for (String line : Files.readAllLines(NETWORK_HANDLER, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (inBlockComment) {
                int end = trimmed.indexOf("*/");
                if (end < 0) {
                    continue;
                }
                inBlockComment = false;
                trimmed = trimmed.substring(end + 2).trim();
            }
            trimmed = trimmed.replaceAll("/\\*.*?\\*/", "");
            int blockStart = trimmed.indexOf("/*");
            if (blockStart >= 0) {
                inBlockComment = true;
                trimmed = trimmed.substring(0, blockStart);
            }
            int lineComment = trimmed.indexOf("//");
            if (lineComment >= 0) {
                trimmed = trimmed.substring(0, lineComment);
            }
            active.append(trimmed).append('\n');
        }
        return active.toString();
    }
}
