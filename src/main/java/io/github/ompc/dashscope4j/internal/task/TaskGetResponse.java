package io.github.ompc.dashscope4j.internal.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.Task;
import io.github.ompc.dashscope4j.Usage;
import io.github.ompc.dashscope4j.internal.api.ApiResponse;

import java.io.IOException;

/**
 * 任务获取应答
 */
public final class TaskGetResponse extends ApiResponse<TaskGetResponse.Output> {

    private TaskGetResponse(String uuid, Ret ret, Usage usage, Output output) {
        super(uuid, ret, usage, output);
    }

    /**
     * 任务获取应答输出
     *
     * @param task 任务
     * @param body 任务内容
     */
    @JsonDeserialize(using = Output.OutputJsonDeserializer.class)
    public record Output(Task task, String body) implements ApiResponse.Output {

        static class OutputJsonDeserializer extends JsonDeserializer<Output> {

            @Override
            public Output deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                final var node = context.readTree(parser);
                final var task = context.readTreeAsValue(node, Task.class);
                final var body = node.toString();
                return new Output(task, body);
            }

        }

    }

    @JsonCreator
    static TaskGetResponse of(
            @JsonProperty("request_id")
            String uuid,
            @JsonProperty("code")
            String code,
            @JsonProperty("message")
            String message,
            @JsonProperty("usage")
            Usage usage,
            @JsonProperty("output")
            Output output
    ) {
        return new TaskGetResponse(uuid, Ret.of(code, message), usage, output);
    }


}
