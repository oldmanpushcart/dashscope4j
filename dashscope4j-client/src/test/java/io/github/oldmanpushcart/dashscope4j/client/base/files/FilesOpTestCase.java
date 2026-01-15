package io.github.oldmanpushcart.dashscope4j.client.base.files;

import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiAssertions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.SystemMessage;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

public class FilesOpTestCase implements LoadingEnv {

    private static final File file = new File("./test-data/image/red-cup.jpeg");

    private void assertFileMeta(FileMeta meta) {
        Assertions.assertNotNull(meta.identity());
        Assertions.assertNotNull(meta.name());
        Assertions.assertNotNull(meta.purpose());
        Assertions.assertNotNull(meta.uploadedAt());
    }

    @Test
    public void test$files$create() {
        final var meta = client.base().files().create(file.toURI(), FilesOpHelper.encodeFilename(file.getName()), Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();
        assertFileMeta(meta);
    }

    @Test
    public void test$files$delete() {

        final var filesOp = client.base().files();

        final var created = filesOp.create(file.toURI(), FilesOpHelper.encodeFilename(file.getName()), Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();

        final var ret = filesOp
                .delete(created.identity())
                .toCompletableFuture()
                .join();

        Assertions.assertTrue(ret);

    }

    @Test
    public void test$files$delete$file_not_exists() {

        final var filesOp = client.base().files();
        final var ret = filesOp
                .delete("fileid-not-exists")
                .toCompletableFuture()
                .join();
        Assertions.assertFalse(ret);
    }

    @Test
    public void test$files$detail() {

        final var filesOp = client.base().files();

        final var created = filesOp.create(file.toURI(), FilesOpHelper.encodeFilename(file.getName()), Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();

        final var detail = filesOp.detail(created.identity())
                .toCompletableFuture()
                .join();

        assertFileMeta(detail);

    }

    @Test
    public void test$files$detail$file_not_existed() {

        final var filesOp = client.base().files();

        final var detail = filesOp.detail("fileid-not-existed")
                .toCompletableFuture()
                .join();

        Assertions.assertNull(detail);

    }

    @Test
    public void test$files$flow() {

        final var filesOp = client.base().files();

        final var createdA = filesOp.create(file.toURI(), FilesOpHelper.encodeFilename(file.getName()), Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();

        final var createdB = filesOp.create(file.toURI(), FilesOpHelper.encodeFilename(file.getName()), Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();

        final var founds = FlowX.fromPublisher(filesOp.flow())
                .filter(meta -> meta.identity().equals(createdA.identity())
                        || meta.identity().equals(createdB.identity()))
                .map(FileMeta::identity)
                .blockingCollect(Collectors.toList());

        Assertions.assertEquals(2, founds.size());
        Assertions.assertTrue(founds.contains(createdA.identity()));
        Assertions.assertTrue(founds.contains(createdB.identity()));

    }

    @BeforeAll
    public static void cleanup() {

        final var filesOp = client.base().files();

        final var waitingCleanupList = FlowX.fromPublisher(filesOp.flow())
                .filter(meta -> FilesOpHelper.isEncodedFilename(meta.name()))
                .filter(meta -> {
                    final var updatedAt = FilesOpHelper.parseInstantFromEncodedFilename(meta.name());
                    return updatedAt.isBefore(Instant.now().minus(1, ChronoUnit.DAYS));
                })
                .blockingCollect(Collectors.toList());

        waitingCleanupList.stream()
                .map(FileMeta::identity)
                .forEach(identity -> filesOp.delete(identity)
                        .toCompletableFuture()
                        .join());

    }

    /*
     * file-fe-58febfa682b34d898b1693a6
     */
    @Disabled
    @Test
    public void debug() {

        final var filesOp = client.base().files();
        final var created = filesOp.create(new File("./test-data/document/P020210313315693279320.pdf"), Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();

        Assertions.assertNotNull(created);
        System.out.println(created);

    }

}
