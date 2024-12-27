package io.github.oldmanpushcart.dashscope4j.internal.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.task.Task;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@JsonDeserialize(using = TaskGetResponse.TaskGetResponseJsonDeserializer.class)
public class TaskGetResponse extends ApiResponse<TaskGetResponse.Output> {

    Output output;
    Usage usage;
    String raw;

    private TaskGetResponse(
            String uuid,
            String code,
            String message,
            Output output,
            Usage usage,
            String raw
    ) {
        super(uuid, code, message);
        this.output = output;
        this.usage = usage;
        this.raw = raw;
    }

    static class TaskGetResponseJsonDeserializer extends JsonDeserializer<TaskGetResponse> {

        @Override
        public TaskGetResponse deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            final JsonNode node = context.readTree(parser);
            final JsonNode outputNode = node.required("output");
            return new TaskGetResponse(
                    node.required("request_id").asText(),
                    Optional.ofNullable(outputNode.get("code")).map(JsonNode::asText).orElse(null),
                    Optional.ofNullable(outputNode.get("message")).map(JsonNode::asText).orElse(null),
                    context.readTreeAsValue(outputNode, Output.class),
                    deserializeUsage(context, node.get("usage")),
                    node.toString()
            );
        }

        private Usage deserializeUsage(DeserializationContext context, JsonNode usageNode) throws IOException {
            return Objects.nonNull(usageNode)
                    ? context.readTreeAsValue(usageNode, Usage.class)
                    : Usage.empty();
        }

    }

    @Value
    @Accessors(fluent = true)
    @ToString
    @EqualsAndHashCode
    public static class Output {

        Task task;

        @JsonCreator
        private Output(

                @JsonProperty("task_id")
                String taskId,

                @JsonProperty("task_status")
                Task.Status status,

                @JsonProperty("task_metrics")
                Task.Metrics metrics,

                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
                @JsonProperty("submit_time")
                Date submitTime,

                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
                @JsonProperty("scheduled_time")
                Date scheduledTime,

                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss.SSS")
                @JsonProperty("end_time")
                Date endTime

        ) {
            this.task = new Task(taskId, status, metrics, new Task.Timing(submitTime, scheduledTime, endTime));
        }

    }

}
