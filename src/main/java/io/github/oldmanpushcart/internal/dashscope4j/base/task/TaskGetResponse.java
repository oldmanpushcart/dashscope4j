package io.github.oldmanpushcart.internal.dashscope4j.base.task;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;

import java.io.IOException;

import static io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils.getAsText;

/**
 * 任务获取应答
 */
@JsonDeserialize(using = TaskGetResponse.TaskGetResponseJsonDeserializer.class)
public record TaskGetResponse(String uuid, Ret ret, Usage usage, Output output, String raw)
        implements HttpApiResponse<TaskGetResponse.Output> {

    /**
     * 任务获取应答输出
     *
     * @param task 任务
     */
    @JsonDeserialize(using = Output.OutputJsonDeserializer.class)
    public record Output(Task task) {

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
            final var outputNode = node.get("output");
            final var usageNode = node.get("usage");
            return new TaskGetResponse(
                    node.get("request_id").asText(),
                    deserializeRet(outputNode),
                    context.readTreeAsValue(usageNode, Usage.class),
                    context.readTreeAsValue(outputNode, Output.class),
                    node.toString()
            );
        }

        private Ret deserializeRet(JsonNode node) {
            final var code = getAsText(node, "code");
            final var message = getAsText(node, "message");
            return Ret.of(code, message);
        }

    }


}
