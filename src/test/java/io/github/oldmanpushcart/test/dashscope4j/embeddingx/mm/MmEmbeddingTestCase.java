package io.github.oldmanpushcart.test.dashscope4j.embeddingx.mm;

import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.FactorContent;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingOptions;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.test.dashscope4j.DashScopeAssertions;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

public class MmEmbeddingTestCase implements LoadingEnv {

    @Test
    public void test$embeddingx$mm() {

        final var request = MmEmbeddingRequest.newBuilder()
                .model(MmEmbeddingModel.MM_EMBEDDING_ONE_PEACE_V1)
                .option(MmEmbeddingOptions.AUTO_TRUNCATION, true)
                .contents(
                        FactorContent.ofAudio(URI.create("https://dashscope.oss-cn-beijing.aliyuncs.com/audios/2channel_16K.wav")),
                        FactorContent.ofImage(URI.create("https://ompc-images.oss-cn-hangzhou.aliyuncs.com/image-002.jpeg")),
                        FactorContent.ofText("一个帅哥在骑自行车念经"),
                        FactorContent.ofText("有两个自行车")
                )
                .build();

        final var response = client.mmEmbedding(request)
                .async()
                .join();

        DashScopeAssertions.assertApiResponse(response);
        Assertions.assertNotNull(response.output());
        Assertions.assertNotNull(response.output().embedding());
        Assertions.assertEquals(response.output().embedding().vector().length, MmEmbeddingModel.MM_EMBEDDING_ONE_PEACE_V1.dimension());

        System.out.println(response);

    }

}
