package io.github.oldmanpushcart.dashscope4j.internal.api.audio.vocabulary;

import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.HashMap;

import static java.util.Objects.requireNonNull;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VocabularyDetailRequest extends AlgoRequest<VocabularyModel, VocabularyDetailResponse> {

    String vocabularyId;

    private VocabularyDetailRequest(Builder builder) {
        super(VocabularyDetailResponse.class, builder);
        this.vocabularyId = requireNonNull(builder.vocabularyId);
    }

    @Override
    protected Object input() {
        return new HashMap<String, Object>() {{
            put("action", "query_vocabulary");
            put("vocabulary_id", vocabularyId);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(VocabularyDetailRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<VocabularyModel, VocabularyDetailRequest, Builder> {

        private String vocabularyId;

        public Builder() {

        }

        public Builder(VocabularyDetailRequest request) {
            super(request);
            this.vocabularyId = request.vocabularyId;
        }

        public Builder vocabularyId(String vocabularyId) {
            this.vocabularyId = requireNonNull(vocabularyId);
            return this;
        }

        @Override
        public VocabularyDetailRequest build() {
            return new VocabularyDetailRequest(this);
        }

    }

}
