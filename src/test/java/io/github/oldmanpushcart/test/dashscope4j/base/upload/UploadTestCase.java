package io.github.oldmanpushcart.test.dashscope4j.base.upload;

import io.github.oldmanpushcart.dashscope4j.base.upload.UploadRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class UploadTestCase implements LoadingEnv {

    @Test
    public void test$upload$image() {

        final var resource = URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg");
        final var model = ChatModel.QWEN_PLUS;

        final var request = UploadRequest.newBuilder()
                .resource(resource)
                .model(model)
                .build();

        final var response = client.base().upload(request).async()
                .join();

        Assertions.assertEquals(resource, response.output().resource());
        Assertions.assertEquals(model, response.output().model());
        Assertions.assertNotNull(response.output().uploaded());
        Assertions.assertTrue(response.ret().isSuccess());
        Assertions.assertNotNull(response.usage());

    }

}
