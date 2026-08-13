package com.github.tartaricacid.touhoulittlemaid;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 每一个 mixin 源文件都必须登记进对应的 {@code *.mixins.json}。
 *
 * <p>这是核心纪律第 3 条的机械化：**没登记的 mixin 是纯静默的**——编译、打包、启动一路正常，
 * 功能一次都不执行。本仓库已经为此抓过 5 枚「纸面接口」（ScreenAccessor、LivingEntityRendererMixin、
 * KeyboardHandlerMixin、SoundEngineMixin、ChannelAccessHandleMixin），症状是 CCE 或功能静默失效。
 * 反过来，**登记了但靶点不存在**会因为 {@code required: true} + {@code defaultRequire: 1} 直接崩启动，
 * 那一半有闸门看着，不需要这条测试。</p>
 *
 * <p>判据是「源码树里有的，配置里必须有」——反向（配置里有、源码没有）会被 mixin 自己在启动时报错，
 * 不在本测试的职责内。</p>
 */
class MixinRegistrationInvariantTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path ROOT = Path.of("..", "..");
    private static final Path SOURCE_ROOT = ROOT.resolve("src/main/java");
    private static final Path RESOURCES = ROOT.resolve("src/main/resources");

    private record MixinConfig(String fileName, String packageName) {
    }

    private static final List<MixinConfig> CONFIGS = List.of(
            new MixinConfig("touhou_little_maid.mixins.json",
                    "com.github.tartaricacid.touhoulittlemaid.mixin"),
            new MixinConfig("touhou_little_maid_fabric.mixins.json",
                    "cn.sh1rocu.touhoulittlemaid.mixin"));

    /**
     * 代码宿主自己搁置的 mixin，**不是我们漏登记的**。
     *
     * <p>{@code client.HumanoidModelMixin}：整个方法体被注释掉、{@code @Mixin} 靶点改指
     * {@code cn.sh1rocu.touhoulittlemaid.util.Dummy}、类上留着 {@code // FIXME}——
     * {@code origin/26.1} 迁移期停在这里的。登记它没有任何意义（靶点是个空壳类）。</p>
     *
     * <p>⚠️ 但它是一处**真实的行为回归**：`origin/1.21.1` 与行为基准 `port/1.21.11-fabric`
     * 上这个 mixin 都是注册且生效的（玩家扛女仆时两条手臂会摆成抱姿）。
     * 已记入 CURRENT_STATUS 开放项，**修好之后要把这一条从本名单里删掉**——
     * 名单留着而缺陷已修，下一个人会以为它还坏着。</p>
     */
    private static final Set<String> HOST_PARKED = Set.of("client.HumanoidModelMixin");

    @Test
    void everyMixinSourceFileIsRegistered() throws IOException {
        List<String> unregistered = new ArrayList<>();
        int checked = 0;

        for (MixinConfig config : CONFIGS) {
            Set<String> declared = declaredEntries(RESOURCES.resolve(config.fileName()));
            assertTrue(declared.size() >= 10,
                    config.fileName() + " 只解析出 " + declared.size() + " 条登记，解析多半是坏的");

            Path packageRoot = SOURCE_ROOT.resolve(config.packageName().replace('.', '/'));
            assertTrue(Files.isDirectory(packageRoot), "找不到 mixin 源码目录 " + packageRoot);

            try (Stream<Path> files = Files.walk(packageRoot)) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String relative = packageRoot.relativize(file).toString()
                            .replace('\\', '/')
                            .replaceAll("\\.java$", "")
                            .replace('/', '.');
                    if (relative.equals("package-info") || HOST_PARKED.contains(relative)) {
                        continue;
                    }
                    checked++;
                    // 内部类以 Outer$Inner 单独登记；外层类登记了就算它这一支到位。
                    boolean registered = declared.contains(relative)
                            || declared.stream().anyMatch(entry -> entry.startsWith(relative + "$"));
                    if (!registered) {
                        unregistered.add(config.fileName() + " 缺 " + relative);
                    }
                }
            }
        }

        assertTrue(checked >= 60, "只扫到 " + checked + " 个 mixin 源文件，扫描本身可能已失效");
        assertEquals(List.of(), unregistered,
                "这些 mixin 没有登记进 mixins.json：编译、打包、启动全都正常，功能一次都不会执行");
    }

    private static Set<String> declaredEntries(Path configPath) throws IOException {
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            Set<String> entries = new HashSet<>();
            for (String key : List.of("mixins", "client", "server")) {
                if (!root.has(key)) {
                    continue;
                }
                JsonArray array = root.getAsJsonArray(key);
                for (JsonElement element : array) {
                    entries.add(element.getAsString());
                }
            }
            return entries;
        }
    }
}
