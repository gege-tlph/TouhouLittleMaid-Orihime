package com.github.tartaricacid.touhoulittlemaid.gametest;

import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.network.message.config.SaveServerRulesPacket;
import com.github.tartaricacid.touhoulittlemaid.network.message.config.SyncServerRulesPacket;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * 世界规则网络层的运行期验收。
 *
 * <p>JUnit 那边能测编解码与暂存语义，但**测不到「包有没有真的被注册进去」**——
 * 载荷类型没注册时，编译、打包、启动一路正常，发包那一刻才抛。这是本仓库反复栽的静默注册面
 * （entrypoint / mixins.json / brain memory 之后的又一类），故在真服务器里查一次。</p>
 *
 * <p>⚠️ 只能查 C2S：{@code PayloadTypeRegistry} 没有任何读接口（javap 实查
 * fabric-networking-api-v1 6.3.1，只有四个静态工厂与一个 register），
 * S2C 的注册与进服下发因此改由源码层的 {@code ServerRuleNetworkWiringContractTest} 看管。</p>
 */
public class ServerRuleSyncGameTest {
    /**
     * {@code PlayerListMixin} 真的被织进去了。
     *
     * <p>{@code required: true} + {@code defaultRequire: 1} 只在**目标类被加载**时才会因注入失败而崩，
     * 所以「服务器起来了」本身并不构成证据。这里直接查织入产物：mixin 加进去的私有方法在不在
     * {@code PlayerList} 上。查得到 = 这个类被转换过且我们的注入落地了。</p>
     */
    @GameTest
    public void playerListMixinIsWovenIn(GameTestHelper helper) {
        boolean woven = java.util.Arrays.stream(
                        net.minecraft.server.players.PlayerList.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("tlm$resyncServerRules"));
        if (!woven) {
            helper.fail("PlayerListMixin 没有织进 PlayerList："
                    + "op/deop 之后不会重发规则快照，先进服后被授予 OP 的玩家要重进才看得到玩法设置");
            return;
        }
        helper.succeed();
    }

    @GameTest
    public void saveRulePayloadHasAServerSideReceiver(GameTestHelper helper) {
        if (!ServerPlayNetworking.getGlobalReceivers().contains(SaveServerRulesPacket.TYPE.id())) {
            helper.fail("SaveServerRulesPacket 没有服务端接收器：配置菜单一点保存就石沉大海");
            return;
        }
        helper.succeed();
    }

    /**
     * 快照的两半在专服语义下必须可分：{@code applyJson(.., false)} 只改文件快照，
     * 运行期快照要等重载。这条是专服「保存了但还没生效」那句提示的**机制依据**——
     * 两份快照如果实际上是同一份，那句提示就是在骗人。
     */
    @GameTest
    public void fileSnapshotAndRuntimeSnapshotDivergeUntilReload(GameTestHelper helper) {
        String key = ServerRuleConfig.key(MaidConfig.MAID_IDLE_RANGE);
        int original = ServerRuleConfig.get(MaidConfig.MAID_IDLE_RANGE);
        int probe = original == 21 ? 22 : 21;

        if (!ServerRuleConfig.applyJson("{\"" + key + "\":" + probe + "}", false)) {
            helper.fail("事务写盘被拒：" + key + " = " + probe);
            return;
        }
        try {
            int fileValue = JsonParser.parseString(ServerRuleConfig.snapshotJson())
                    .getAsJsonObject().get(key).getAsInt();
            if (fileValue != probe) {
                helper.fail("文件快照没有跟上：期望 " + probe + "，实为 " + fileValue);
                return;
            }
            if (ServerRuleConfig.get(MaidConfig.MAID_IDLE_RANGE) != original) {
                helper.fail("activate=false 却改了运行期值——专服的「保存后需重载」提示会变成假话");
                return;
            }
            if (!ServerRuleConfig.reloadFromDisk()) {
                helper.fail("reloadFromDisk 失败，/tlm config reload 这条路是断的");
                return;
            }
            if (ServerRuleConfig.get(MaidConfig.MAID_IDLE_RANGE) != probe) {
                helper.fail("重载后运行期值仍未生效：" + ServerRuleConfig.get(MaidConfig.MAID_IDLE_RANGE));
                return;
            }
        } finally {
            // 存档在同批用例间共用，改完必须还原
            ServerRuleConfig.applyJson("{\"" + key + "\":" + original + "}", true);
        }
        helper.succeed();
    }
}
