package io.github.oldmanpushcart.dashscope4j.api.embedding.text;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.github.oldmanpushcart.dashscope4j.api.ApiAssertions.assertApiResponseSuccessful;

public class EmbeddingTestCase extends ClientSupport {

    @Test
    public void test$embedding() {

        final EmbeddingRequest request = EmbeddingRequest.newBuilder()
                .model(EmbeddingModel.TEXT_EMBEDDING_V2)
                .documents(List.of("我爱北京天安门", "天安门上太阳升"))
                .build();

        final EmbeddingResponse response = client.embedding().text().async(request)
                .toCompletableFuture()
                .join();

        assertApiResponseSuccessful(response);
        assertEmbeddingResponse(response);

        response.output().embeddings().forEach(embedding -> {
            Assertions.assertNotNull(embedding);
            Assertions.assertNotNull(embedding.vector());
            Assertions.assertEquals(embedding.vector().length, EmbeddingModel.TEXT_EMBEDDING_V2.dimension());
        });

    }

    private static void assertEmbeddingResponse(EmbeddingResponse response) {
        Assertions.assertNotNull(response.output());
        Assertions.assertNotNull(response.output().embeddings());
        Assertions.assertFalse(response.output().embeddings().isEmpty());
    }

}
