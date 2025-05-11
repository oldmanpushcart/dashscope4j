package io.github.oldmanpushcart.dashscope4j.client.internal.api.embedding;

import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.OpAsync;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.api.embedding.EmbeddingOp;
import io.github.oldmanpushcart.dashscope4j.client.api.embedding.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.embedding.mm.MmEmbeddingResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.embedding.text.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.embedding.text.EmbeddingResponse;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class EmbeddingOpImpl implements EmbeddingOp {

    private static final List<Interceptor> interceptors = Arrays.asList(
            new ProcessAutoUploadForMmEmbeddingInterceptor()
    );
    private final ApiOp apiOp;

    @Override
    public OpAsync<EmbeddingRequest, EmbeddingResponse> text() {
        return apiOp::executeAsync;
    }

    @Override
    public OpAsync<MmEmbeddingRequest, MmEmbeddingResponse> mm() {
        return request -> {
            final MmEmbeddingRequest newRequest = MmEmbeddingRequest.newBuilder(request)
                    .interceptors(interceptors)
                    .build();
            return apiOp.executeAsync(newRequest);
        };
    }

}
