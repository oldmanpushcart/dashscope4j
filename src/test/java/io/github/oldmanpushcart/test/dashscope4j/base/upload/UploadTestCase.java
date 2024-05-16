package io.github.oldmanpushcart.test.dashscope4j.base.upload;

import io.github.oldmanpushcart.dashscope4j.base.upload.UploadRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class UploadTestCase implements LoadingEnv {

    @Test
    public void test$upload$image() {

        final var request = UploadRequest.newBuilder()
                .model(ChatModel.QWEN_PLUS)
                .resource(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg"))
                .build();

        final var response = client.base().upload(request).async()
                .join();

        System.out.println(response.output().uploaded());

    }

}
