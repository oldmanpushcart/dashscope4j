package io.github.oldmanpushcart.dashscope4j.client.aigc.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.embedding.internal.interceptor.UploadMmContentInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.util.List;
import java.util.function.UnaryOperator;

public record MmEmbeddingModel(
        String name,
        String path
) implements AigcModel<MmEmbeddingModel.Input, MmEmbeddingModel.Output> {

    private static final String PATH = "/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding";
    private static final List<Interceptor> interceptors = List.of(
            new UploadMmContentInterceptor()
    );

    public static final MmEmbeddingModel QWEN3_VL_EMBEDDING = new MmEmbeddingModel("qwen3-vl-embedding", PATH);
    public static final MmEmbeddingModel QWEN2_5_VL_EMBEDDING = new MmEmbeddingModel("qwen2.5-vl-embedding", PATH);
    public static final MmEmbeddingModel TONGYI_EMBEDDING_VISION_PLUS = new MmEmbeddingModel("tongyi-embedding-vision-plus", PATH);
    public static final MmEmbeddingModel TONGYI_EMBEDDING_VISION_FLASH = new MmEmbeddingModel("tongyi-embedding-vision-flash", PATH);
    public static final MmEmbeddingModel MULTIMODAL_EMBEDDING_V1 = new MmEmbeddingModel("multimodal-embedding-v1", PATH);

    @Override
    public List<Interceptor> interceptors() {
        return interceptors;
    }

    public record Input(

            @JsonProperty("contents")
            List<MmContent> contents,

            boolean uploadEnabled,

            boolean inlineEnabled

    ) {

        private Input(Builder builder) {
            this(
                    builder.contents,
                    builder.uploadEnabled,
                    builder.inlineEnabled
            );
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public static Builder newBuilder(Input input) {
            return new Builder(input);
        }

        public static class Builder implements Buildable<Input, Builder> {

            private List<MmContent> contents;
            private boolean uploadEnabled;
            private boolean inlineEnabled;

            public Builder() {

            }

            public Builder(Input input) {
                this.contents = input.contents;
                this.uploadEnabled = input.uploadEnabled;
                this.inlineEnabled = input.inlineEnabled;
            }

            public Builder uploadEnabled(boolean uploadEnabled) {
                this.uploadEnabled = uploadEnabled;
                return this;
            }

            public Builder inlineEnabled(boolean inlineEnabled) {
                this.inlineEnabled = inlineEnabled;
                return this;
            }

            public Builder contents(List<MmContent> contents) {
                this.contents = contents;
                return this;
            }

            public Builder contents(UnaryOperator<List<MmContent>> operator) {
                this.contents = operator.apply(CommonUtils.mutableCopy(this.contents));
                return this;
            }

            @Override
            public Input build() {
                return new Input(this);
            }

        }

    }

    public record Output(

            @JsonProperty("embeddings")
            List<Embedding> embeddings

    ) {

        public record Embedding(

                @JsonProperty("index")
                int index,

                @JsonProperty("embedding")
                float[] vector

        ) {

            public enum Type {

                @JsonProperty("text")
                TEXT,

                @JsonProperty("image")
                IMAGE,

                @JsonProperty("video")
                VIDEO,

                @JsonProperty("vl")
                COMPLEX

            }

        }

    }

}
