package io.github.oldmanpushcart.internal.dashscope4j.base.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;

/**
 * 任务半应答
 */
public record TaskHalfResponse(String uuid, Ret ret, Usage usage, Output output)
        implements HttpApiResponse<TaskHalfResponse.Output> {

    public record Output(String taskId, Task.Status status) {

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
