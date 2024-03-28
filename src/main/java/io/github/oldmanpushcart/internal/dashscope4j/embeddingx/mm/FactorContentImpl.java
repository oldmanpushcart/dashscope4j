package io.github.oldmanpushcart.internal.dashscope4j.embeddingx.mm;

import com.fasterxml.jackson.annotation.JsonValue;
import io.github.oldmanpushcart.dashscope4j.embeddingx.mm.FactorContent;

import java.util.Map;

public record FactorContentImpl<T>(float factor, Type type, T data) implements FactorContent<T> {

    public static final float DEFAULT_FACTOR = 1.0f;

    public FactorContentImpl(Type type, T data) {
        this(DEFAULT_FACTOR, type, data);
    }

    /**
     * @return Json Object Map
     */
    @JsonValue
    Map<Object, Object> extract() {
        return Map.of(type, data(), "factor", factor());
    }

}
