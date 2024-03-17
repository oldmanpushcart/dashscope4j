package io.github.oldmanpushcart.internal.dashscope4j.chat.tool;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface Tool {

    @JsonProperty("type")
    Classify classify();

    enum Classify {

        @JsonProperty("function")
        FUNCTION

    }

    interface Call {

        @JsonProperty("type")
        Classify classify();

    }

}
