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
import java.util.Map;
import java.util.function.Predicate;

/** Preserves extension-owned JSON while atomically publishing known site changes. */
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

    private static void mergeExtensionFields(JsonObject previous, JsonObject next,
                                             Predicate<String> knownApiType) {
        for (Map.Entry<String, JsonElement> entry : previous.entrySet()) {
            if (!(entry.getValue() instanceof JsonObject oldSite)) {
                next.add(entry.getKey(), entry.getValue().deepCopy());
                continue;
            }
            JsonElement nextElement = next.get(entry.getKey());
            if (nextElement instanceof JsonObject nextSite) {
                oldSite.entrySet().stream()
                        .filter(field -> !nextSite.has(field.getKey()))
                        .forEach(field -> nextSite.add(field.getKey(), field.getValue().deepCopy()));
                continue;
            }
            JsonElement apiType = oldSite.get(Site.API_TYPE);
            if (apiType == null || !knownApiType.test(apiType.getAsString())) {
                next.add(entry.getKey(), oldSite.deepCopy());
            }
        }
    }
}
