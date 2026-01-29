package io.github.oldmanpushcart.dashscope4j.client.api;

import com.fasterxml.jackson.annotation.JsonValue;

public interface Model {

    @JsonValue
    String name();

    String path();

}
