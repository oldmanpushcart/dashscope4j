package io.github.oldmanpushcart.internal.dashscope4j.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingResponse;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.SpecifyModelAlgoRequestImpl;

import java.time.Duration;
import java.util.List;

final class EmbeddingRequestImpl extends SpecifyModelAlgoRequestImpl<EmbeddingModel, EmbeddingResponse> implements EmbeddingRequest {
    
    private final List<String> documents;

    EmbeddingRequestImpl(EmbeddingModel model, Option option, Duration timeout, List<String> documents) {
        super(model, option, timeout, EmbeddingResponseImpl.class);
        this.documents = documents;
    }

    @Override
    public List<String> documents() {
        return documents;
    }

    @Override
    public Object input() {
        return new Input(documents);
    }

    private record Input(
            @JsonProperty("texts") List<String> documents
    ) {

    }

}
