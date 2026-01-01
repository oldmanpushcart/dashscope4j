package io.github.oldmanpushcart.dashscope4j.client.base.store;

import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.util.ProgressListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

public class StoreTestCase implements LoadingEnv {

    @Test
    public void test$upload$success() throws IOException {

        final ProgressListener checker = (bytesWritten, contentLength, done) -> {
            Assertions.assertTrue(bytesWritten <= contentLength);
            if (done) {
                Assertions.assertEquals(contentLength, bytesWritten);
            } else {
                Assertions.assertTrue(bytesWritten > 0);
                Assertions.assertTrue(bytesWritten < contentLength);
            }
        };

        final File file = Files.createTempFile("test", ".pdf").toFile();
        final URI uploaded = client.base().store().upload(file.toURI(), ChatModel.QWEN_TURBO, checker)
                .toCompletableFuture()
                .join();
        Assertions.assertEquals("dashscope-instant", uploaded.getHost());
        Assertions.assertEquals("oss", uploaded.getScheme());
    }

}
