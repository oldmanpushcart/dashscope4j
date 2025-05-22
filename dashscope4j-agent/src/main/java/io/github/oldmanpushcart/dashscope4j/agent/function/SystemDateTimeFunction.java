package io.github.oldmanpushcart.dashscope4j.agent.function;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnDescription;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFnName;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import lombok.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletionStage;

import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * 系统当前时间函数
 */
@ChatFnName("system_date_time")
@ChatFnDescription("获取系统当前时间。在处理涉及时间相关问题时，需调用此函数获取系统当前时间以作校准")
public class SystemDateTimeFunction implements ChatFunction<SystemDateTimeFunction.Parameter, SystemDateTimeFunction.Result> {

    private static final String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

    @Override
    public CompletionStage<Result> call(Tool.Caller caller, Parameter parameter) {
        return completedFuture(
                new Result(
                        LocalDateTime.now().format(formatter),
                        pattern
                ));
    }

    @Value
    public static class Parameter {
    }

    public record Result(

            @JsonPropertyDescription("当前时间")
            @JsonProperty
            String datetime,

            @JsonPropertyDescription("时间格式")
            @JsonProperty
            String pattern

    ) {

    }

}
