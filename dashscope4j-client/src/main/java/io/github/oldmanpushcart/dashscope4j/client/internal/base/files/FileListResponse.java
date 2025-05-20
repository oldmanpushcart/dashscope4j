package io.github.oldmanpushcart.dashscope4j.client.internal.base.files;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.base.files.FileMeta;
import io.github.oldmanpushcart.dashscope4j.client.base.files.Purpose;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.OpenAiError;
import io.github.oldmanpushcart.dashscope4j.client.internal.base.OpenAiResponse;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
class FileListResponse extends OpenAiResponse<List<FileMeta>> {

    List<FileMeta> output;
    boolean hasNext;

    @JsonCreator
    private FileListResponse(

            @JacksonInject("dashscope/request")
            FileListRequest request,

            @JacksonInject("http/header/x-request-id")
            String uuid,

            @JsonProperty("error")
            OpenAiError error,

            @JsonProperty("data")
            List<Data> list,

            @JsonProperty("has_more")
            boolean hasNext

    ) {
        super(request, uuid, error);

        this.output = list.stream()
                .map(Data::toMeta)
                .toList();
        this.hasNext = hasNext;
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

        FileMeta toMeta() {
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
