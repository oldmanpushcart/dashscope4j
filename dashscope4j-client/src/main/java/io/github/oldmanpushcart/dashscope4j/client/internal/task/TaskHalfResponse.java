package io.github.oldmanpushcart.dashscope4j.client.internal.task;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.task.Task;

public class TaskHalfResponse extends ApiResponse {

    private final Output output;

    @JsonCreator
    protected TaskHalfResponse(

            @JacksonInject("dashscope/request")
            ApiRequest<?> request,

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String desc,

            @JsonProperty("output")
            Output output

    ) {
        super(request, uuid, code, desc);
        this.output = output;
    }

    public Output output() {
        return output;
    }

    public record Output(

            @JsonProperty("task_id")
            String taskId,

            @JsonProperty("task_status")
            Task.Status status

    ) {

    }

}
