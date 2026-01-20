package io.github.oldmanpushcart.dashscope4j.client.aigc;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Set;

public interface Model<I, O> {

    @JsonValue
    String name();

    String path();

    default Set<String> tags() {
        return Set.of();
    }

    Class<I> inputType();
    Class<O> outputType();

}
