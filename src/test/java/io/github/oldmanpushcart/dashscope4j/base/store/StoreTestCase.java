package io.github.oldmanpushcart.dashscope4j.base.store;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URI;

public class StoreTestCase extends ClientSupport {

    @Test
    public void test$upload$success() {
        final File file = new File("./test-data/P020210313315693279320.pdf");
        final URI uploaded = client.base().store().upload(file.toURI(), ChatModel.QWEN_TURBO)
                .toCompletableFuture()
                .join();
        Assertions.assertEquals("dashscope-instant", uploaded.getHost());
        Assertions.assertEquals("oss", uploaded.getScheme());
    }

}
