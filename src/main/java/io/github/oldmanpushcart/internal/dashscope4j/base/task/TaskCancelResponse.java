package io.github.oldmanpushcart.internal.dashscope4j.base.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse.Output;

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
