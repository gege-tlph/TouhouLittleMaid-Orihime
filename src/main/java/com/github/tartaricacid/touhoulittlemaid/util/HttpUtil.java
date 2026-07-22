package com.github.tartaricacid.touhoulittlemaid.util;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProgressListener;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class HttpUtil {
    private static final int CONNECT_TIMEOUT_MILLIS = 15_000;
    private static final int READ_TIMEOUT_MILLIS = 60_000;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder()
            .setDaemon(true).setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER))
            .setNameFormat("TLM Downloader %d").build();
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool(THREAD_FACTORY);
    private static final ListeningExecutorService DOWNLOAD_EXECUTOR = MoreExecutors.listeningDecorator(EXECUTOR_SERVICE);

    private HttpUtil() {
    }

    public static CompletableFuture<?> downloadTo(File saveFile, URL packUrl, Map<String, String> requestProperties, int maxSize, @Nullable ProgressListener listener, Proxy proxy) {
        return CompletableFuture.runAsync(() -> {
            HttpURLConnection connection = null;
            File partialFile = new File(saveFile.getPath() + ".part");
            if (listener != null) {
                listener.progressStart(Component.translatable("resourcepack.downloading"));
                listener.progressStage(Component.translatable("resourcepack.requesting"));
            }

            try {
                byte[] bytes = new byte[4096];
                connection = (HttpURLConnection) packUrl.openConnection(proxy);
                connection.setInstanceFollowRedirects(true);
                connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
                connection.setReadTimeout(READ_TIMEOUT_MILLIS);
                int headerIndex = 0;
                int headerCount = requestProperties.size();

                for (Map.Entry<String, String> entry : requestProperties.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                    if (listener != null) {
                        listener.progressStagePercentage(headerCount == 0 ? 100 : ++headerIndex * 100 / headerCount);
                    }
                }

                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) {
                    throw new IOException("HTTP " + responseCode + " while downloading " + packUrl);
                }

                long requestSize = connection.getContentLengthLong();
                if (listener != null) {
                    listener.progressStage(Component.translatable("resourcepack.progress", String.format(Locale.ROOT, "%.2f", requestSize / 1_000_000.0)));
                }
                if (maxSize > 0 && requestSize > maxSize) {
                    throw new IOException("Filesize is bigger than maximum allowed (file is " + requestSize + ", limit is " + maxSize + ")");
                }

                File parent = saveFile.getParentFile();
                if (parent != null) {
                    Files.createDirectories(parent.toPath());
                }
                Files.deleteIfExists(partialFile.toPath());

                long downloadedSize = 0;
                try (InputStream inputStream = connection.getInputStream();
                     OutputStream outputStream = new DataOutputStream(new FileOutputStream(partialFile))) {
                    int readSize;
                    while ((readSize = inputStream.read(bytes)) >= 0) {
                        downloadedSize += readSize;
                        if (listener != null && requestSize > 0) {
                            listener.progressStagePercentage((int) (downloadedSize * 100 / requestSize));
                        }
                        if (maxSize > 0 && downloadedSize > maxSize) {
                            throw new IOException("Filesize was bigger than maximum allowed (got >= " + downloadedSize + ", limit was " + maxSize + ")");
                        }
                        if (Thread.currentThread().isInterrupted()) {
                            throw new InterruptedIOException("TLM download interrupted");
                        }
                        outputStream.write(bytes, 0, readSize);
                    }
                }

                Files.move(partialFile.toPath(), saveFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception exception) {
                LOGGER.error("Failed to download file from {}", packUrl, exception);
                try {
                    Files.deleteIfExists(partialFile.toPath());
                } catch (IOException cleanupError) {
                    LOGGER.warn("Failed to clean partial download {}", partialFile, cleanupError);
                }
                if (connection != null) {
                    InputStream errorStream = connection.getErrorStream();
                    if (errorStream != null) {
                        try (errorStream) {
                            LOGGER.error("HTTP response error: {}", IOUtils.toString(errorStream, StandardCharsets.UTF_8));
                        } catch (IOException ioexception) {
                            LOGGER.error("Failed to read response from server", ioexception);
                        }
                    }
                }
                throw new CompletionException(exception);
            } finally {
                if (listener != null) {
                    listener.stop();
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }, DOWNLOAD_EXECUTOR);
    }
}
