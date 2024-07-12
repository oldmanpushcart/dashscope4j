package io.github.oldmanpushcart.internal.dashscope4j.embedding.text;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.embedding.text.Embedding;
import io.github.oldmanpushcart.dashscope4j.embedding.text.EmbeddingResponse;

import java.util.List;

import static java.util.Comparator.comparingInt;

record EmbeddingResponseImpl(String uuid, Ret ret, Usage usage, Output output) implements EmbeddingResponse {

    public record OutputImpl(List<Embedding> embeddings) implements Output {

        @JsonCreator
        static OutputImpl of(

                @JsonProperty("embeddings")
                List<EmbeddingImpl> embeddings

        ) {
            embeddings.sort(comparingInt(o -> o.index));
            return new OutputImpl(embeddings.stream().map(v -> (Embedding) v).toList());
        }

    }

    public record EmbeddingImpl(

            @JsonProperty("text_index")
            int index,

            @JsonProperty("embedding")
            float[] vector

    ) implements Embedding {

    }

    @JsonCreator
    static EmbeddingResponseImpl of(

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String message,

            @JsonProperty("usage")
            Usage usage,

            @JsonProperty("output")
            OutputImpl output

    ) {
        return new EmbeddingResponseImpl(uuid, Ret.of(code, message), usage, output);
    }

}
