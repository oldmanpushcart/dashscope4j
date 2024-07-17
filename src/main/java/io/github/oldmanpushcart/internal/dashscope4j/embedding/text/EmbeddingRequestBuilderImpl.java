package io.github.oldmanpushcart.internal.dashscope4j.embedding.text;

import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.AlgoRequestBuilderImpl;

import java.util.ArrayList;
import java.util.List;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.UpdateMode.REPLACE_ALL;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CollectionUtils.updateList;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.requireNotEmpty;
import static java.util.Objects.requireNonNull;

public class EmbeddingRequestBuilderImpl
        extends AlgoRequestBuilderImpl<EmbeddingModel, EmbeddingRequest, EmbeddingRequest.Builder>
        implements EmbeddingRequest.Builder {

    private final List<String> documents = new ArrayList<>();

    public EmbeddingRequestBuilderImpl() {
    }

    public EmbeddingRequestBuilderImpl(EmbeddingRequest request) {
        super(requireNonNull(request));
        updateList(REPLACE_ALL, this.documents, request.documents());
    }

    @Override
    public EmbeddingRequest.Builder documents(List<String> documents) {
        requireNonNull(documents);
        updateList(REPLACE_ALL, this.documents, documents);
        return this;
    }

    @Override
    public EmbeddingRequest build() {
        requireNonNull(model(), "model is required!");
        requireNotEmpty(documents, "documents is required!");
        return new EmbeddingRequestImpl(
                model(),
                option(),
                timeout(),
                documents
        );
    }

}
