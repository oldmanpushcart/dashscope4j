package io.github.oldmanpushcart.dashscope4j.client.base.files;

import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

public class FilesOpTestCase implements LoadingEnv {

    private void assertFileMeta(FileMeta meta) {
        Assertions.assertNotNull(meta.identity());
        Assertions.assertNotNull(meta.name());
        Assertions.assertNotNull(meta.purpose());
        Assertions.assertNotNull(meta.uploadedAt());
    }

    @Test
    public void test$files$create() throws IOException {
        final var tempF = File.createTempFile("files-op-test-file", ".txt");
        tempF.deleteOnExit();
        final var meta = client.base().files().create(tempF.toURI(), FilesOpHelper.encodeFilename(tempF.getName()), Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();
        assertFileMeta(meta);
    }

    @BeforeAll
    public static void cleanup() {

        final var filesOp = client.base().files();

        final var waitingCleanupList = FlowX.fromPublisher(filesOp.flow())
//                .filter(meta -> FilesOpHelper.isEncodedFilename(meta.name()))
//                .filter(meta-> {
//                    final var updatedAt = FilesOpHelper.parseInstantFromEncodedFilename(meta.name());
//                    return updatedAt.isBefore(Instant.now().minus(1, ChronoUnit.DAYS));
//                })
                .blockingCollect(Collectors.toList());

        waitingCleanupList.stream()
                .map(FileMeta::identity)
                .forEach(identity-> {
                    filesOp.delete(identity)
                            .toCompletableFuture()
                            .join();
                });

    }

}
