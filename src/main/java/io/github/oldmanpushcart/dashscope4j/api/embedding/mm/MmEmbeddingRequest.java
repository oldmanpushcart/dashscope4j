package io.github.oldmanpushcart.dashscope4j.api.embedding.mm;

import io.github.oldmanpushcart.dashscope4j.api.AlgoRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Content;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;
import okhttp3.Request;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static io.github.oldmanpushcart.dashscope4j.internal.InternalContents.ENABLE;
import static io.github.oldmanpushcart.dashscope4j.internal.InternalContents.HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * 多模态向量计算请求
 */
@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class MmEmbeddingRequest extends AlgoRequest<MmEmbeddingModel, MmEmbeddingResponse> {

    /**
     * 内容列表
     * <p>
     * 超过以下限制时会发生截断
     * <li>图像格式目前支持：JPG、PNG、BMP；文件大小不超过3M</li>
     * <li>视频格式目前支持 MP4、MPEG、MPG、WEBM、AVI、FLV、MKV、MOV；文件大小不超过10M</li>
     * <li>文本长度为512个Token, 超过512 token长度的文本内容将会被截断</li>
     * </p>
     */
    List<Content<?>> contents;

    private MmEmbeddingRequest(Builder builder) {
        super(MmEmbeddingResponse.class, builder);
        this.contents = unmodifiableList(builder.contents);
    }

    @Override
    public Request newHttpRequest() {
        return new Request.Builder(super.newHttpRequest())

                /*
                 * 启用OSS路径解析
                 */
                .addHeader(HTTP_HEADER_X_DASHSCOPE_OSS_RESOURCE_RESOLVE, ENABLE)

                .build();
    }

    @Override
    protected Object input() {
        return new HashMap<String, Object>() {{
            put("contents", contents);
        }};
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(MmEmbeddingRequest request) {
        return new Builder(request);
    }

    public static class Builder extends AlgoRequest.Builder<MmEmbeddingModel, MmEmbeddingRequest, Builder> {

        private final List<Content<?>> contents = new ArrayList<>();

        public Builder() {

        }

        public Builder(MmEmbeddingRequest request) {
            super(request);
            this.contents.addAll(request.contents);
        }

        public Builder contents(Collection<Content<?>> contents) {
            requireNonNull(contents);
            this.contents.clear();
            this.contents.addAll(contents);
            return this;
        }

        public Builder addContent(Content<?> content) {
            requireNonNull(content);
            this.contents.add(content);
            return this;
        }

        public Builder addContents(Collection<Content<?>> contents) {
            requireNonNull(contents);
            this.contents.addAll(contents);
            return this;
        }

        @Override
        public MmEmbeddingRequest build() {
            return new MmEmbeddingRequest(this);
        }

    }

}
