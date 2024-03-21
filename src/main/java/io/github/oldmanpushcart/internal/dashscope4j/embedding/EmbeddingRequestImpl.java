package io.github.oldmanpushcart.internal.dashscope4j.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingResponse;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestImpl;

import java.time.Duration;
import java.util.List;

final class EmbeddingRequestImpl extends AlgoRequestImpl<EmbeddingResponse> implements EmbeddingRequest {

    EmbeddingRequestImpl(Model model, Option option, Duration timeout, List<String> documents) {
        super(model, new Input(documents), option, timeout, EmbeddingResponseImpl.class);
    }

    private record Input(
            @JsonProperty("texts")
            List<String> documents
    ) {

    }

}
