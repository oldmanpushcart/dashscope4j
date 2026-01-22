package io.github.oldmanpushcart.dashscope4j.client;

import com.fasterxml.jackson.annotation.JsonValue;

public interface Model {

    @JsonValue
    String name();

    String path();

}
