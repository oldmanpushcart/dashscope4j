package io.github.oldmanpushcart.internal.dashscope4j.embedding;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.embedding.Embedding;
import io.github.oldmanpushcart.dashscope4j.embedding.EmbeddingResponse;

import java.util.List;

record EmbeddingResponseImpl(String uuid, Ret ret, Usage usage, Output output) implements EmbeddingResponse {

    public record OutputImpl(List<Embedding> embeddings) implements Output {

        @JsonCreator
        static OutputImpl of(
                @JsonProperty("embeddings") List<EmbeddingImpl> embeddings
        ) {
            return new OutputImpl(embeddings.stream().map(v -> (Embedding) v).sorted().toList());
        }

    }

    public record EmbeddingImpl(int index, float[] vector) implements Embedding {

        @JsonCreator
        static EmbeddingImpl of(
                @JsonProperty("text_index") int index,
                @JsonProperty("embedding") float[] vector
        ) {
            return new EmbeddingImpl(index, vector);
        }

    }

    @JsonCreator
    static EmbeddingResponseImpl of(
            @JsonProperty("request_id") String uuid,
            @JsonProperty("code") String code,
            @JsonProperty("message") String message,
            @JsonProperty("usage") Usage usage,
            @JsonProperty("output") OutputImpl output
    ) {
        return new EmbeddingResponseImpl(uuid, Ret.of(code, message), usage, output);
    }

}
