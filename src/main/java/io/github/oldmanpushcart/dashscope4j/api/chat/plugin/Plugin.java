package io.github.oldmanpushcart.dashscope4j.api.chat.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

/**
 * 插件
 */
public interface Plugin {

    /**
     * @return 插件名称
     */
    String name();

    /**
     * @return 插件元数据
     */
    Map<String, Object> meta();

    /**
     * 插件调用存根
     */
    @Value
    @Accessors(fluent = true)
    @Builder(access = AccessLevel.PRIVATE)
    @Jacksonized
    class Call {

        /**
         * 插件名称
         */
        @JsonProperty
        String name;

        /**
         * 调用参数
         */
        @JsonProperty
        String arguments;

    }

    /**
     * 插件应答状态
     */
    @Value
    @Accessors(fluent = true)
    @Builder(access = AccessLevel.PRIVATE)
    @Jacksonized
    class Status {

        /**
         * 状态代码
         */
        @JsonProperty
        int code;

        /**
         * 状态名称
         */
        @JsonProperty
        String name;

        /**
         * 状态描述
         */
        @JsonProperty("message")
        String desc;

    }

}
