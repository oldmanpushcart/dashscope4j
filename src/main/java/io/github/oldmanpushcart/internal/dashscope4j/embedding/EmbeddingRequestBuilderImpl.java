package io.github.oldmanpushcart.internal.dashscope4j.embedding;

import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingModel;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingRequest;
import io.github.oldmanpushcart.internal.dashscope4j.base.algo.SpecifyModelAlgoRequestBuilderImpl;

import java.util.ArrayList;
import java.util.List;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.requireNotEmpty;
import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.updateList;
import static java.util.Objects.requireNonNull;

public class EmbeddingRequestBuilderImpl
        extends SpecifyModelAlgoRequestBuilderImpl<EmbeddingModel, EmbeddingRequest, EmbeddingRequest.Builder>
        implements EmbeddingRequest.Builder {

    private final List<String> documents = new ArrayList<>();

    public EmbeddingRequestBuilderImpl() {
    }

    public EmbeddingRequestBuilderImpl(EmbeddingRequest request) {
        super(request);
        this.documents.addAll(request.documents());
    }

    @Override
    public EmbeddingRequest.Builder documents(boolean isAppend, List<String> documents) {
        updateList(isAppend, this.documents, documents);
        return this;
    }

    @Override
    public EmbeddingRequest build() {
        requireNonNull(model(), "model is required");
        requireNotEmpty(documents, "documents is required");
        return new EmbeddingRequestImpl(
                model(),
                option(),
                timeout(),
                documents
        );
    }

}
