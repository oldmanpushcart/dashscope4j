package io.github.oldmanpushcart.dashscope4j.api.audio.voice;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.URI;
import java.time.Instant;

/**
 * 音色
 *
 * @since 3.1.0
 */
@Value
@Accessors(fluent = true)
@ToString
@EqualsAndHashCode
public class Voice {

    /**
     * 音色ID
     */
    String identity;

    /**
     * 目标模型名称
     */
    String target;

    /**
     * 创建时间
     */
    Instant createdAt;

    /**
     * 修改时间
     */
    Instant updatedAt;

    /**
     * 音色资源
     */
    URI resource;

}
