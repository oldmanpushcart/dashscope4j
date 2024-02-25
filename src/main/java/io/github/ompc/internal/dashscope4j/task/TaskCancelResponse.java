package io.github.ompc.internal.dashscope4j.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.Usage;
import io.github.ompc.dashscope4j.api.ApiResponse;
import io.github.ompc.dashscope4j.api.ApiResponse.Output;

/**
 * 任务取消应答
 */
public record TaskCancelResponse(String uuid, Ret ret, Usage usage, Output output) implements ApiResponse<Output> {

    private TaskCancelResponse(String uuid, Ret ret) {
        this(uuid, ret, Usage.empty(), null);
    }

    @JsonCreator
    static TaskCancelResponse of(
            @JsonProperty("request_id")
            String uuid,
            @JsonProperty("code")
            String code,
            @JsonProperty("message")
            String message
    ) {
        return new TaskCancelResponse(uuid, Ret.of(code, message));
    }

}
