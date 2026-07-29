package com.github.tartaricacid.touhoulittlemaid.compat;

import org.junit.jupiter.api.DisplayName;
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
 * <b>未安装 YSM 时，本模组必须完全照常工作</b>——这是 COMPAT.md 的可插拔铁律，也是项目所有者
 * 2026-07-28 明确的要求（YSM 侧更新时间不定，我们不能依赖它在场）。
 *
 * <p>本测试钉住的不是"功能对不对"，而是<b>那几个让单独加载安全的守卫还在不在</b>。
 * 防的不是今天写错，是几个月后被一行"顺手简化"删掉——那时编译、启动、单测全绿，
 * 只有装不装 YSM 的玩家会看到两种不同的世界。</p>
 *
 * <p>为什么用源码扫描而不是运行时断言：这些守卫全在客户端渲染路径上，纯 JUnit 环境里没有
 * 渲染器实例可构造；而 GameTest 是服务端，同样碰不到。源码扫描是唯一能自动化的角度。
 * 同款手法见 {@code PayloadRegistrationInvariantTest} 与 {@code RegistrationInvariantTest}。</p>
 */
@DisplayName("未安装 YSM 时的隔离契约")
class YsmStandaloneIsolationContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path MAIN = ROOT.resolve(Path.of("src", "main", "java", "com", "github",
            "tartaricacid", "touhoulittlemaid"));
    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /** compat/ysm 包自身不在扫描范围内：它整包只在 isInstalled 为真时才被触碰 */
    private static final String COMPAT_YSM_DIR = "compat" + java.io.File.separator + "ysm";

    @Test
    @DisplayName("渲染器注入必须双重守卫：未装 YSM 或钩子未赋值都要提前返回")
    void rendererInjectionIsDoubleGuarded() throws IOException {
        String src = activeSource(MAIN.resolve(Path.of("client", "renderer", "entity", "EntityMaidRenderer.java")));
        // 缺 isInstalled → 装了别的 mod 恰好给钩子赋值就会误接管；
        // 缺 == null → 未装 YSM 时 apply(null 钩子) 直接 NPE，女仆渲染整条崩。
        assertTrue(src.contains("!YsmCompat.isInstalled() || YSM_ENTITY_MAID_RENDERER == null"),
                "initYsmModelRenderer 必须同时守 isInstalled 与钩子非空后才提前返回");
    }

    @Test
    @DisplayName("渲染移交必须先确认渲染器实例存在，不能只看女仆的 isYsmModel 标志")
    void renderTakeoverRequiresRendererInstance() throws IOException {
        String src = activeSource(MAIN.resolve(Path.of("client", "renderer", "entity", "EntityMaidRenderer.java")));
        // 旧档里可能留着 isYsmModel=true 的女仆。未装 YSM 时 ysmMaidRenderer 恒为 null，
        // 只判标志就会 NPE；判了实例才会自然回落到 gecko / bedrock 渲染。
        assertTrue(src.contains("this.ysmMaidRenderer != null"),
                "submit 的 YSM 分支必须判 ysmMaidRenderer != null，不能只判 isYsmModel");
    }

    @Test
    @DisplayName("展示柜与雕像的 YSM 动画分支必须带 isInstalled")
    void displayCaseBranchesGuardOnInstalled() throws IOException {
        for (String name : new String[]{"GarageKitRenderer.java", "StatueRenderer.java"}) {
            String src = activeSource(MAIN.resolve(Path.of("client", "renderer", "blockentity", name)));
            assertTrue(src.contains("YsmCompat.isInstalled() && maid.isYsmModel()"),
                    name + " 的 tickCount 分支必须守 isInstalled——未装 YSM 时旧档标志不得改变动画时基");
        }
    }

    @Test
    @DisplayName("YSM 同步包只在装了 YSM 时才发送")
    void ysmSyncIsOnlySentWhenInstalled() throws IOException {
        String src = activeSource(MAIN.resolve(Path.of("entity", "passive", "EntityMaid.java")));
        assertTrue(src.contains("YsmCompat.isInstalled() && this.isYsmModel()"),
                "EntityMaid 的 YSM tick/同步块必须守 isInstalled");
    }

    @Test
    @DisplayName("compat/ysm 包外不得出现无守卫的 YSM 类型引用")
    void noUnguardedYsmReferencesOutsideCompatPackage() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                if (file.toString().contains(COMPAT_YSM_DIR)) {
                    continue;
                }
                String src = activeSource(file);
                // compat.ysm 的事件类型只允许在 import 与被守卫的调用里出现；
                // 这里只做一件事：凡引用了 compat.ysm 的**非事件**类型（YsmCompat 除外）就报出来，
                // 它们意味着有人把 YSM 的实现类型漏进了核心路径。
                if (src.contains("compat.ysm.") && !src.contains("compat.ysm.event.")
                        && !src.contains("compat.ysm.YsmCompat")) {
                    offenders.add(MAIN.relativize(file).toString());
                }
            }
        }
        assertTrue(offenders.isEmpty(),
                "以下文件把 compat/ysm 的实现类型引进了核心路径，未装 YSM 时有类加载风险：" + offenders);
    }

    /** 剥掉块注释与行注释，避免注释里的示例代码被当成真实接线（同 PayloadRegistrationInvariantTest 的手法） */
    private static String activeSource(Path file) throws IOException {
        String src = BLOCK_COMMENT.matcher(Files.readString(file)).replaceAll("");
        StringBuilder sb = new StringBuilder(src.length());
        for (String line : src.split("\n", -1)) {
            int idx = line.indexOf("//");
            sb.append(idx >= 0 ? line.substring(0, idx) : line).append('\n');
        }
        return sb.toString();
    }
}
