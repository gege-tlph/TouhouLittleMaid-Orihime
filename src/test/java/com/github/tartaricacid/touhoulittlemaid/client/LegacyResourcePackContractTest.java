package com.github.tartaricacid.touhoulittlemaid.client;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 内置「TLM Legacy Pack」这条链的四道闸。
 *
 * <p><b>它守的是什么</b>：一个随模组发布的可选资源包（默认关，开了换回旧版模型与贴图）。
 * 宿主迁移时把类与整个 {@code legacy_pack/} 目录删了，只留下两个 lang 键 ——
 * 「载体还在、行为没了」的典型，本树已补回。</p>
 *
 * <p><b>为什么四条都必要</b>：这条链上每一处失败都是**静默**的 ——
 * ① 不在客户端入口注册 → 包不出现在列表，没有任何报错；
 * ② 常量里的 id 与磁盘目录名对不上 → 同样只是包消失（Fabric 按
 * {@code "resourcepacks/" + id.getPath()} 找，找不到就当没有）；
 * ③ 包体被误删/清空 → 列表里有个空包；
 * ④ {@code pack_format} 与本版客户端脱节 → 包被标成「旧版 / 不兼容」而玩家以为是自己装错了。</p>
 */
class LegacyResourcePackContractTest {
    /** 测试的 workingDir 是 build/test-working，回两级才是项目根。 */
    private static final Path PROJECT_ROOT = Path.of("..", "..");
    private static final Path REGISTRAR_SOURCE = PROJECT_ROOT.resolve(
            "src/main/java/com/github/tartaricacid/touhoulittlemaid/client/init/InitLegacyResourcePack.java");
    private static final Path CLIENT_ENTRYPOINT_SOURCE = PROJECT_ROOT.resolve(
            "src/main/java/cn/sh1rocu/touhoulittlemaid/client/TouhouLittleMaidFabricClient.java");
    private static final Path RESOURCE_PACKS_ROOT = PROJECT_ROOT.resolve("src/main/resources/resourcepacks");
    private static final Path LANG_EN_US = PROJECT_ROOT.resolve(
            "src/main/resources/assets/touhou_little_maid/lang/en_us.json");

    private static final Pattern PACK_DIR_NAME = Pattern.compile(
            "String\\s+PACK_DIR_NAME\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern TITLE_KEY = Pattern.compile(
            "String\\s+TITLE_KEY\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern PACK_FORMAT = Pattern.compile("\"pack_format\"\\s*:\\s*(\\d+)");

    /** 不在客户端入口调用，这个包就永远不会出现在资源包列表里，而且不会报任何错。 */
    @Test
    void registrarIsCalledFromTheClientEntrypoint() throws IOException {
        String source = stripComments(Files.readString(CLIENT_ENTRYPOINT_SOURCE, StandardCharsets.UTF_8));
        assertTrue(source.contains("InitLegacyResourcePack.register()"),
                "客户端入口没有调用 InitLegacyResourcePack.register() —— 内置资源包会静默地不出现在列表里");
    }

    /**
     * 常量里的 id 路径必须与磁盘上的目录名一致。
     *
     * <p>Fabric 按 {@code "resourcepacks/" + id.getPath()} 定位包体（javap -c 实证），
     * 对不上时既不报错也不警告，只是包不存在。</p>
     */
    @Test
    void packIdPathMatchesTheDirectoryOnDisk() throws IOException {
        String dirName = readConstant(PACK_DIR_NAME, "PACK_DIR_NAME");
        Path packDir = RESOURCE_PACKS_ROOT.resolve(dirName);
        assertTrue(Files.isDirectory(packDir),
                "PACK_DIR_NAME 是 \"" + dirName + "\"，但 " + packDir + " 不存在 —— "
                + "Fabric 按 resourcepacks/<id path> 找包，对不上时包只是静默消失");
        assertTrue(Files.isRegularFile(packDir.resolve("pack.mcmeta")),
                packDir + " 里没有 pack.mcmeta，这不是一个合法的资源包");
    }

    /** 包体不能是空壳：它的价值就在于真的覆盖了本树现存的那些资源。 */
    @Test
    void packActuallyOverridesLiveResources() throws IOException {
        String dirName = readConstant(PACK_DIR_NAME, "PACK_DIR_NAME");
        Path packDir = RESOURCE_PACKS_ROOT.resolve(dirName);
        Path liveAssets = PROJECT_ROOT.resolve("src/main/resources");

        List<String> overriding = new ArrayList<>();
        int total = 0;
        try (Stream<Path> paths = Files.walk(packDir)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                total++;
                String rel = packDir.relativize(path).toString().replace('\\', '/');
                if (!rel.startsWith("assets/")) {
                    continue;
                }
                if (Files.exists(liveAssets.resolve(rel))) {
                    overriding.add(rel);
                }
            }
        }
        // 活性判据与结论正交：文件总数与「其中多少个真覆盖」分开看守，
        // 空目录与「走错了目录」在结论上会一模一样
        assertTrue(total >= 100, "只在包里找到 " + total + " 个文件，包体像是被裁过或路径走错了");
        assertTrue(overriding.size() >= 90,
                "包里只有 " + overriding.size() + " 个文件真的覆盖了本树现存资源，"
                + "剩下的都是孤儿 —— 这个包已经失去意义，或者本树资源大改过需要重新裁定");
    }

    /**
     * {@code pack_format} 必须等于**本版客户端的资源包格式**，且描述与标题都要落在真实的 lang 键上。
     *
     * <p>⚠️ <b>这条判据修过一次，原来那条是错的。</b>它原先断言「与主包 pack_format 一致」，
     * 而主包的值是宿主/上游有意保留的旧值——**mod 自带资源不走兼容性检查，内置可选包走**。
     * 于是包在资源包列表里被标成「旧版」，而契约测试全绿。**2026-08-18 用户实机报出**。
     * 正确的判据只能来自本版客户端自己：Minecraft jar 里的 {@code version.json}，
     * 它随版本自动跟走，不需要人记得改。</p>
     */
    @Test
    void packFormatMatchesThisClientVersionAndLangKeysAreConsumed() throws IOException {
        String dirName = readConstant(PACK_DIR_NAME, "PACK_DIR_NAME");
        String mcmeta = Files.readString(
                RESOURCE_PACKS_ROOT.resolve(dirName).resolve("pack.mcmeta"), StandardCharsets.UTF_8);

        assertEquals(clientResourcePackFormat(), readGroup(PACK_FORMAT, mcmeta, "legacy 包 pack_format"),
                "legacy 包的 pack_format 与本版客户端不符 —— 它会被标成「旧版 / 不兼容」，"
                + "而玩家只会以为是自己装错了");

        String lang = Files.readString(LANG_EN_US, StandardCharsets.UTF_8);
        String titleKey = readConstant(TITLE_KEY, "TITLE_KEY");
        assertTrue(lang.contains("\"" + titleKey + "\""),
                "标题 lang 键 " + titleKey + " 不在 en_us.json 里");

        String descKey = titleKey.replaceAll("\\.title$", ".desc");
        assertTrue(lang.contains("\"" + descKey + "\""),
                "描述 lang 键 " + descKey + " 不在 en_us.json 里");
        assertTrue(mcmeta.contains(descKey),
                "pack.mcmeta 的 description 没有引用 " + descKey
                + " —— 那个键会重新变成没有消费者的孤儿（本包补回来的理由之一正是消灭这种孤儿）");
    }

    /**
     * 本版客户端的资源包格式，取自 Minecraft jar 里的 {@code version.json}。
     *
     * <p>**故意不读 {@code SharedConstants}**：那要触发它的类初始化（需要 bootstrap），
     * 而这份 json 是同一份权威数据的静态形态，直接从测试 classpath 读即可，且随版本自动跟走。</p>
     */
    private static String clientResourcePackFormat() throws IOException {
        try (InputStream in = LegacyResourcePackContractTest.class.getResourceAsStream("/version.json")) {
            assertNotNull(in, "classpath 上没有 version.json —— 取不到本版客户端的资源包格式，"
                    + "本条断言会退化成恒真");
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            Matcher m = Pattern.compile("\"resource_major\"\\s*:\\s*(\\d+)").matcher(json);
            assertTrue(m.find(), "version.json 里没有 pack_version.resource_major —— 抽取正则已失效");
            return m.group(1);
        }
    }

    private static String readConstant(Pattern pattern, String name) throws IOException {
        String source = stripComments(Files.readString(REGISTRAR_SOURCE, StandardCharsets.UTF_8));
        return readGroup(pattern, source, name);
    }

    private static String readGroup(Pattern pattern, String text, String name) {
        Matcher matcher = pattern.matcher(text);
        assertTrue(matcher.find(), "没能从源码里读出 " + name + " —— 抽取正则已失效，本条断言会退化成恒真");
        return matcher.group(1);
    }

    /** 剥注释后去掉全部空白比较字符流：注释可能紧贴代码，按空白分词会产生假阳性。 */
    private static String stripComments(String source) {
        String noBlock = source.replaceAll("(?s)/\\*.*?\\*/", " ");
        return noBlock.replaceAll("(?m)//.*$", " ");
    }
}
