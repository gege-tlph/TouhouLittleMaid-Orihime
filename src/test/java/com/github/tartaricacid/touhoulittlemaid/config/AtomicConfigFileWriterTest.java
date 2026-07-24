package com.github.tartaricacid.touhoulittlemaid.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AtomicConfigFileWriterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void successfulWritePublishesOneCompleteValidatedImage() throws Exception {
        Path target = targetWithOldContent();
        byte[] replacement = "{\"unicode\":\"灵梦\\n魔理沙\",\"unknown\":17}".getBytes(StandardCharsets.UTF_8);

        AtomicConfigFileWriter.write(target, replacement, path -> {
            String text = Files.readString(path, StandardCharsets.UTF_8);
            if (!text.startsWith("{") || !text.endsWith("}")) {
                throw new IOException("invalid JSON image");
            }
        });

        assertArrayEquals(replacement, Files.readAllBytes(target));
        assertEquals(sha256(replacement), sha256(Files.readAllBytes(target)));
        assertNoTemporaryFiles();
    }

    @Test
    void everyInjectedIoFailurePreservesTheLastGoodImage() throws Exception {
        byte[] replacement = new byte[20_000];
        java.util.Arrays.fill(replacement, (byte) 'N');
        for (AtomicConfigFileWriter.Stage failedStage : AtomicConfigFileWriter.Stage.values()) {
            Path target = targetWithOldContent();
            byte[] before = Files.readAllBytes(target);
            String beforeHash = sha256(before);

            assertThrows(IOException.class, () -> AtomicConfigFileWriter.write(
                    target, replacement, path -> Files.readAllBytes(path), stage -> {
                        if (stage == failedStage) {
                            throw new IOException("injected " + stage);
                        }
                    }));

            assertArrayEquals(before, Files.readAllBytes(target), failedStage.toString());
            assertEquals(beforeHash, sha256(Files.readAllBytes(target)), failedStage.toString());
            assertNoTemporaryFiles();
        }
    }

    @Test
    void validationFailureNeverPublishesTheTemporaryImage() throws Exception {
        Path target = targetWithOldContent();
        byte[] before = Files.readAllBytes(target);

        assertThrows(IOException.class, () -> AtomicConfigFileWriter.write(target,
                "truncated".getBytes(StandardCharsets.UTF_8),
                path -> {
                    throw new IOException("parse rejected truncated data");
                }));

        assertArrayEquals(before, Files.readAllBytes(target));
        assertNoTemporaryFiles();
    }

    @Test
    void lastGoodSnapshotRestoresATruncatedFormalFile() throws Exception {
        Path target = targetWithOldContent();
        byte[] valid = "{\"version\":\"new-complete\"}".getBytes(StandardCharsets.UTF_8);
        AtomicConfigFileWriter.write(target, valid, AtomicConfigFileWriterTest::validateJsonShape);
        String validHash = sha256(Files.readAllBytes(target));

        Files.writeString(target, "{\"version\":", StandardCharsets.UTF_8);
        assertEquals(true, AtomicConfigFileWriter.restoreLastGood(
                target, AtomicConfigFileWriterTest::validateJsonShape));
        assertEquals(validHash, sha256(Files.readAllBytes(target)));
        assertNoTemporaryFiles();
    }

    @Test
    void lockReadOnlyAndDiskFullEquivalentsKeepTheFormalFileComplete() throws Exception {
        for (String failure : new String[]{"target locked", "read only", "disk full"}) {
            Path target = targetWithOldContent();
            byte[] before = Files.readAllBytes(target);
            assertThrows(IOException.class, () -> AtomicConfigFileWriter.write(target,
                    "{\"version\":\"new\"}".getBytes(StandardCharsets.UTF_8),
                    AtomicConfigFileWriterTest::validateJsonShape,
                    stage -> {
                        if (stage == AtomicConfigFileWriter.Stage.BEFORE_MOVE) {
                            throw new IOException(failure);
                        }
                    }));
            assertArrayEquals(before, Files.readAllBytes(target), failure);
        }
        assertNoTemporaryFiles();
    }

    private Path targetWithOldContent() throws IOException {
        Path target = temporaryDirectory.resolve("config.json");
        Files.writeString(target, "{\"version\":\"old-complete\",\"preserved\":true}",
                StandardCharsets.UTF_8);
        return target;
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (var files = Files.list(temporaryDirectory)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    private static String sha256(byte[] content) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
    }

    private static void validateJsonShape(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        if (!text.startsWith("{") || !text.endsWith("}")) {
            throw new IOException("invalid JSON image");
        }
    }
}
