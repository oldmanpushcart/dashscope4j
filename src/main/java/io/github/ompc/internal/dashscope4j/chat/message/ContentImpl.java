package io.github.ompc.internal.dashscope4j.chat.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.github.ompc.dashscope4j.chat.message.Content;

import java.net.URI;
import java.util.Map;

/**
 * 内容实现
 *
 * @param type 类型
 * @param data 数据
 * @param <T>
 */
public record ContentImpl<T>(Type type, T data) implements Content<T> {

    /**
     * 序列化为 {@code {"<TYPE>":"<DATA>"}} 格式
     *
     * @return Json Object Map
     */
    @JsonValue
    Map<Type, T> extract() {
        return Map.of(type, data());
    }

    /**
     * 反序列化
     *
     * @param map Json Object Map
     * @return 内容
     */
    @JsonCreator
    static ContentImpl<?> of(Map<String, String> map) {
        return map.entrySet().stream()
                .map(entry -> switch (entry.getKey()) {
                    case "text" -> new ContentImpl<>(Type.TEXT, entry.getValue());
                    case "image" -> new ContentImpl<>(Type.IMAGE, URI.create(entry.getValue()));
                    case "audio" -> new ContentImpl<>(Type.AUDIO, URI.create(entry.getValue()));
                    default -> throw new IllegalArgumentException("Unknown content-type: %s".formatted(entry.getKey()));
                })
                .findFirst()
                .orElse(null);
    }


}
