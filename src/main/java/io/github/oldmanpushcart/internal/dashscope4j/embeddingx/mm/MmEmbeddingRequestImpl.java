package io.github.oldmanpushcart.internal.dashscope4j.embeddingx.mm;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.Option;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.FactorContent;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingResponse;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestImpl;

import java.time.Duration;
import java.util.List;

final class MmEmbeddingRequestImpl extends AlgoRequestImpl<MmEmbeddingResponse> implements MmEmbeddingRequest {

    MmEmbeddingRequestImpl(Model model, Option option, Duration timeout, List<FactorContent<?>> contents) {
        super(model, new Input(contents), option, timeout, MmEmbeddingResponseImpl.class);
    }

    private record Input(

            @JsonProperty("contents")
            List<FactorContent<?>> contents

    ) {

    }

}
