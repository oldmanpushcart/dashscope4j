package io.github.oldmanpushcart.dashscope4j.client.aigc.embedding;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcModel;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

public record TextEmbeddingModel(String name,
                                 String path) implements AigcModel<TextEmbeddingModel.Input, TextEmbeddingModel.Output> {

    private static final String PATH = "/api/v1/services/embeddings/text-embedding/text-embedding";

    public static final TextEmbeddingModel TEXT_EMBEDDING_V1 = new TextEmbeddingModel("text-embedding-v1", PATH);
    public static final TextEmbeddingModel TEXT_EMBEDDING_V2 = new TextEmbeddingModel("text-embedding-v2", PATH);
    public static final TextEmbeddingModel TEXT_EMBEDDING_V3 = new TextEmbeddingModel("text-embedding-v3", PATH);
    public static final TextEmbeddingModel TEXT_EMBEDDING_V4 = new TextEmbeddingModel("text-embedding-v4", PATH);

    public record Input(List<String> texts) {

        private Input(Builder builder) {
            this(
                    Collections.unmodifiableList(builder.texts)
            );
        }

        public static Builder newBuilder() {
            return new Builder();
        }

        public static Builder newBuilder(Input input) {
            return new Builder(input);
        }

        public static class Builder implements Buildable<Input, Builder> {

            private final List<String> texts = new ArrayList<>();

            public Builder() {
            }

            public Builder(Input input) {
                this.texts.addAll(input.texts);
            }

            public Builder texts(List<String> texts) {
                this.texts.clear();
                if (null != texts) {
                    this.texts.addAll(texts);
                }
                return this;
            }

            public Builder texts(UnaryOperator<List<String>> operator) {
                return texts(operator.apply(this.texts));
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

                @JsonProperty("text_index")
                int index,

                @JsonProperty("embedding")
                float[] vector

        ) {

        }

    }

}
