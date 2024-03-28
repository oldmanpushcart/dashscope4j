package io.github.oldmanpushcart.internal.dashscope4j.embeddingx.mm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbedding;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.MmEmbeddingResponse;

record MmEmbeddingResponseImpl(String uuid, Ret ret, Usage usage, Output output) implements MmEmbeddingResponse {

    record OutputImpl(MmEmbedding embedding) implements Output {

        @JsonCreator
        static OutputImpl of(
                @JsonProperty("embedding")
                float[] vector
        ) {
            return new OutputImpl(new MmEmbeddingImpl(vector));
        }

    }

    record MmEmbeddingImpl(float[] vector) implements MmEmbedding {

    }

    @JsonCreator
    static MmEmbeddingResponseImpl of(
            @JsonProperty("request_id")
            String uuid,
            @JsonProperty("code")
            String code,
            @JsonProperty("message")
            String message,
            @JsonProperty("usage")
            Usage usage,
            @JsonProperty("output")
            MmEmbeddingResponseImpl.OutputImpl output
    ) {
        return new MmEmbeddingResponseImpl(uuid, Ret.of(code, message), usage, output);
    }

}
