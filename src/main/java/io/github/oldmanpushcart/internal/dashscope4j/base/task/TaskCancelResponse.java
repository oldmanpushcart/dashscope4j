package io.github.oldmanpushcart.internal.dashscope4j.base.task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.Ret;
import io.github.oldmanpushcart.dashscope4j.Usage;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiResponse;

/**
 * 任务取消应答
 */
public record TaskCancelResponse(String uuid, Ret ret) implements HttpApiResponse<Object> {

    @Override
    public Object output() {
        return null;
    }

    @Override
    public Usage usage() {
        return Usage.empty();
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
