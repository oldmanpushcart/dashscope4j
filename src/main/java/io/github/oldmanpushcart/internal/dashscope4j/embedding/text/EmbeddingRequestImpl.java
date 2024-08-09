package io.github.oldmanpushcart.internal.dashscope4j.embedding.text;

import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingResponse;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.HttpAlgoRequestImpl;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

class EmbeddingRequestImpl extends HttpAlgoRequestImpl<EmbeddingModel, EmbeddingResponse> implements EmbeddingRequest {

    private final List<String> documents;

    EmbeddingRequestImpl(EmbeddingModel model, Option option, Duration timeout, List<String> documents) {
        super(model, option, timeout, EmbeddingResponseImpl.class);
        this.documents = documents;
    }

    @Override
    public String suite() {
        return "dashscope://embedding/text";
    }

    @Override
    public List<String> documents() {
        return documents;
    }

    @Override
    protected Object input() {
        return new HashMap<>(){{
            put("texts", documents);
        }};
    }

}
