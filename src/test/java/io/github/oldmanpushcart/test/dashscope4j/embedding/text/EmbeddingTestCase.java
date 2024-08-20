package io.github.oldmanpushcart.test.dashscope4j.embedding.text;

import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingRequest;
import io.github.oldmanpushcart.test.dashscope4j.DashScopeAssertions;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Test;

import java.util.List;

public class EmbeddingTestCase implements LoadingEnv {

    @Test
    public void test$embedding() {

        final var request = EmbeddingRequest.newBuilder()
                .model(EmbeddingModel.TEXT_EMBEDDING_V2)
                .documents(List.of("我爱北京天安门", "天安门上太阳升"))
                .build();

        DashScopeAssertions.assertEmbeddingRequest(request);

        final var response = client.embedding().text(request)
                .async()
                .toCompletableFuture()
                .join();

        DashScopeAssertions.assertEmbeddingResponse(response);

    }

}
