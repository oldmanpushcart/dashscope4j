package io.github.oldmanpushcart.test.dashscope4j.embedding.mm;

import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.embedding.mm.MmEmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embedding.mm.MmEmbeddingOptions;
import io.github.oldmanpushcart.dashscope4j.embedding.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.test.dashscope4j.DashScopeAssertions;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

/**
 * MM Embedding 测试用例
 * <p>阿里云的免费额度耗尽，暂停测试用例执行</p>
 */
@Disabled
public class MmEmbeddingTestCase implements LoadingEnv {

    @Test
    public void test$embeddingx$mm() {

        final var request = MmEmbeddingRequest.newBuilder()
                .model(MmEmbeddingModel.MM_EMBEDDING_ONE_PEACE_V1)
                .option(MmEmbeddingOptions.AUTO_TRUNCATION, true)
                .contents(List.of(
                        Content.ofAudio(URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/audios/2channel_16K.wav")),
                        Content.ofImage(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg")),
                        Content.ofText("一个帅哥在骑自行车念经"),
                        Content.ofText("有两个自行车")
                ))
                .build();

        final var response = client.embedding().mm(request)
                .async()
                .join();

        DashScopeAssertions.assertApiResponse(response);
        Assertions.assertNotNull(response.output());
        Assertions.assertNotNull(response.output().embedding());
        Assertions.assertEquals(response.output().embedding().vector().length, MmEmbeddingModel.MM_EMBEDDING_ONE_PEACE_V1.dimension());

        System.out.println(response);

    }

}
