package io.github.oldmanpushcart.dashscope4j.base.store;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;

public class StoreTestCase extends ClientSupport {

    @Test
    public void test$upload$success() throws IOException {
        final File file = Files.createTempFile("test", ".pdf").toFile();
        final URI uploaded = client.base().store().upload(file.toURI(), ChatModel.QWEN_TURBO)
                .toCompletableFuture()
                .join();
        Assertions.assertEquals("dashscope-instant", uploaded.getHost());
        Assertions.assertEquals("oss", uploaded.getScheme());
    }

    @Test
    public void test$upload$cached() throws IOException {
        final File file = Files.createTempFile("test", ".pdf").toFile();
        final URI uploadedForA = client.base().store().upload(file.toURI(), ChatModel.QWEN_TURBO)
                .toCompletableFuture()
                .join();
        final URI uploadedForB = client.base().store().upload(file.toURI(), ChatModel.QWEN_TURBO)
                .toCompletableFuture()
                .join();
        Assertions.assertEquals(uploadedForA, uploadedForB);
    }

}
