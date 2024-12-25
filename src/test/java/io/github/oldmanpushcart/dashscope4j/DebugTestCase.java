package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.base.files.Purpose;
import okhttp3.MediaType;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.ExecutionException;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() throws InterruptedException, ExecutionException {
        final FileMeta meta = client.base().files().create(new File("./test-data/P020210313315693279320.pdf"), Purpose.FILE_EXTRACT)
                .toCompletableFuture()
                .join();
        System.out.println(meta);
    }

    @Test
    public void test$debug1() throws IOException {
        final File file = new File("./test-data/P020210313315693279320.pdf");
        final String ct = Files.probeContentType(file.toPath());
        final MediaType mt = MediaType.get(ct);
        System.out.println(mt);
    }

}
