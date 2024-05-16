package io.github.oldmanpushcart.internal.dashscope4j.embeddingx.mm;

import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.FactorContent;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.SpecifyModelAlgoRequestBuilderImpl;

import java.util.ArrayList;
import java.util.List;

public class MmEmbeddingRequestBuilderImpl extends SpecifyModelAlgoRequestBuilderImpl<MmEmbeddingModel, MmEmbeddingRequest, MmEmbeddingRequest.Builder>
        implements MmEmbeddingRequest.Builder {

    private final List<FactorContent<?>> contents;

    public MmEmbeddingRequestBuilderImpl() {
        this.contents = new ArrayList<>();
    }

    public MmEmbeddingRequestBuilderImpl(MmEmbeddingRequest request) {
        super(request);
        this.contents = request.contents();
    }

    @Override
    public MmEmbeddingRequest.Builder contents(FactorContent<?>... contents) {
        this.contents.addAll(List.of(contents));
        return this;
    }

    @Override
    public MmEmbeddingRequest build() {
        return new MmEmbeddingRequestImpl(
                model(),
                option(),
                timeout(),
                contents
        );
    }

}
