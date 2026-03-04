package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 工具调用结果
 *
 * @param success    调用是否成功
 * @param data       调用结果
 * @param error      调用失败原因
 * @param suggestion 建议
 * @param <T>        调用结果数据类型
 */
public record ToolResult<T>(

        @JsonProperty("success")
        boolean success,

        @JsonProperty("data")
        T data,

        @JsonProperty("error")
        Throwable error,

        @JsonProperty("suggestion")
        String suggestion

) {

    public static <T> ToolResult<T> success(T data) {
        return new ToolResult<>(true, data, null, null);
    }

    public static <T> ToolResult<T> error(Throwable error) {
        final ToolExecutionException teCause;
        if (error instanceof ToolExecutionException cause) {
            teCause = cause;
        } else {
            teCause = ToolExecutionException.wrap(error);
        }
        return new ToolResult<>(
                false,
                null,
                teCause,
                teCause.getSuggestion()
        );
    }

}
