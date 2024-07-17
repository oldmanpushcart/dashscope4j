package io.github.oldmanpushcart.dashscope4j.chat.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;

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
     * @return 插件参数
     */
    Map<String, Object> arguments();

    /**
     * 插件调用
     *
     * @param name      插件名称
     * @param arguments 调用参数
     */
    record Call(

            @JsonProperty("name")
            String name,

            @JsonProperty("arguments")
            String arguments

    ) {
    }

    /**
     * 插件调用结果状态
     *
     * @param code 状态代码
     * @param name 状态名称
     * @param desc 状态描述
     */
    record Status(

            @JsonProperty("code")
            int code,

            @JsonProperty("name")
            String name,

            @JsonProperty("message")
            String desc

    ) {
    }

}
