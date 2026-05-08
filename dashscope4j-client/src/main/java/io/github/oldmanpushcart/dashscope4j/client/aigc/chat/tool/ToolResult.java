package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

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
        boolean ofSuccess,

        @JsonProperty("data")
        T data,

        @JsonIgnore
        Throwable error,

        @JsonProperty("suggestion")
        String suggestion

) {

    /**
     * 提取异常堆栈的关键信息
     * <p>
     * 当工具调用出现异常时，完整的异常堆栈非常多。
     * 这里将每一层的异常消息抽出并返回给LLM，方便其进行下一步的计划和执行
     * </p>
     *
     * @return 异常 cause
     */
    @JsonProperty("causes")
    List<String> causes() {
        final var causes = new ArrayList<String>();
        Throwable current = error;
        while (current != null) {
            causes.add(current.getClass().getSimpleName() + ": " + current.getMessage());
            current = current.getCause();
        }
        return causes;
    }

    public static <T> ToolResult<T> ofSuccess(T data) {
        return new ToolResult<>(true, data, null, null);
    }

    public static <T> ToolResult<T> ofError(String name, Throwable error) {
        final ToolExecutionException teCause;
        if (error instanceof ToolExecutionException cause) {
            teCause = cause;
        } else {
            teCause = ToolExecutionException.wrap(name, error);
        }
        return new ToolResult<>(
                false,
                null,
                teCause,
                teCause.getSuggestion()
        );
    }

}
