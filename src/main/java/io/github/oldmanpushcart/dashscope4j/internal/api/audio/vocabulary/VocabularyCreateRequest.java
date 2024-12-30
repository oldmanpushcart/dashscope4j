package io.github.oldmanpushcart.dashscope4j.internal.api.audio.vocabulary;

import io.github.oldmanpushcart.dashscope4j.Model;
import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.api.audio.vocabulary.Vocabulary;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static io.github.oldmanpushcart.dashscope4j.internal.util.CommonUtils.check;
import static io.github.oldmanpushcart.dashscope4j.internal.util.CommonUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VocabularyCreateRequest extends AlgoRequest<VocabularyModel, VocabularyCreateResponse> {

    Model targetModel;
    String group;
    List<Vocabulary.Item> items;

    /**
     * 构建Api请求
     *
     * @param builder 构建器
     */
    private VocabularyCreateRequest(Builder builder) {
        super(VocabularyCreateResponse.class, builder);
        this.targetModel = requireNonNull(builder.targetModel);
        this.group = requireNonBlankString(builder.group, "group is required!");
        this.items = check(builder.items, v -> !v.isEmpty(), "items is required!");
    }

    @Override
    protected Object input() {
        return new HashMap<String, Object>() {{
            put("action", "create_vocabulary");
            put("target_model", targetModel);
            put("prefix", group);
            put("vocabulary", items);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(VocabularyCreateRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<VocabularyModel, VocabularyCreateRequest, Builder> {

        private Model targetModel;
        private String group;
        private final List<Vocabulary.Item> items = new LinkedList<>();

        public Builder() {
        }

        public Builder(VocabularyCreateRequest request) {
            super(request);
            this.targetModel = request.targetModel();
            this.group = request.group();
            this.items.addAll(request.items());
        }

        public Builder targetModel(Model targetModel) {
            this.targetModel = requireNonNull(targetModel);
            return this;
        }

        public Builder group(String group) {
            this.group = requireNonNull(group);
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
        public VocabularyCreateRequest build() {
            return new VocabularyCreateRequest(this);
        }

    }

}
