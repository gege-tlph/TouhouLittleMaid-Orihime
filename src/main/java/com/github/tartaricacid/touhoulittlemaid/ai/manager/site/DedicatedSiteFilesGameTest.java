package com.github.tartaricacid.touhoulittlemaid.ai.manager.site;

import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.gametest.framework.GameTestHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 专用服务器不该管理 STT 站点，因而不该生成 {@code stt.json}。
 *
 * <p>这条原先只能人工验（「专服起一次看 sites/ 里有几个文件」）。**GameTest 的运行环境正是
 * {@code EnvType.SERVER}**（`build.gradle` 里 `gametest { server() }`），所以物理端判据在这里
 * 就能真验——而判据本身是一个 {@code envType != SERVER} 的单比较，服务端侧验通了，
 * 客户端侧（三个文件照常生成）在逻辑上随之成立，不必再单独跑一次单人档。</p>
 *
 * <p>为什么这件事值得一道门：<b>载体的存在会被读成语义</b>。一份躺在服务端配置目录里的
 * {@code stt.json} 会让管理员照着它去找一个并不存在的服务端开关——那正是曾经烧掉三轮排查的
 * 那类误导。文件不该在，就必须真的不在。</p>
 */
public final class DedicatedSiteFilesGameTest {
    /**
     * <b>先删再验「不会被重新生成」</b>，而不是直接断言文件不存在：run-dir 跨轮持久化，
     * 别的用例或上一轮残留都可能留下一份 stt.json，那样这条用例的结论就取决于执行顺序。
     */
    @GameTest(maxTicks = 100)
    public void aDedicatedServerNeverGeneratesTheSttFile(GameTestHelper helper) {
        helper.assertTrue(FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER,
                "本用例的前提是 GameTest 跑在物理服务端；若 build.gradle 改成 client()，"
                        + "它测的东西就变了，必须同步更新");

        Path sites = FabricLoader.getInstance().getConfigDir()
                .resolve("touhou_little_maid").resolve("sites");
        Path stt = sites.resolve("stt.json");
        try {
            Files.createDirectories(sites);
            Files.deleteIfExists(stt);
        } catch (IOException exception) {
            throw new AssertionError("无法清理探针前置状态", exception);
        }

        helper.assertTrue(AvailableSites.init(), "站点加载应干净完成");
        helper.assertTrue(Files.isRegularFile(sites.resolve("llm.json")), "专服应生成 llm.json");
        helper.assertTrue(Files.isRegularFile(sites.resolve("tts.json")), "专服应生成 tts.json");
        helper.assertFalse(Files.exists(stt),
                "专服不得生成 stt.json：语音识别整条链路都在玩家客户端，"
                        + "这份文件在服务端没有任何消费者，留着只会误导管理员");
        helper.assertTrue(AvailableSites.STT_SITES.isEmpty(),
                "专服不得持有任何 STT 站点，实测有 " + AvailableSites.STT_SITES.size() + " 个");
        helper.succeed();
    }

    /**
     * 旧档遗留的 {@code stt.json}（撤除功能前生成的）不得影响启动。
     * 它应当只换来一条日志提示——「不影响启动」这一半在这里验，日志那一半仍需人读。
     */
    @GameTest(maxTicks = 100)
    public void aStrayLegacySttFileDoesNotBreakLoading(GameTestHelper helper) {
        Path root = FabricLoader.getInstance().getConfigDir().resolve("touhou_little_maid").resolve("sites");
        Path stray = root.resolve("stt.json");
        try {
            Files.createDirectories(root);
            Files.writeString(stray, "{\"aliyun\":{\"enabled\":true}}");
            boolean clean = AvailableSites.init();
            helper.assertTrue(clean, "遗留的 stt.json 不得让站点加载报告为非干净——"
                    + "它在专服上根本不参与加载");
            helper.assertTrue(AvailableSites.STT_SITES.isEmpty(),
                    "遗留文件不得被读进站点表：专服不管理 STT");
            helper.assertTrue(Files.exists(stray), "不得删除管理员的文件，只提示不动手");
        } catch (IOException exception) {
            throw new AssertionError("写入探针文件失败", exception);
        } finally {
            try {
                Files.deleteIfExists(stray);
            } catch (IOException ignored) {
                // 探针清理失败不该让用例结论翻转
            }
        }
        helper.succeed();
    }
}
