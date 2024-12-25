package io.github.oldmanpushcart.dashscope4j.internal.api.embedding;

import io.github.oldmanpushcart.dashscope4j.OpAsync;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.embedding.EmbeddingOp;
import io.github.oldmanpushcart.dashscope4j.api.embedding.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.api.embedding.mm.MmEmbeddingResponse;
import io.github.oldmanpushcart.dashscope4j.api.embedding.text.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.api.embedding.text.EmbeddingResponse;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EmbeddingOpImpl implements EmbeddingOp {

    private final ApiOp apiOp;

    @Override
    public OpAsync<EmbeddingRequest, EmbeddingResponse> text() {
        return apiOp::executeAsync;
    }

    @Override
    public OpAsync<MmEmbeddingRequest, MmEmbeddingResponse> mm() {
        return apiOp::executeAsync;
    }

}
