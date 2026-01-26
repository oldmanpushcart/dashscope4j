package io.github.oldmanpushcart.dashscope4j.client.internal.api.task;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.oldmanpushcart.dashscope4j.client.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.Task;
import io.github.oldmanpushcart.dashscope4j.client.Usage;

import java.io.IOException;

@JsonDeserialize(using = TaskGetResponse.TaskGetResponseJsonDeserializer.class)
class TaskGetResponse extends ApiResponse {

    private final Usage usage;
    private final Task task;
    private final String raw;

    private TaskGetResponse(
            TaskGetRequest request,
            String uuid,
            String code,
            String desc,
            Usage usage,
            Task task,
            String raw
    ) {
        super(request, uuid, code, desc);
        this.usage = usage;
        this.task = task;
        this.raw = raw;
    }

    public Usage usage() {
        return usage;
    }

    public Task task() {
        return task;
    }

    public String raw() {
        return raw;
    }

    static class TaskGetResponseJsonDeserializer extends JsonDeserializer<TaskGetResponse> {

        @Override
        public TaskGetResponse deserialize(JsonParser parser, DeserializationContext context) throws IOException, JacksonException {
            final var node = (JsonNode) parser.getCodec().readTree(parser);
            final var data = context.readTreeAsValue(node, Data.class);
            return new TaskGetResponse(
                    data.request,
                    data.uuid,
                    data.code,
                    data.desc,
                    data.usage,
                    data.task,
                    node.toString()
            );
        }

        static final class Data {

            private final TaskGetRequest request;
            private final String uuid;
            private final String code;
            private final String desc;
            private final Usage usage;
            private final Task task;

            @JsonCreator
            private Data(

                    @JacksonInject("dashscope/request")
                    TaskGetRequest request,

                    @JsonProperty("request_id")
                    String uuid,

                    @JsonProperty("code")
                    String code,

                    @JsonProperty("message")
                    String desc,

                    @JsonProperty("usage")
                    Usage usage,

                    @JsonProperty("output")
                    Task task

            ) {
                this.request = request;
                this.uuid = uuid;
                this.code = code;
                this.desc = desc;
                this.usage = usage;
                this.task = task;
            }


        }

    }

}
