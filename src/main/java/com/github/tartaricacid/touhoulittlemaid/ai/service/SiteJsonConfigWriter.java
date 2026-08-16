package com.github.tartaricacid.touhoulittlemaid.ai.service;

import com.github.tartaricacid.touhoulittlemaid.config.AtomicConfigFileWriter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import net.minecraft.util.GsonHelper;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 原子地发布站点改动，同时保住文件里**属于扩展的那部分 JSON**。
 *
 * <p>站点文件是共享的：别的 mod 可以往里加自己的站点（我们不认识的 {@code api_type}），
 * 也可以往我们的站点里加私有字段。写盘时把它们原样带过去，否则装了扩展的玩家每存一次
 * 配置就丢一次扩展数据，而且不会有任何提示。</p>
 */
public final class SiteJsonConfigWriter {
    private SiteJsonConfigWriter() {
    }

    public static void write(Path file, JsonObject next, Predicate<String> knownApiType) throws IOException {
        if (Files.isRegularFile(file)) {
            JsonObject previous;
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                previous = GsonHelper.parse(reader);
            }
            mergeExtensionFields(previous, next, knownApiType);
        }

        StringWriter output = new StringWriter();
        try (JsonWriter writer = new JsonWriter(output)) {
            writer.setSerializeNulls(false);
            writer.setIndent("  ");
            GsonHelper.writeValue(writer, next, Site.KEY_COMPARATOR);
        }
        AtomicConfigFileWriter.write(file, output.toString().getBytes(StandardCharsets.UTF_8), path -> {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                GsonHelper.parse(reader);
            }
        });
    }

    /**
     * 同一个 id 上，磁盘里是别人的扩展站点、要写的是我们认识的站点——这不是合并，是顶替。
     *
     * <p>抛出而不是「挑一个赢家」：调用方会把它降级成保存失败，管理员看得到；默默替换则会让
     * 扩展站点的配置在它自己那个 mod 缺席的一次启动里被改写成别的类型，装回来也回不去。</p>
     */
    public static final class ForeignSiteCollisionException extends IllegalStateException {
        ForeignSiteCollisionException(String siteId) {
            super("Refusing to overwrite extension-owned site '" + siteId
                    + "': its api_type is unknown to this installation");
        }
    }

    /**
     * 文件里**不属于本安装**的站点 id：值不是对象，或 {@code api_type} 未知。
     *
     * <p>它与各家读取时的「跳过」判据必须是同一条线——那边跳过的，这边必须收下，
     * 否则扩展站点会掉进「既不被读、也不被保留」的缝里，一次保存就没了。
     * 两处判据由 {@code SiteForeignIdPartitionTest} 钉住。</p>
     */
    public static Set<String> foreignIds(Path file, Predicate<String> knownApiType) throws IOException {
        if (!Files.isRegularFile(file)) {
            return Set.of();
        }
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            root = GsonHelper.parse(reader);
        }
        Set<String> foreign = new HashSet<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (isForeign(entry.getValue(), knownApiType)) {
                foreign.add(entry.getKey());
            }
        }
        return Set.copyOf(foreign);
    }

    static boolean isForeign(JsonElement value, Predicate<String> knownApiType) {
        if (!(value instanceof JsonObject site)) {
            return true;
        }
        JsonElement apiType = site.get(Site.API_TYPE);
        return apiType == null || !apiType.isJsonPrimitive() || !knownApiType.test(apiType.getAsString());
    }

    private static void mergeExtensionFields(JsonObject previous, JsonObject next,
                                             Predicate<String> knownApiType) {
        for (Map.Entry<String, JsonElement> entry : previous.entrySet()) {
            boolean foreign = isForeign(entry.getValue(), knownApiType);
            JsonElement nextElement = next.get(entry.getKey());
            // 我们要写的东西撞上了一个我们不认识的旧条目：拒绝，别顶替
            if (foreign && nextElement != null) {
                throw new ForeignSiteCollisionException(entry.getKey());
            }
            if (!(entry.getValue() instanceof JsonObject oldSite)) {
                next.add(entry.getKey(), entry.getValue().deepCopy());
                continue;
            }
            if (nextElement instanceof JsonObject nextSite) {
                // 同一个已知站点的字段级合并：扩展往我们的站点里加的私有字段要保住
                oldSite.entrySet().stream()
                        .filter(field -> !nextSite.has(field.getKey()))
                        .forEach(field -> nextSite.add(field.getKey(), field.getValue().deepCopy()));
                continue;
            }
            if (foreign) {
                next.add(entry.getKey(), oldSite.deepCopy());
            }
        }
    }
}
