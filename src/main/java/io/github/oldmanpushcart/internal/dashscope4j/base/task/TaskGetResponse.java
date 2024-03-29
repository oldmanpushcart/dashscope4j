package io.github.oldmanpushcart.internal.dashscope4j.base.task;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;

import java.io.IOException;

/**
 * 任务获取应答
 */
@JsonDeserialize(using = TaskGetResponse.TaskGetResponseJsonDeserializer.class)
public record TaskGetResponse(String uuid, Ret ret, Usage usage, Output output, String raw)
        implements ApiResponse<TaskGetResponse.Output> {

    /**
     * 任务获取应答输出
     *
     * @param task 任务
     */
    @JsonDeserialize(using = Output.OutputJsonDeserializer.class)
    public record Output(Task task) implements ApiResponse.Output {

        static class OutputJsonDeserializer extends JsonDeserializer<Output> {

            @Override
            public Output deserialize(JsonParser parser, DeserializationContext context) throws IOException {
                final var node = context.readTree(parser);
                final var task = context.readTreeAsValue(node, Task.class);
                return new Output(task);
            }

        }

    }


    static class TaskGetResponseJsonDeserializer extends JsonDeserializer<TaskGetResponse> {

        @Override
        public TaskGetResponse deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            final var node = context.readTree(parser);
            return new TaskGetResponse(
                    node.get("request_id").asText(),
                    context.readTreeAsValue(node, Ret.class),
                    context.readTreeAsValue(node.get("usage"), Usage.class),
                    context.readTreeAsValue(node.get("output"), Output.class),
                    node.toString()
            );
        }

    }


}
