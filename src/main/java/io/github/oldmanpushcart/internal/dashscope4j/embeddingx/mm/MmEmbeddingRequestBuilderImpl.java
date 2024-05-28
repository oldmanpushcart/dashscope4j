package io.github.oldmanpushcart.internal.dashscope4j.embeddingx.mm;

import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.FactorContent;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.SpecifyModelAlgoRequestBuilderImpl;

import java.util.ArrayList;
import java.util.List;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.updateList;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.requireNotEmpty;
import static java.util.Objects.requireNonNull;

public class MmEmbeddingRequestBuilderImpl
        extends SpecifyModelAlgoRequestBuilderImpl<MmEmbeddingModel, MmEmbeddingRequest, MmEmbeddingRequest.Builder>
        implements MmEmbeddingRequest.Builder {

    private final List<FactorContent<?>> contents = new ArrayList<>();

    public MmEmbeddingRequestBuilderImpl() {
    }

    public MmEmbeddingRequestBuilderImpl(MmEmbeddingRequest request) {
        super(request);
        this.contents.addAll(request.contents());
    }

    @Override
    public MmEmbeddingRequest.Builder contents(boolean isAppend, List<FactorContent<?>> contents) {
        updateList(isAppend, this.contents, contents);
        return this;
    }

    @Override
    public MmEmbeddingRequest build() {
        requireNonNull(model(), "model is required");
        requireNotEmpty(contents, "contents is required");
        return new MmEmbeddingRequestImpl(
                model(),
                option(),
                timeout(),
                contents
        );
    }

}
