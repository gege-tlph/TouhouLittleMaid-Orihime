package com.github.tartaricacid.touhoulittlemaid.gametest;

import com.github.tartaricacid.touhoulittlemaid.config.AtomicConfigFileWriter;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 探路轮的运行期探针：证明 {@code runGametest} 真的会执行用例，且刚搬入的原子写在游戏运行时里成立。
 *
 * <p>为什么需要它：{@code :test} 在没有源码时报 {@code NO-SOURCE}，屏幕输出与"通过"一模一样；
 * GameTest 这条路更隐蔽——**类写好了也可能一次都不跑**，只要漏了 {@code fabric.mod.json} 的
 * {@code fabric-gametest} entrypoint，编译、打包、启动全都正常。1.21.11 分支上首轮三个新用例
 * 就是这么一个都没执行的。**判据必须是「报告里出现了这个用例」，不是「任务跑绿」。**
 *
 * <p>断言的内容不是摆设：配置写盘是玩家数据安全的底线，
 * 而"写一半掉电"这类故障只有在真实文件系统上才有意义，单元测试里的临时目录同样验不到运行时权限。
 */
public class ConfigWriteGameTest {
    @GameTest
    public void atomicWriteLeavesNoPartialImage(GameTestHelper helper) {
        try {
            Path dir = Files.createTempDirectory("tlm-gametest-config");
            Path target = dir.resolve("touhou_little_maid-server.toml");
            byte[] first = "value = 1\n".getBytes(StandardCharsets.UTF_8);
            byte[] second = "value = 2\n".getBytes(StandardCharsets.UTF_8);

            AtomicConfigFileWriter.write(target, first, path -> {
            });
            assertContent(helper, target, first, "首次写入");

            // 第二次写入必须整体替换，且把上一份完好的镜像留成 .last-good。
            AtomicConfigFileWriter.write(target, second, path -> {
            });
            assertContent(helper, target, second, "覆盖写入");

            Path lastGood = dir.resolve("touhou_little_maid-server.toml.last-good");
            if (!Files.isRegularFile(lastGood)) {
                helper.fail("覆盖写入后没有留下 .last-good：坏配置将无从回滚");
            }

            // 目录里不得残留任何临时镜像——它们是"写了一半"的证据。
            try (var stream = Files.list(dir)) {
                stream.filter(p -> p.getFileName().toString().endsWith(".tmp"))
                        .findAny()
                        .ifPresent(p -> helper.fail("目录里残留临时文件 " + p.getFileName()));
            }

            helper.succeed();
        } catch (IOException e) {
            helper.fail("原子写抛出 IOException: " + e);
        }
    }

    private static void assertContent(GameTestHelper helper, Path target, byte[] expected, String stage) {
        try {
            byte[] actual = Files.readAllBytes(target);
            if (!java.util.Arrays.equals(expected, actual)) {
                helper.fail(stage + "后内容不符：期望 " + new String(expected, StandardCharsets.UTF_8)
                        + "，实为 " + new String(actual, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            helper.fail(stage + "后读不回目标文件: " + e);
        }
    }
}
