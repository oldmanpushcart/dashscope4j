package io.github.oldmanpushcart.dashscope4j.client.internal.util;

import io.github.oldmanpushcart.dashscope4j.client.internal.util.codec.AsyncFileBase64Encoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

public class DataURI {

    private final Supplier<CompletionStage<Content>> supplier;

    public DataURI(Supplier<CompletionStage<Content>> supplier) {
        this.supplier = supplier;
    }

    public URI toURI() {
        return asyncToURI()
                .toCompletableFuture()
                .join();
    }

    public CompletionStage<URI> asyncToURI() {
        return supplier.get()
                .thenApply(content -> {
                    final var mime = null == content.mime() ? "" : content.mime();
                    return "data:%s;base64,%s".formatted(mime, content.base64());
                })
                .thenApply(URI::create);
    }

    public record Content(String mime, String base64) {

    }

    public static DataURI from(File file) {
        return from(file.toPath());
    }

    public static DataURI from(Path path) {
        return new DataURI(() -> {
            try {
                final var mime = Files.probeContentType(path);
                return AsyncFileBase64Encoder.encode(path)
                        .thenApply(base64 -> new Content(mime, base64));
            } catch (Throwable ex) {
                return CompletableFuture.failedFuture(ex);
            }
        });
    }

    public static DataURI from(String mime, byte[] bytes) {
        return from(mime, ByteBuffer.wrap(bytes));
    }

    public static DataURI from(String mime, ByteBuffer buffer) {
        return new DataURI(() -> {
            try (final var baos = new ByteArrayOutputStream()) {
                final var bytes = new byte[1024];
                while (buffer.hasRemaining()) {
                    int len = Math.min(buffer.remaining(), bytes.length);
                    buffer.get(bytes, 0, len);
                    baos.write(bytes, 0, len);
                }
                final var base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                return CompletableFuture.completedFuture(new Content(mime, base64));
            } catch (Throwable ex) {
                return CompletableFuture.failedFuture(ex);
            }
        });
    }

}
