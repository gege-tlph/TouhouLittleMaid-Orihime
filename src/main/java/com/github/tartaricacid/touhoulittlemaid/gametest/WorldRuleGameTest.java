package com.github.tartaricacid.touhoulittlemaid.gametest;

import com.github.tartaricacid.touhoulittlemaid.config.ConfigFileMigration;
import com.github.tartaricacid.touhoulittlemaid.config.ServerRuleConfig;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 世界规则的运行期全链路：服务器起来之后，这套东西是不是真的在管事。
 *
 * <p>JUnit 那边（{@code ServerRuleConfigTransactionTest}）测的是事务语义，喂的是临时目录里手工造的文件。
 * 这里测的是**另一半**——真实服务器启动时 {@code SERVER_STARTING} 钩子有没有被调、
 * 存档的 {@code serverconfig/} 下是不是真的落了文件、{@code ServerRuleConfig.get()} 读到的是不是它。
 * 这两件事单元测试按定义都验不到：接线是不是活的，只有真跑一次服务器才知道。</p>
 */
public class WorldRuleGameTest {
    @GameTest
    public void worldRulesAreLoadedFromTheSaveOnServerStart(GameTestHelper helper) {
        Path worldConfig = helper.getLevel().getServer()
                .getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig")
                .resolve(ConfigFileMigration.SERVER_FILE_NAME);

        if (!Files.isRegularFile(worldConfig)) {
            helper.fail("服务器已启动，但存档里没有世界规则文件：" + worldConfig
                    + "（SERVER_STARTING 钩子没接上，或 prepareWorldFile 没落盘）");
            return;
        }

        // ⚠️ 上面那条只是必要条件，**单独不足以证明接线是活的**：runGametest 的 run 目录在多轮之间复用，
        // 上一轮留下的文件会让「文件存在」在钩子被摘掉的情况下照样成立（红测实证：摘掉 SERVER_STARTING
        // 后这条依然通过，是下面那条把它照出来的）。
        // reloadFromDisk() 只有在 currentWorldFile 已被 loadForServer 设上时才可能为真——
        // 这才是「这一轮真的加载过」的判据，与「盘上还留着上一轮的文件」能区分开。
        if (!ServerRuleConfig.reloadFromDisk()) {
            helper.fail("世界规则没有在本轮启动时被加载（currentWorldFile 为空）："
                    + "SERVER_STARTING 钩子没接上。盘上那个文件可能是上一轮遗留的。");
            return;
        }

        // 读口必须给出这个存档的值。规则集里每一项都得读得出来——任何一项读到 null，
        // 都说明 values() 与 loadForServer 的加载结果对不上。
        for (var value : ServerRuleConfig.values()) {
            if (ServerRuleConfig.get(value) == null) {
                helper.fail("世界规则读不出值：" + ServerRuleConfig.key(value));
                return;
            }
        }

        // 抽一项验语义：读口给的值必须等于文件里的值，而不是 spec 默认值碰巧相同。
        // 用整型规则，改成一个与默认值不同的数再读回。
        int original = ServerRuleConfig.get(MaidConfig.MAID_WORK_RANGE);
        int probe = original == 15 ? 16 : 15;
        String key = ServerRuleConfig.key(MaidConfig.MAID_WORK_RANGE);
        if (!ServerRuleConfig.applyJson("{\"" + key + "\":" + probe + "}", true)) {
            helper.fail("事务写盘被拒：" + key + " = " + probe);
            return;
        }
        try {
            int applied = ServerRuleConfig.get(MaidConfig.MAID_WORK_RANGE);
            if (applied != probe) {
                helper.fail("保存即生效没有生效：期望 " + probe + "，实为 " + applied);
                return;
            }
            if (!ServerRuleConfig.reloadFromDisk()) {
                helper.fail("reloadFromDisk 失败——刚写下去的文件自己读不回来");
                return;
            }
            int reloaded = ServerRuleConfig.get(MaidConfig.MAID_WORK_RANGE);
            if (reloaded != probe) {
                helper.fail("从盘上重读得到 " + reloaded + "，与刚写入的 " + probe + " 不符");
                return;
            }
        } finally {
            // 这个存档在同批用例之间是共用的，改完必须还原，别把状态漏给下一条。
            ServerRuleConfig.applyJson("{\"" + key + "\":" + original + "}", true);
        }

        helper.succeed();
    }
}
