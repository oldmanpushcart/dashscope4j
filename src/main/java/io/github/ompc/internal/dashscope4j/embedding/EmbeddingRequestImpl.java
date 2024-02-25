package io.github.ompc.internal.dashscope4j.embedding;

import io.github.ompc.dashscope4j.Model;
import io.github.ompc.dashscope4j.Option;
import io.github.ompc.dashscope4j.embedding.EmbeddingRequest;
import io.github.ompc.dashscope4j.embedding.EmbeddingResponse;
import io.github.ompc.internal.dashscope4j.base.algo.AlgoRequestImpl;

import java.time.Duration;

final class EmbeddingRequestImpl extends AlgoRequestImpl<EmbeddingResponse> implements EmbeddingRequest {

    EmbeddingRequestImpl(Model model, Object input, Option option, Duration timeout) {
        super(model, input, option, timeout, EmbeddingResponseImpl.class);
    }

    @Override
    public String toString() {
        return "dashscope://embedding";
    }

}
