package io.github.oldmanpushcart.dashscope4j.base.files;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.net.URI;
import java.time.Instant;

/**
 * 文件元数据
 */
@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
@Jacksonized
@Builder
@AllArgsConstructor
public class FileMeta {

    @JsonProperty("identity")
    String identity;

    @JsonProperty("filename")
    String name;

    @JsonProperty("bytes")
    long size;

    @EqualsAndHashCode.Exclude
    @JsonProperty("create_at")
    Instant uploadedAt;

    @JsonProperty("purpose")
    Purpose purpose;

    /**
     * 转换为URI
     *
     * @return URI
     */
    public URI toURI() {
        return URI.create(String.format("fileid://%s", identity));
    }

}
