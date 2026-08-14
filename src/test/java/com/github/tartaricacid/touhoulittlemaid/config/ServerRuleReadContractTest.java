package com.github.tartaricacid.touhoulittlemaid.config;

import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 女仆任务读世界规则的两条不变量。
 *
 * <p>本树做过配置所有权重构：{@code MaidConfig} 里的世界规则由 {@link ServerRuleConfig} 持有每世界的值，
 * 而它的读口对**未认领的键**会回落到裸 spec：
 * <pre>{@code Object active = activeValues.get(value); return active == null ? value.get() : active; }</pre>
 * 裸 spec 在集成服务端 tick 期没加载，一读就是
 * {@code IllegalStateException: Cannot get config value before config is loaded}。
 * 而 {@code EntityMaid.searchRadius()} 委派给**当前任务**，于是任何一个任务里的失配都会在寻路时崩服务端。</p>
 *
 * <p>这里有两个独立的坑：① 直接照抄基准（origin/1.21.1，那边没有这层重构）的
 * {@code MaidConfig.XXX.get()} 会崩；② 只改成 {@code ServerRuleConfig.get(...)} 仍会崩，
 * 因为新增的键还得同时登记进认领清单。两者都不会被启动门发现——启动门只验到「能进世界」，
 * 而崩溃需要一只女仆真的在跑对应任务。这两条断言把该判据机械化。</p>
 */
class ServerRuleReadContractTest {
    private static final Path SOURCE_ROOT = Path.of("..", "..", "src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid");
    private static final List<Path> TASK_DIRS = List.of(
            SOURCE_ROOT.resolve(Path.of("entity", "task")),
            SOURCE_ROOT.resolve(Path.of("compat", "gun", "common", "task")),
            SOURCE_ROOT.resolve(Path.of("compat", "gun", "tacz")));

    /** 经读口读世界规则：ServerRuleConfig.get(MaidConfig.X) */
    private static final Pattern ROUTED_READ =
            Pattern.compile("ServerRuleConfig\\.get\\(\\s*MaidConfig\\.([A-Z0-9_]+)\\s*\\)");
    /** 把 spec 交给 IRangedAttackTask.targetConditionsTest，它内部同样走读口 */
    private static final Pattern CONDITION_READ =
            Pattern.compile("targetConditionsTest\\([^)]*?MaidConfig\\.([A-Z0-9_]+)\\s*\\)");
    /** 裸读：绕过世界规则，既拿不到每世界的值，也可能直接抛异常 */
    private static final Pattern RAW_READ = Pattern.compile("MaidConfig\\.[A-Z0-9_]+\\.get\\(\\)");

    @TempDir
    Path temporaryDirectory;

    @BeforeEach
    void initialize() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        WorldRuleTestHarness.loadDefaults(temporaryDirectory);
    }

    @Test
    void everyWorldRuleATaskReadsIsClaimedByServerRuleConfig() throws Exception {
        Set<String> readKeys = new LinkedHashSet<>();
        int scanned = 0;
        for (Path source : taskSources()) {
            scanned++;
            String code = stripComments(Files.readString(source));
            collect(ROUTED_READ, code, readKeys);
            collect(CONDITION_READ, code, readKeys);
        }

        // 下限断言：识别依据一变就静默零覆盖，而零覆盖的测试永远是绿的
        assertTrue(scanned >= 10, "只扫到 " + scanned + " 个任务源文件，识别依据可能已失效");
        assertTrue(readKeys.size() >= 5, "只认出 " + readKeys.size() + " 个世界规则读点，识别依据可能已失效：" + readKeys);

        List<ModConfigSpec.ConfigValue<?>> claimed = ServerRuleConfig.values();
        List<String> unclaimed = new ArrayList<>();
        for (String fieldName : readKeys) {
            Field field = MaidConfig.class.getField(fieldName);
            Object value = field.get(null);
            if (!claimed.contains(value)) {
                unclaimed.add(fieldName);
            }
        }

        assertTrue(unclaimed.isEmpty(),
                "这些世界规则被任务读取，却不在 ServerRuleConfig.values() 的认领清单里"
                        + "——运行期会回落到未加载的裸 spec 并崩服：" + unclaimed);
    }

    @Test
    void tasksNeverReadWorldRulesRaw() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path source : taskSources()) {
            String code = stripComments(Files.readString(source));
            Matcher matcher = RAW_READ.matcher(code);
            while (matcher.find()) {
                violations.add(source.getFileName() + " -> " + matcher.group());
            }
        }
        assertTrue(violations.isEmpty(),
                "任务里出现裸的世界规则读取，必须改成 ServerRuleConfig.get(...)：" + violations);
    }

    private static List<Path> taskSources() throws IOException {
        List<Path> sources = new ArrayList<>();
        for (Path dir : TASK_DIRS) {
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.filter(p -> p.getFileName().toString().endsWith(".java")).forEach(sources::add);
            }
        }
        return sources;
    }

    private static void collect(Pattern pattern, String code, Set<String> into) {
        Matcher matcher = pattern.matcher(code);
        while (matcher.find()) {
            into.add(matcher.group(1));
        }
    }

    /** 注释可能紧贴代码，也可能是 javadoc；两种都要剥，否则注释里的示例会变成假阳性。 */
    private static String stripComments(String source) {
        return source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("(?m)//.*$", " ");
    }
}
