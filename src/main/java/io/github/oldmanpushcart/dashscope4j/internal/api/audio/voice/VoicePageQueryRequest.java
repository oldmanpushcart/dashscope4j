package io.github.oldmanpushcart.dashscope4j.internal.api.audio.voice;

import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.internal.util.ObjectMap;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import static io.github.oldmanpushcart.dashscope4j.internal.util.CommonUtils.check;
import static java.util.Objects.requireNonNull;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class VoicePageQueryRequest extends AlgoRequest<VoiceModel, VoicePageQueryResponse> {

    String group;
    int pageIndex;
    int pageSize;

    private VoicePageQueryRequest(Builder builder) {
        super(VoicePageQueryResponse.class, builder);
        this.group = builder.group;
        this.pageIndex = builder.pageIndex;
        this.pageSize = builder.pageSize;
    }

    @Override
    protected Object input() {
        return new ObjectMap() {{
            put("action", "list_voice");
            put("prefix", group);
            put("page_index", pageIndex);
            put("page_size", pageSize);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(VoicePageQueryRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<VoiceModel, VoicePageQueryRequest, Builder> {

        private String group;
        private int pageIndex;
        private int pageSize;

        public Builder() {

        }

        public Builder(VoicePageQueryRequest request) {
            super(request);
            this.group = request.group;
            this.pageIndex = request.pageIndex;
            this.pageSize = request.pageSize;
        }

        public Builder group(String group) {
            this.group = requireNonNull(group, "group is required!");
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
        public VoicePageQueryRequest build() {
            return new VoicePageQueryRequest(this);
        }
    }

}
