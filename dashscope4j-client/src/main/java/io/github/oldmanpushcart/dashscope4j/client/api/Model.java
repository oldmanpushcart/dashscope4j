package io.github.oldmanpushcart.dashscope4j.client.api;

import com.fasterxml.jackson.annotation.JsonValue;

public interface Model<I,O> {

    @JsonValue
    String name();

    String path();

    default Parameters parameters() {
        return new Parameters().unmodifiable();
    }

}
