package io.github.oldmanpushcart.dashscope4j.client.base.store;

import io.github.oldmanpushcart.dashscope4j.client.LoadingEnv;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

public class StoreTestCase implements LoadingEnv {

    @Test
    public void test$upload$success() throws IOException {

        final File file = Files.createTempFile("test", ".pdf").toFile();
        final URI uploaded = client.base().store().upload(file.toURI(), ChatModel.QWEN_TURBO)
                .toCompletableFuture()
                .join();
        Assertions.assertEquals("dashscope-instant", uploaded.getHost());
        Assertions.assertEquals("oss", uploaded.getScheme());
    }

}
