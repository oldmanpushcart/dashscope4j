package io.github.oldmanpushcart.dashscope4j.client.internal.task;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.task.Task;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.jackson.Jacksonized;

@Value
@Accessors(fluent = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class TaskHalfResponse extends ApiResponse<TaskHalfResponse.Output> {

    Output output;

    @JsonCreator
    private TaskHalfResponse(

            @JacksonInject("dashscope/request")
            ApiRequest<?> request,

            @JsonProperty("request_id")
            String uuid,

            @JsonProperty("code")
            String code,

            @JsonProperty("message")
            String message,

            @JsonProperty("output")
            Output output

    ) {
        super(request, uuid, code, message);
        this.output = output;
    }

    @Value
    @Accessors(fluent = true)
    @Jacksonized
    @Builder(access = AccessLevel.PRIVATE)
    public static class Output {

        @JsonProperty("task_id")
        String taskId;

        @JsonProperty("task_status")
        Task.Status status;

    }

}
