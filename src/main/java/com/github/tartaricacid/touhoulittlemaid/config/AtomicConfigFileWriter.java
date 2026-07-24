package com.github.tartaricacid.touhoulittlemaid.config;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Writes a validated complete config image without ever streaming into the live file. */
public final class AtomicConfigFileWriter {
    private static final int WRITE_CHUNK_SIZE = 8 * 1024;

    private AtomicConfigFileWriter() {
    }

    public static void write(Path target, byte[] content, Validator validator) throws IOException {
        write(target, content, validator, stage -> {
        });
    }

    static void write(Path target, byte[] content, Validator validator, FaultInjector faultInjector)
            throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent == null) {
            throw new IOException("Config target has no parent directory: " + target);
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            faultInjector.at(Stage.BEFORE_WRITE);
            try (FileChannel channel = FileChannel.open(temporary,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(content);
                boolean firstChunk = true;
                while (buffer.hasRemaining()) {
                    int oldLimit = buffer.limit();
                    buffer.limit(Math.min(buffer.position() + WRITE_CHUNK_SIZE, oldLimit));
                    while (buffer.hasRemaining()) {
                        channel.write(buffer);
                    }
                    buffer.limit(oldLimit);
                    if (firstChunk) {
                        firstChunk = false;
                        faultInjector.at(Stage.AFTER_FIRST_CHUNK);
                    }
                }
                faultInjector.at(Stage.BEFORE_FORCE);
                channel.force(true);
                faultInjector.at(Stage.BEFORE_CLOSE);
            }
            validator.validate(temporary);
            prepareLastGood(target, content, validator);
            faultInjector.at(Stage.BEFORE_MOVE);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            moved = true;
            try {
                write(backupPath(target), content, validator, stage -> {
                }, false);
            } catch (IOException | RuntimeException ignored) {
                // The live file is already a complete validated image. Keep the previous
                // backup rather than reporting a false save failure after publication.
            }
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    public static boolean restoreLastGood(Path target, Validator validator) {
        Path backup = backupPath(target);
        if (!Files.isRegularFile(backup)) {
            return false;
        }
        try {
            validator.validate(backup);
            byte[] content = Files.readAllBytes(backup);
            write(target, content, validator, stage -> {
            }, false);
            return true;
        } catch (IOException | RuntimeException exception) {
            return false;
        }
    }

    private static void prepareLastGood(Path target, byte[] newContent, Validator validator) throws IOException {
        Path backup = backupPath(target);
        if (Files.isRegularFile(target)) {
            try {
                validator.validate(target);
                write(backup, Files.readAllBytes(target), validator, stage -> {
                }, false);
            } catch (IOException | RuntimeException invalidTarget) {
                if (!Files.isRegularFile(backup)) {
                    throw new IOException("Existing config is invalid and has no last-good backup: " + target,
                            invalidTarget);
                }
            }
        } else if (!Files.isRegularFile(backup)) {
            write(backup, newContent, validator, stage -> {
            }, false);
        }
    }

    private static Path backupPath(Path target) {
        return target.resolveSibling(target.getFileName() + ".last-good");
    }

    private static void write(Path target, byte[] content, Validator validator,
                              FaultInjector faultInjector, boolean prepareBackup) throws IOException {
        if (prepareBackup) {
            write(target, content, validator, faultInjector);
            return;
        }
        Path parent = target.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, target.getFileName().toString(), ".tmp");
        boolean moved = false;
        try {
            try (FileChannel channel = FileChannel.open(temporary,
                    StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(content);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            validator.validate(temporary);
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    @FunctionalInterface
    public interface Validator {
        void validate(Path path) throws IOException;
    }

    @FunctionalInterface
    interface FaultInjector {
        void at(Stage stage) throws IOException;
    }

    enum Stage {
        BEFORE_WRITE,
        AFTER_FIRST_CHUNK,
        BEFORE_FORCE,
        BEFORE_CLOSE,
        BEFORE_MOVE
    }
}
