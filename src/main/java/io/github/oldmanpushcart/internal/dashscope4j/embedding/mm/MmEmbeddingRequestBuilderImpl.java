package io.github.oldmanpushcart.internal.dashscope4j.embedding.mm;

import io.github.oldmanpushcart.dashscope4j.chat.message.Content;
import io.github.oldmanpushcart.dashscope4j.embedding.mm.MmEmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embedding.mm.MmEmbeddingRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestBuilderImpl;

import java.util.ArrayList;
import java.util.List;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.UpdateMode.REPLACE_ALL;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.updateList;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.requireNotEmpty;
import static java.util.Objects.requireNonNull;

public class MmEmbeddingRequestBuilderImpl
        extends AlgoRequestBuilderImpl<MmEmbeddingModel, MmEmbeddingRequest, MmEmbeddingRequest.Builder>
        implements MmEmbeddingRequest.Builder {

    private final List<Content<?>> contents = new ArrayList<>();

    public MmEmbeddingRequestBuilderImpl() {
    }

    public MmEmbeddingRequestBuilderImpl(MmEmbeddingRequest request) {
        super(requireNonNull(request));
        updateList(REPLACE_ALL, this.contents, request.contents());
    }

    @Override
    public MmEmbeddingRequest.Builder contents(List<Content<?>> contents) {
        requireNonNull(contents);
        updateList(REPLACE_ALL, this.contents, contents);
        return this;
    }

    @Override
    public MmEmbeddingRequest build() {
        requireNonNull(model(), "model is required!");
        requireNotEmpty(contents, "contents is empty!");
        return new MmEmbeddingRequestImpl(
                model(),
                option(),
                timeout(),
                contents
        );
    }

}
