package io.github.oldmanpushcart.internal.dashscope4j.base.resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.base.files.FileMeta;

public record FileMetaImpl(
        String id,
        String name,
        long size,
        long uploadedAt,
        String purpose
) implements FileMeta {

    @JsonCreator
    static FileMetaImpl of(

            @JsonProperty("id")
            String id,

            @JsonProperty("filename")
            String name,

            @JsonProperty("bytes")
            long size,

            @JsonProperty("created_at")
            int created,

            @JsonProperty("purpose")
            String purpose

    ) {
        return new FileMetaImpl(
                id,
                name,
                size,
                created * 1000L,
                purpose
        );
    }

}
