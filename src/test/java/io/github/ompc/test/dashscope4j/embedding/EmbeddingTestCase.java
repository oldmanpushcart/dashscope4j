package io.github.ompc.test.dashscope4j.embedding;

import io.github.ompc.dashscope4j.embedding.EmbeddingModel;
import io.github.ompc.dashscope4j.embedding.EmbeddingRequest;
import io.github.ompc.test.dashscope4j.DashScopeAssertions;
import io.github.ompc.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Test;

public class EmbeddingTestCase implements LoadingEnv {

    @Test
    public void test$embedding() {

        final var request = EmbeddingRequest.newBuilder()
                .model(EmbeddingModel.TEXT_EMBEDDING_V2)
                .documents("我爱北京天安门", "天安门上太阳升")
                .build();

        DashScopeAssertions.assertEmbeddingRequest(request);

        final var response = client.embedding(request)
                .async()
                .join();

        DashScopeAssertions.assertEmbeddingResponse(response);

    }

}
