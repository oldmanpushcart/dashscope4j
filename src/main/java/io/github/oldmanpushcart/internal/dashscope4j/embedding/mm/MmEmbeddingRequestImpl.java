package io.github.oldmanpushcart.internal.dashscope4j.embedding.mm;

import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.embedding.mm.MmEmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embedding.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embedding.mm.MmEmbeddingResponse;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestImpl;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;

class MmEmbeddingRequestImpl extends AlgoRequestImpl<MmEmbeddingModel, MmEmbeddingResponse>
        implements MmEmbeddingRequest {

    private final List<Content<?>> contents;

    MmEmbeddingRequestImpl(MmEmbeddingModel model, Option option, Duration timeout, List<Content<?>> contents) {
        super(model, option, timeout, MmEmbeddingResponseImpl.class);
        this.contents = contents;
    }

    @Override
    public String suite() {
        return "dashscope://embedding/mm";
    }

    @Override
    public List<Content<?>> contents() {
        return contents;
    }

    @Override
    public Object input() {
        return new HashMap<>() {{
            put("contents", contents);
        }};
    }

}
