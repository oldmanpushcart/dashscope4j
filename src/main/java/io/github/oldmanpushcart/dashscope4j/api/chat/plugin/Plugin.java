package io.github.oldmanpushcart.dashscope4j.api.chat.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

public interface Plugin {

    /**
     * @return 插件名称
     */
    String name();

    /**
     * @return 插件参数
     */
    Map<String, Object> arguments();

    @Value
    @Accessors(fluent = true)
    @Builder(access = AccessLevel.PRIVATE)
    @Jacksonized
    class Call {

        @JsonProperty
        String name;

        @JsonProperty
        String arguments;

    }

    @Value
    @Accessors(fluent = true)
    @Builder(access = AccessLevel.PRIVATE)
    @Jacksonized
    class Status {

        @JsonProperty
        int code;

        @JsonProperty
        String name;

        @JsonProperty("message")
        String desc;

    }

}
