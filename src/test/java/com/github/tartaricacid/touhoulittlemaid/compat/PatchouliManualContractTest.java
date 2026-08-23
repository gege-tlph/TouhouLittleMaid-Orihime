package com.github.tartaricacid.touhoulittlemaid.compat;

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
    private static final Path ENTRIES = RESOURCES.resolve(
            "assets/touhou_little_maid/patchouli_books/memorizable_gensokyo/en_us/entries");
    private static final Path GENERATED_RECIPES = ROOT.resolve(
            "src/main/generated/data/touhou_little_maid/recipe/altar_recipe");
    private static final Path RECIPE_GENERATOR = JAVA.resolve(
            "com/github/tartaricacid/touhoulittlemaid/datagen/RecipeGenerator.java");
    private static final Pattern RECIPE_ID = Pattern.compile(
            "\\\"recipe_id\\\"\\s*:\\s*\\\"touhou_little_maid:altar_recipe/([^\\\"]+)\\\"");
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
    void everyAltarRecipePageReferencesARealGeneratedRecipe() throws IOException {
        Set<String> generated = new HashSet<>();
        try (Stream<Path> files = Files.list(GENERATED_RECIPES)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .forEach(path -> generated.add(path.getFileName().toString().replaceFirst("\\.json$", "")));
        }
        assertTrue(generated.size() >= 40, "generated altar recipe set unexpectedly small");

        Set<String> referenced = new HashSet<>();
        try (Stream<Path> files = Files.walk(ENTRIES)) {
            files.filter(path -> path.getFileName().toString().endsWith(".json")).forEach(path -> {
                try {
                    Matcher matcher = RECIPE_ID.matcher(Files.readString(path, StandardCharsets.UTF_8));
                    while (matcher.find()) {
                        referenced.add(matcher.group(1));
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
        assertTrue(referenced.size() >= 30, "Patchouli manual has too few altar recipe pages");
        String generator = Files.readString(RECIPE_GENERATOR, StandardCharsets.UTF_8);
        Set<String> missing = referenced.stream()
                .filter(id -> !generated.contains(id))
                .filter(id -> !generator.contains("\"" + id + "\""))
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(missing.isEmpty(),
                "Patchouli manual references an altar recipe with no generated/source definition: " + missing);
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
        try (Stream<Path> files = Files.walk(ENTRIES)) {
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
