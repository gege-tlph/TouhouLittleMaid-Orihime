package com.github.tartaricacid.touhoulittlemaid.compat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Static contract for the optional Patchouli manual and its compile-time wiring. */
class PatchouliManualContractTest {
    private static final Path ROOT = Path.of("..", "..");
    private static final Path RESOURCES = ROOT.resolve("src/main/resources");
    private static final Path JAVA = ROOT.resolve("src/main/java");
    private static final Path BOOK = RESOURCES.resolve(
            "data/touhou_little_maid/patchouli_books/memorizable_gensokyo/book.json");
    private static final Path CONTENT = RESOURCES.resolve(
            "assets/touhou_little_maid/patchouli_books/memorizable_gensokyo/en_us");
    private static final Path ENTRIES = CONTENT.resolve("entries");
    private static final Path CATEGORIES = CONTENT.resolve("categories");
    private static final Path GENERATED_RECIPE_ROOT = ROOT.resolve(
            "src/main/generated/data/touhou_little_maid/recipe");
    private static final Pattern RECIPE_ID = Pattern.compile(
            "\\\"(?:recipe_id|recipe)\\\"\\s*:\\s*\\\"(touhou_little_maid:[^\\\"]+)\\\"");
    private static final Pattern ITEM_REFERENCE = Pattern.compile(
            "\\\"(?:icon|item)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

    @Test
    void manualBookAndRuntimeHooksArePresent() throws IOException {
        assertTrue(Files.isRegularFile(BOOK), "Patchouli book.json is missing");
        String book = Files.readString(BOOK, StandardCharsets.UTF_8);
        assertTrue(book.contains("memorizable_gensokyo"), "book.json points at the wrong book id");
        assertTrue(Files.isRegularFile(JAVA.resolve(
                "com/github/tartaricacid/touhoulittlemaid/compat/patchouli/AltarRecipeComponent.java")));
        assertTrue(Files.isRegularFile(JAVA.resolve(
                "com/github/tartaricacid/touhoulittlemaid/compat/patchouli/MultiblockRegistry.java")));
        String tabs = Files.readString(JAVA.resolve(
                "com/github/tartaricacid/touhoulittlemaid/init/InitCreativeTabs.java"), StandardCharsets.UTF_8);
        assertTrue(tabs.contains("PatchouliCompat.getBookStack"),
                "creative tab does not expose the Patchouli book when installed");
    }

    @Test
    void everyRecipePageReferencesARealGeneratedRecipe() throws IOException {
        Set<String> generated = new HashSet<>();
        try (Stream<Path> files = Files.walk(GENERATED_RECIPE_ROOT)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> {
                        Path relative = GENERATED_RECIPE_ROOT.relativize(path);
                        generated.add(relative.toString().replace('\\', '/').replaceFirst("\\.json$", ""));
                    });
        }
        assertTrue(generated.size() >= 45, "generated recipe set unexpectedly small");

        Set<String> referenced = new HashSet<>();
        try (Stream<Path> files = Files.walk(ENTRIES)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                try {
                    Matcher matcher = RECIPE_ID.matcher(Files.readString(path, StandardCharsets.UTF_8));
                    while (matcher.find()) {
                        referenced.add(matcher.group(1).substring("touhou_little_maid:".length()));
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        assertTrue(referenced.size() >= 30, "Patchouli manual has too few recipe pages");
        Set<String> missing = referenced.stream()
                .filter(id -> !generated.contains(id))
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(missing.isEmpty(),
                "Patchouli manual references a recipe with no generated data file: " + missing);
    }

    @Test
    void patchouliEntriesUseRegisteredTlmItems() throws IOException {
        String itemsSource = Files.readString(JAVA.resolve(
                "com/github/tartaricacid/touhoulittlemaid/init/InitItems.java"), StandardCharsets.UTF_8);
        Set<String> registered = new HashSet<>();
        Matcher registrations = Pattern.compile("register\\(\\\"([^\\\"]+)\\\"").matcher(itemsSource);
        while (registrations.find()) {
            registered.add(registrations.group(1));
        }

        Set<String> missing = new HashSet<>();
        try (Stream<Path> files = Files.walk(CONTENT)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                try {
                    Matcher references = ITEM_REFERENCE.matcher(Files.readString(path, StandardCharsets.UTF_8));
                    while (references.find()) {
                        for (String value : references.group(1).split(",")) {
                            if (value.startsWith("#") || !value.startsWith("touhou_little_maid:")) {
                                continue;
                            }
                            String id = value.substring("touhou_little_maid:".length());
                            if (!registered.contains(id)) {
                                missing.add(value + " in " + path);
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        assertTrue(missing.isEmpty(),
                "Patchouli manual references an unregistered TLM item: " + missing);
    }

    @Test
    void manualContentReferencesExistingCategoriesImagesAndTranslations() throws IOException {
        Set<String> categoryIds = new HashSet<>();
        try (Stream<Path> files = Files.list(CATEGORIES)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> categoryIds.add("touhou_little_maid:"
                            + path.getFileName().toString().replaceFirst("\\.json$", "")));
        }
        assertTrue(categoryIds.size() >= 3, "Patchouli category set unexpectedly small");

        JsonObject english = readJson(RESOURCES.resolve(
                "assets/touhou_little_maid/lang/en_us.json")).getAsJsonObject();
        JsonObject chinese = readJson(RESOURCES.resolve(
                "assets/touhou_little_maid/lang/zh_cn.json")).getAsJsonObject();
        Set<String> missing = new HashSet<>();
        Set<String> translationKeys = new HashSet<>();

        collectTranslationKeys(readJson(BOOK), translationKeys);
        try (Stream<Path> files = Files.walk(CONTENT)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                try {
                    JsonElement json = readJson(path);
                    collectTranslationKeys(json, translationKeys);
                    if (path.startsWith(ENTRIES)) {
                        String category = json.getAsJsonObject().get("category").getAsString();
                        if (!categoryIds.contains(category)) {
                            missing.add("category " + category + " in " + path);
                        }
                    }
                    collectMissingImages(json, path, missing);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }

        for (String key : translationKeys) {
            if (!english.has(key)) {
                missing.add("en_us translation " + key);
            }
            if (!chinese.has(key)) {
                missing.add("zh_cn translation " + key);
            }
        }
        assertTrue(translationKeys.size() >= 130, "Patchouli translation key set unexpectedly small");
        assertTrue(missing.isEmpty(), "Patchouli manual has broken content references: " + missing);
    }

    private static JsonElement readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
    }

    private static void collectTranslationKeys(JsonElement element, Set<String> keys) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (value.startsWith("patchouli.touhou_little_maid.book.")) {
                keys.add(value);
            }
            return;
        }
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectTranslationKeys(child, keys));
        } else if (element.isJsonObject()) {
            element.getAsJsonObject().entrySet()
                    .forEach(entry -> collectTranslationKeys(entry.getValue(), keys));
        }
    }

    private static void collectMissingImages(JsonElement element, Path source, Set<String> missing) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectMissingImages(child, source, missing));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("images")) {
            object.getAsJsonArray("images").forEach(image -> {
                String[] id = image.getAsString().split(":", 2);
                Path asset = RESOURCES.resolve("assets").resolve(id[0]).resolve(id[1]);
                if (!Files.isRegularFile(asset)) {
                    missing.add("image " + image.getAsString() + " in " + source);
                }
            });
        }
        object.entrySet().forEach(entry -> collectMissingImages(entry.getValue(), source, missing));
    }

    @Test
    void unsupportedCompatStubsAreNotRegisteredAsActiveAdapters() throws IOException {
        String registry = Files.readString(JAVA.resolve(
                "com/github/tartaricacid/touhoulittlemaid/init/registry/CompatRegistry.java"), StandardCharsets.UTF_8);
        assertFalse(registry.contains("TBackpackCompat::init"),
                "Traveler's Backpack stub must not be advertised as active compatibility");
        assertFalse(registry.contains("ImmersiveMelodiesServerCompat::init"),
                "Immersive Melodies server stub must not be advertised as active compatibility");
        String mixins = Files.readString(RESOURCES.resolve("touhou_little_maid.mixins.json"), StandardCharsets.UTF_8);
        assertFalse(mixins.contains("MobInteractMixin"),
                "empty Immersive Melodies interaction mixin must not be loaded");
    }
}
