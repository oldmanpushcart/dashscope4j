package io.github.ompc.dashscope4j.internal.task;

import io.github.ompc.dashscope4j.Ret;
import io.github.ompc.dashscope4j.Usage;
import io.github.ompc.dashscope4j.internal.api.ApiResponse;

public class TaskCancelResponse extends ApiResponse<ApiResponse.Output> {

    private final String taskId;

    protected TaskCancelResponse(String uuid, Ret ret, String taskId) {
        super(uuid, ret, Usage.empty(), null);
        this.taskId = taskId;
    }

    public String taskId() {
        return taskId;
    }

}
