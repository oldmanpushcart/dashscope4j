package io.github.oldmanpushcart.dashscope4j.internal.api.audio.vocabulary;

import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.vocabulary.Vocabulary;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VocabularyUpdateRequest extends AlgoRequest<VocabularyModel, VocabularyUpdateResponse> {

    String vocabularyId;
    List<Vocabulary.Item> items;

    private VocabularyUpdateRequest(Builder builder) {
        super(VocabularyUpdateResponse.class, builder);
        this.vocabularyId = requireNonNull(builder.vocabularyId);
        this.items = unmodifiableList(builder.items);
    }

    @Override
    protected Object input() {
        return new HashMap<String, Object>() {{
            put("action", "update_vocabulary");
            put("vocabulary_id", vocabularyId);
            put("vocabulary", items);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(VocabularyUpdateRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<VocabularyModel, VocabularyUpdateRequest, Builder> {

        private String vocabularyId;
        private final List<Vocabulary.Item> items = new ArrayList<>();

        public Builder() {
        }

        public Builder(VocabularyUpdateRequest request) {
            super(request);
            this.vocabularyId = request.vocabularyId;
            this.items.addAll(request.items);
        }

        public Builder vocabularyId(String vocabularyId) {
            this.vocabularyId = requireNonNull(vocabularyId);
            return this;
        }

        public Builder addItem(Vocabulary.Item item) {
            requireNonNull(item);
            this.items.add(item);
            return this;
        }

        public Builder addItems(Collection<Vocabulary.Item> items) {
            requireNonNull(items);
            this.items.addAll(items);
            return this;
        }

        public Builder items(Collection<Vocabulary.Item> items) {
            requireNonNull(items);
            this.items.clear();
            this.items.addAll(items);
            return this;
        }

        @Override
        public VocabularyUpdateRequest build() {
            return new VocabularyUpdateRequest(this);
        }

    }

}
