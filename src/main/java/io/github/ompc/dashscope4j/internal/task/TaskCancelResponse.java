package io.github.ompc.dashscope4j.internal.task;

import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.Usage;
import io.github.ompc.dashscope4j.api.ApiResponse;
import io.github.ompc.dashscope4j.api.ApiResponse.Output;

/**
 * 任务取消应答
 */
public record TaskCancelResponse(String uuid, Ret ret, Usage usage, Output output) implements ApiResponse<Output> {

    public TaskCancelResponse(String uuid, Ret ret) {
        this(uuid, ret, Usage.empty(), null);
    }

}
