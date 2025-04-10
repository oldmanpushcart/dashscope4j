package io.github.oldmanpushcart.dashscope4j.api.embedding.text;

import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.ObjectMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static io.github.oldmanpushcart.dashscope4j.internal.util.CommonUtils.requireNonEmptyCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * 文本向量计算请求
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class EmbeddingRequest extends AlgoRequest<EmbeddingModel, EmbeddingResponse> {

    List<String> documents;

    private EmbeddingRequest(Builder builder) {
        super(EmbeddingResponse.class, builder);
        requireNonEmptyCollection(builder.documents, "documents is empty!");
        documents = unmodifiableList(builder.documents);
    }

    @Override
    protected Object input() {
        return new ObjectMap() {{
            put("texts", documents);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(EmbeddingRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<EmbeddingModel, EmbeddingRequest, Builder> {

        private final List<String> documents = new ArrayList<>();

        public Builder() {

        }

        public Builder(EmbeddingRequest request) {
            super(request);
            documents.addAll(request.documents());
        }

        public Builder addDocument(String document) {
            requireNonNull(document);
            documents.add(document);
            return this;
        }

        public Builder addDocuments(Collection<String> documents) {
            requireNonNull(documents);
            this.documents.addAll(documents);
            return this;
        }

        public Builder documents(List<String> documents) {
            requireNonNull(documents);
            this.documents.clear();
            this.documents.addAll(documents);
            return this;
        }


        @Override
        public EmbeddingRequest build() {
            return new EmbeddingRequest(this);
        }

    }

}
