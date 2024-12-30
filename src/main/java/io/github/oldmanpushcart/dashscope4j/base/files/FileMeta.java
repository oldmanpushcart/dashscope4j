package io.github.oldmanpushcart.dashscope4j.base.files;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;
import java.time.Instant;

/**
 * 文件元数据
 */
@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public class FileMeta {

    String identity;
    String name;
    long size;

    @EqualsAndHashCode.Exclude
    Instant uploadedAt;

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
