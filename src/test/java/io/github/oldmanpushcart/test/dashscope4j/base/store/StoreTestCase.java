package io.github.oldmanpushcart.test.dashscope4j.base.store;

import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class StoreTestCase implements LoadingEnv {

    @Test
    public void test$upload$op() {
        final var uri = client.base().store()
                .upload(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg"), ChatModel.QWEN_PLUS)
                .join();

        Assertions.assertNotNull(uri);
    }

    @Test
    public void test$upload$op$hit_cache() {
        final var uri = URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg");
        final var first = client.base().store().upload(uri, ChatModel.QWEN_PLUS).join();
        final var second = client.base().store().upload(uri, ChatModel.QWEN_PLUS).join();
        Assertions.assertEquals(first, second);
    }

}
