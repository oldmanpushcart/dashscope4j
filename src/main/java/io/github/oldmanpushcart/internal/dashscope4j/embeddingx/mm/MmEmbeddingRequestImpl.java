package io.github.oldmanpushcart.internal.dashscope4j.embeddingx.mm;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.FactorContent;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingResponse;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.SpecifyModelAlgoRequestImpl;

import java.time.Duration;
import java.util.List;

final class MmEmbeddingRequestImpl extends SpecifyModelAlgoRequestImpl<MmEmbeddingModel, MmEmbeddingResponse>
        implements MmEmbeddingRequest {

    private final Input input;
    private final List<FactorContent<?>> contents;

    MmEmbeddingRequestImpl(MmEmbeddingModel model, Option option, Duration timeout, List<FactorContent<?>> contents) {
        super(model, option, timeout, MmEmbeddingResponseImpl.class);
        this.input = new Input(contents);
        this.contents = contents;
    }

    @Override
    public String suite() {
        return "/dashscope";
    }

    @Override
    public String type() {
        return "mm-embedding";
    }

    @Override
    public List<FactorContent<?>> contents() {
        return contents;
    }

    @Override
    public Object input() {
        return input;
    }

    private record Input(

            @JsonProperty("contents")
            List<FactorContent<?>> contents

    ) {

    }

}
