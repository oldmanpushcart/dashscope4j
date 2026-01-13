package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.client.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.OpenAiError;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.OpenAiResponse;

import java.time.Instant;
import java.util.List;

public class FileListResponse extends OpenAiResponse {

    private final List<FileMeta> metas;
    private final boolean hasNext;

    @JsonCreator
    private FileListResponse(

            @JacksonInject("dashscope/request")
            FileListRequest request,

            @JacksonInject("header/x-request-id")
            String uuid,

            @JsonProperty("error")
            OpenAiError error,

            @JsonProperty("data")
            List<Data> list,

            @JsonProperty("has_more")
            boolean hasNext

    ) {
        super(request, uuid, error);
        this.metas = null != list
                ? list.stream().map(Data::toMeta).toList()
                : List.of();
        this.hasNext = hasNext;
    }

    public List<FileMeta> metas() {
        return metas;
    }

    public boolean hasNext() {
        return hasNext;
    }

    private record Data(

            @JsonProperty("id")
            String id,

            @JsonProperty("object")
            String object,

            @JsonProperty("bytes")
            long bytes,

            @JsonProperty("created_at")
            int created,

            @JsonProperty("filename")
            String filename,

            @JsonProperty("purpose")
            Purpose purpose,

            @JsonProperty("status")
            String status

    ) {

        public FileMeta toMeta() {
            return new FileMeta(
                    id,
                    filename,
                    bytes,
                    Instant.ofEpochSecond(created),
                    purpose
            );
        }

    }

}
