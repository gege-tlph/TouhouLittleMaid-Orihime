package com.github.tartaricacid.touhoulittlemaid.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalConfigMigrationTest {
    @TempDir
    Path temporaryDirectory;

    @BeforeAll
    static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    void existingGlobalFileIsNeverOverwrittenByLegacySources() throws Exception {
        var spec = GeneralConfig.getConfigSpec();
        Path global = temporaryDirectory.resolve(ConfigFileMigration.GLOBAL_FILE_NAME);
        Path legacy = temporaryDirectory.resolve("touhou_little_maid-common.toml");
        String existing = "marker = \"global-last-good\"\n";
        Files.writeString(global, existing, StandardCharsets.UTF_8);
        Files.writeString(legacy, "marker = \"legacy-must-not-win\"\n", StandardCharsets.UTF_8);
        String before = sha256(global);

        ConfigFileMigration.migrateGlobalFileIfNeeded(
                temporaryDirectory, GeneralConfig.values(), spec);

        assertEquals(before, sha256(global));
        assertEquals(existing, Files.readString(global, StandardCharsets.UTF_8));
    }

    @Test
    void newGlobalFileIsACompleteParseableAtomicImage() throws Exception {
        var spec = GeneralConfig.getConfigSpec();
        ConfigFileMigration.migrateGlobalFileIfNeeded(
                temporaryDirectory, GeneralConfig.values(), spec);
        Path global = temporaryDirectory.resolve(ConfigFileMigration.GLOBAL_FILE_NAME);

        assertTrue(Files.isRegularFile(global));
        CommentedConfig parsed = ConfigFileMigration.read(global);
        for (var value : GeneralConfig.values()) {
            assertTrue(parsed.contains(value.getPath()), ServerRuleConfig.key(value));
        }
        try (var files = Files.list(temporaryDirectory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    private static String sha256(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(path)));
    }
}
