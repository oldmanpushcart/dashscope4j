package io.github.oldmanpushcart.dashscope4j.client.internal.base.api;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.base.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowApi;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.TaskApi;
import io.github.oldmanpushcart.dashscope4j.client.task.Task;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class ApiOpImpl implements ApiOp {

    private final AsyncApi asyncApi;
    private final FlowApi flowApi;
    private final TaskApi taskApi;

    public ApiOpImpl(AsyncApi asyncApi, FlowApi flowApi, TaskApi taskApi) {
        this.asyncApi = asyncApi;
        this.flowApi = flowApi;
        this.taskApi = taskApi;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> async(T request) {
        return asyncApi.execute(request);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> flow(T request) {
        return flowApi.execute(request);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> task(T request) {
        return taskApi.execute(request);
    }
}
