package com.github.tartaricacid.touhoulittlemaid;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 注册完整性不变量：**类写好了不等于会被执行**。
 *
 * <p>本仓库已实证 7 枚「纸面接口」——mixin/accessor 编译通过却没进 mixins.json（运行期 CCE 或功能静默失效）、
 * compat 插件缺 Fabric entrypoint（JEI/Jade 插件从未运行）、新增 GameTest 类漏登记 `fabric-gametest`
 * （编译、打包、启动全部正常，用例一个都没跑）。这三类失败的共同点是：
 * **只有 `build.gradle` 的排除行会自己报错，entrypoint 与 mixins.json 全静默。**
 *
 * <p>本测试把这类静默失败变成红灯。它只做纯文本/文件系统核对，不加载 Minecraft，因此在普通
 * `build` 里就会跑到，不必等 `runGametest`。
 */
class RegistrationInvariantTest {
    private static final String[] MIXIN_CONFIGS = {
            "touhou_little_maid.mixins.json",
            "touhou_little_maid_fabric.mixins.json",
    };

    private static final java.util.regex.Pattern BLOCK_COMMENT =
            java.util.regex.Pattern.compile("/\\*.*?\\*/", java.util.regex.Pattern.DOTALL);

    /** 匹配类声明上的 {@code implements ... ILittleMaid}；接口自身是 {@code interface}，不会命中。 */
    private static final java.util.regex.Pattern IMPLEMENTS_LITTLE_MAID =
            java.util.regex.Pattern.compile("\\bclass\\s+\\w+[^{;]*\\bimplements\\b[^{;]*\\bILittleMaid\\b");

    @Test
    void everyMixinClassInsideAConfiguredPackageIsRegistered() throws IOException {
        for (String configName : MIXIN_CONFIGS) {
            JsonObject config = readJson(resources().resolve(configName));
            String mixinPackage = config.get("package").getAsString();
            Path packageDir = sources().resolve(mixinPackage.replace('.', '/'));
            Set<String> registered = registeredMixins(config);

            for (String onDisk : classesUnder(packageDir)) {
                assertTrue(registered.contains(onDisk),
                        configName + " 未登记 " + mixinPackage + "." + onDisk
                                + "：mixin 类编译通过但不会被应用。要么把它加进该配置的 mixins/client/server，"
                                + "要么删除这个死文件。");
            }
        }
    }

    @Test
    void everyRegisteredMixinStillExists() throws IOException {
        for (String configName : MIXIN_CONFIGS) {
            JsonObject config = readJson(resources().resolve(configName));
            String mixinPackage = config.get("package").getAsString();
            Path packageDir = sources().resolve(mixinPackage.replace('.', '/'));

            for (String registered : registeredMixins(config)) {
                // 嵌套 mixin（Outer$Inner）与外层类同处一个源文件。
                String outer = registered.contains("$")
                        ? registered.substring(0, registered.indexOf('$'))
                        : registered;
                Path file = packageDir.resolve(outer.replace('.', '/') + ".java");
                assertTrue(Files.isRegularFile(file),
                        configName + " 登记了不存在的 " + mixinPackage + "." + registered
                                + "：mixins.json 是 required:true，条目对不上会在加载期直接崩。");
            }
        }
    }

    @Test
    void everyGameTestClassIsRegisteredAsAnEntrypoint() throws IOException {
        Set<String> registered = gameTestEntrypoints();

        for (Path file : findFiles(sources(), "*GameTest.java")) {
            String className = className(sources(), file);
            assertTrue(registered.contains(className),
                    "fabric.mod.json 的 fabric-gametest 未登记 " + className
                            + "：GameTest 类靠该 entrypoint 被发现，漏登记则编译、打包、启动全部正常，"
                            + "但 runGametest 一个用例都不会执行。");
        }
    }

    @Test
    void everyRegisteredGameTestStillExists() throws IOException {
        for (String className : gameTestEntrypoints()) {
            Path file = sources().resolve(className.replace('.', '/') + ".java");
            assertTrue(Files.isRegularFile(file),
                    "fabric.mod.json 登记了不存在的 GameTest 类 " + className);
        }
    }

    /**
     * {@code ILittleMaid} 实现是又一处静默注册面：不登记 {@code little_maid_extension}
     * 就永不被 {@code TouhouLittleMaid.EXTENSIONS} 发现，编译、打包、启动全部正常，
     * 只是它注册的任务 / 箱子类型 / 大脑扩展一个都不生效。
     */
    @Test
    void everyLittleMaidExtensionIsRegisteredAsAnEntrypoint() throws IOException {
        Set<String> registered = entrypoints("little_maid_extension");

        for (Path file : findFiles(sources(), "*.java")) {
            if (!IMPLEMENTS_LITTLE_MAID.matcher(activeSource(file)).find()) {
                continue;
            }
            String className = className(sources(), file);
            assertTrue(registered.contains(className),
                    "fabric.mod.json 的 little_maid_extension 未登记 " + className
                            + "：它实现了 ILittleMaid 却不会被扩展加载发现，注册的一切都不会生效。");
        }
    }

    @Test
    void everyRegisteredLittleMaidExtensionStillExists() throws IOException {
        for (String className : entrypoints("little_maid_extension")) {
            Path file = sources().resolve(className.replace('.', '/') + ".java");
            assertTrue(Files.isRegularFile(file),
                    "fabric.mod.json 登记了不存在的扩展类 " + className);
        }
    }

    private static Set<String> registeredMixins(JsonObject config) {
        Set<String> registered = new LinkedHashSet<>();
        for (String key : new String[]{"mixins", "client", "server"}) {
            JsonElement element = config.get(key);
            if (element == null || !element.isJsonArray()) {
                continue;
            }
            for (JsonElement entry : element.getAsJsonArray()) {
                registered.add(entry.getAsString());
            }
        }
        return registered;
    }

    private static Set<String> gameTestEntrypoints() throws IOException {
        return entrypoints("fabric-gametest");
    }

    private static Set<String> entrypoints(String key) throws IOException {
        JsonObject mod = readJson(resources().resolve("fabric.mod.json"));
        Set<String> registered = new LinkedHashSet<>();
        JsonElement entrypoints = mod.get("entrypoints");
        if (entrypoints != null && entrypoints.isJsonObject()) {
            JsonElement declared = entrypoints.getAsJsonObject().get(key);
            if (declared != null && declared.isJsonArray()) {
                for (JsonElement entry : (JsonArray) declared) {
                    registered.add(entry.isJsonObject()
                            ? entry.getAsJsonObject().get("value").getAsString()
                            : entry.getAsString());
                }
            }
        }
        return registered;
    }

    /** 剥掉块注释与行注释，免得注释里的示例声明被当成真实实现（同 YsmStandaloneIsolationContractTest 的手法）。 */
    private static String activeSource(Path file) throws IOException {
        String src = BLOCK_COMMENT.matcher(Files.readString(file, StandardCharsets.UTF_8)).replaceAll("");
        StringBuilder sb = new StringBuilder(src.length());
        for (String line : src.split("\n", -1)) {
            int idx = line.indexOf("//");
            sb.append(idx >= 0 ? line.substring(0, idx) : line).append('\n');
        }
        return sb.toString();
    }

    /** 包目录下每个 .java 的「相对包名」，例如 {@code accessor.ScreenAccessor}。 */
    private static Set<String> classesUnder(Path packageDir) throws IOException {
        Set<String> names = new LinkedHashSet<>();
        if (!Files.isDirectory(packageDir)) {
            return names;
        }
        for (Path file : findFiles(packageDir, "*.java")) {
            names.add(className(packageDir, file));
        }
        return names;
    }

    private static Set<Path> findFiles(Path root, String glob) throws IOException {
        var matcher = root.getFileSystem().getPathMatcher("glob:" + glob);
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(path -> matcher.matches(path.getFileName()))
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    private static String className(Path root, Path file) {
        String relative = root.relativize(file).toString();
        return relative.substring(0, relative.length() - ".java".length())
                .replace('\\', '.')
                .replace('/', '.');
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static Path sources() {
        return projectRoot().resolve("src/main/java");
    }

    private static Path resources() {
        return projectRoot().resolve("src/main/resources");
    }

    /** 从工作目录向上找 settings.gradle，避免依赖 Gradle 的 test working dir 设置。 */
    private static Path projectRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            if (Files.isRegularFile(current.resolve("settings.gradle"))
                    || Files.isRegularFile(current.resolve("settings.gradle.kts"))) {
                return current;
            }
            current = current.getParent();
        }
        throw new UncheckedIOException(new IOException("未能从 " + Path.of("").toAbsolutePath()
                + " 向上定位到项目根目录（settings.gradle）"));
    }
}
