package io.github.oldmanpushcart.dashscope4j.base.files;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

public class FilesTestCase extends ClientSupport {

    private final File file = new File("./test-data/IMG_0942.JPG");
    private final URI resource = file.toURI();

    private static void assertFileMeta(FileMeta meta) {
        Assertions.assertNotNull(meta);
        Assertions.assertNotNull(meta.identity());
        Assertions.assertNotNull(meta.name());
        Assertions.assertNotNull(meta.uploadedAt());
        Assertions.assertTrue(meta.size() > 0);
    }

    @Test
    public void test$file$create() {

        final String filename = encodingTestFilename(file.getName());
        final FileMeta created = client.base().files().create(resource, filename, Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();

        assertFileMeta(created);

        final FileMeta existed = client.base().files().detail(created.identity())
                .toCompletableFuture()
                .join();

        assertFileMeta(existed);

        Assertions.assertEquals(created, existed);

    }

    @Test
    public void test$file$delete() {

        final String filename = encodingTestFilename(file.getName());
        final FileMeta meta = client.base().files().create(resource, filename, Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();

        final boolean result = client.base().files().delete(meta.identity())
                .toCompletableFuture()
                .join();

        Assertions.assertTrue(result);
        final FileMeta detail = client.base().files().detail(meta.identity())
                .toCompletableFuture()
                .join();

        Assertions.assertNull(detail);

    }

    @Test
    public void test$file$delete$not_existed() {
        client.base().files().delete("fileid-not-existed")
                .thenAccept(Assertions::assertFalse)
                .toCompletableFuture()
                .join();
    }

    @Test
    public void test$file$detail$not_existed() {
        client.base().files().detail("fileid-not-existed")
                .thenAccept(Assertions::assertNull)
                .toCompletableFuture()
                .join();
    }

    @Test
    public void test$file$list() {

        final String filename = encodingTestFilename(file.getName());
        final FileMeta meta1 = client.base().files().create(resource, filename, Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();

        final FileMeta meta2 = client.base().files().create(resource, filename, Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();


        final AtomicInteger hit = new AtomicInteger();
        client.base().files().flow()
                .blockingSubscribe(meta -> {

                    if (meta.identity().equals(meta1.identity())) {
                        assertFileMeta(meta);
                        Assertions.assertEquals(meta1, meta);
                        hit.getAndIncrement();
                    } else if (meta.identity().equals(meta2.identity())) {
                        assertFileMeta(meta);
                        Assertions.assertEquals(meta2, meta);
                        hit.getAndIncrement();
                    }

                });

        Assertions.assertEquals(2, hit.get());

    }

    private static String encodingTestFilename(String filename) {
        if (isTestFilename(filename)) {
            return filename;
        }
        return String.format("%s_test_%s", System.currentTimeMillis(), filename);
    }

    private static boolean isTestFilename(String filename) {
        return filename.matches("\\d+_test_.*");
    }

    private static Instant parseInstantFromTestFilename(String filename) {
        if (!isTestFilename(filename)) {
            throw new IllegalArgumentException("Invalid filename: " + filename);
        }
        return Instant.ofEpochMilli(Long.parseLong(filename.split("_")[0]));
    }

    @BeforeAll
    public static void cleanup() {

        client.base().files().flow()
                .blockingSubscribe(meta -> {

                    if (!isTestFilename(meta.name())) {
                        return;
                    }

                    final Instant instant = parseInstantFromTestFilename(meta.name());
                    if (instant.isBefore(Instant.now().minus(Duration.ofDays(1)))) {
                        client.base().files().delete(meta.identity())
                                .toCompletableFuture()
                                .join();
                    }

                });

    }

}
