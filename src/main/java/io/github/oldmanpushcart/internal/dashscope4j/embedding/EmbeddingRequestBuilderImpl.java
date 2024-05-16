package io.github.oldmanpushcart.internal.dashscope4j.embedding;

import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.SpecifyModelAlgoRequestBuilderImpl;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class EmbeddingRequestBuilderImpl extends SpecifyModelAlgoRequestBuilderImpl<EmbeddingModel, EmbeddingRequest, EmbeddingRequest.Builder> implements EmbeddingRequest.Builder {

    private final List<String> documents;

    public EmbeddingRequestBuilderImpl() {
        this.documents = new ArrayList<>();
    }

    public EmbeddingRequestBuilderImpl(EmbeddingRequest request) {
        super(request);
        this.documents = request.documents();
    }

    @Override
    public EmbeddingRequest.Builder documents(String... documents) {
        this.documents.addAll(List.of(documents));
        return this;
    }

    @Override
    public EmbeddingRequest build() {
        return new EmbeddingRequestImpl(
                requireNonNull(model()),
                option(),
                timeout(),
                documents
        );
    }

}
