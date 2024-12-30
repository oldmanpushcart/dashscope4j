package io.github.oldmanpushcart.dashscope4j.internal.api.audio.vocabulary;

import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.HashMap;

import static io.github.oldmanpushcart.dashscope4j.internal.util.CommonUtils.check;
import static java.util.Objects.requireNonNull;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VocabularyPageQueryRequest extends AlgoRequest<VocabularyModel, VocabularyPageQueryResponse> {

    String group;
    int pageIndex;
    int pageSize;

    private VocabularyPageQueryRequest(Builder builder) {
        super(VocabularyPageQueryResponse.class, builder);
        this.group = builder.group;
        this.pageIndex = builder.pageIndex;
        this.pageSize = builder.pageSize;
    }

    @Override
    protected Object input() {
        return new HashMap<String, Object>() {{
            put("action", "list_vocabulary");
            put("prefix", group);
            put("page_index", pageIndex);
            put("page_size", pageSize);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(VocabularyPageQueryRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<VocabularyModel, VocabularyPageQueryRequest, Builder> {

        private String group;
        private int pageIndex = 0;
        private int pageSize = 10;

        public Builder() {

        }

        public Builder(VocabularyPageQueryRequest request) {
            super(request);
            this.group = request.group;
            this.pageIndex = request.pageIndex;
            this.pageSize = request.pageSize;
        }

        public Builder group(String group) {
            this.group = requireNonNull(group);
            return this;
        }

        public Builder pageIndex(int pageIndex) {
            this.pageIndex = check(pageIndex, v -> v >= 0, "pageIndex must be positive!");
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = check(pageSize, v -> v > 0, "pageSize must be positive!");
            return this;
        }

        @Override
        public VocabularyPageQueryRequest build() {
            return new VocabularyPageQueryRequest(this);
        }

    }

}
