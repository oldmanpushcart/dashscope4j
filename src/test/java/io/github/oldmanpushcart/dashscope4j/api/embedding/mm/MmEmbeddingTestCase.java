package io.github.oldmanpushcart.dashscope4j.api.embedding.mm;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static io.github.oldmanpushcart.dashscope4j.api.ApiAssertions.assertApiResponseSuccessful;

public class MmEmbeddingTestCase extends ClientSupport {

    @Test
    public void test$embedding() {

        final MmEmbeddingRequest request = MmEmbeddingRequest.newBuilder()
                .model(MmEmbeddingModel.MM_EMBEDDING_V1)
                .contents(List.of(
                        Content.ofImage(new File("./test-data/image-002.jpeg").toURI()),
                        Content.ofText("一个帅哥在骑自行车念经"),
                        Content.ofText("有两个自行车")
                ))
                .build();

        final MmEmbeddingResponse response = client.embedding().mm().async(request)
                .toCompletableFuture()
                .join();

        assertApiResponseSuccessful(response);
        assertMmEmbeddingResponse(response);
        Assertions.assertEquals(3, response.output().embeddings().size());
        response.output().embeddings().forEach(embedding -> {
            Assertions.assertNotNull(embedding);
            Assertions.assertNotNull(embedding.vector());
            Assertions.assertEquals(embedding.vector().length, MmEmbeddingModel.MM_EMBEDDING_V1.dimension());
        });
    }

    private static void assertMmEmbeddingResponse(MmEmbeddingResponse response) {
        Assertions.assertNotNull(response.output());
        Assertions.assertNotNull(response.output().embeddings());
    }

}
