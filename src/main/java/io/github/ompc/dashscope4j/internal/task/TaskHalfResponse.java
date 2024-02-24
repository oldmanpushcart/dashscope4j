package io.github.ompc.dashscope4j.internal.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.Task;
import io.github.ompc.dashscope4j.Usage;
import io.github.ompc.dashscope4j.internal.api.ApiResponse;

public final class TaskHalfResponse extends ApiResponse<TaskHalfResponse.Output> {

    private TaskHalfResponse(String uuid, Ret ret, Usage usage, Output output) {
        super(uuid, ret, usage, output);
    }

    public record Output(String taskId, Task.Status status) implements ApiResponse.Output {

        @JsonCreator
        static Output of(
                @JsonProperty("task_id")
                String taskId,
                @JsonProperty("task_status")
                Task.Status status
        ) {
            return new Output(taskId, status);
        }

    }

    @JsonCreator
    static TaskHalfResponse of(
            @JsonProperty("request_id")
            String uuid,
            @JsonProperty("code")
            String code,
            @JsonProperty("message")
            String message,
            @JsonProperty("output")
            Output output
    ) {
        return new TaskHalfResponse(uuid, Ret.of(code, message), Usage.empty(), output);
    }

}
