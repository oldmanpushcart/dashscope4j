package io.github.oldmanpushcart.dashscope4j.client.aigc;

import io.github.oldmanpushcart.dashscope4j.client.interceptor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.Task;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.TaskInterceptor;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface AigcOp {

    <I, O> CompletionStage<AigcResponse<O>> async(AigcRequest<I, O> request, List<AsyncInterceptor> interceptors);

    <I, O> Flow.Publisher<AigcResponse<O>> flow(AigcRequest<I, O> request, List<FlowInterceptor> interceptors);

    <I, O> CompletionStage<? extends Task.Half<AigcResponse<O>>> task(AigcRequest<I, O> request, List<TaskInterceptor> interceptors);

    default <I, O> CompletionStage<AigcResponse<O>> async(AigcRequest<I, O> request) {
        return async(request, List.of());
    }

    default <I, O> Flow.Publisher<AigcResponse<O>> flow(AigcRequest<I, O> request) {
        return flow(request, List.of());
    }

    default <I, O> CompletionStage<? extends Task.Half<AigcResponse<O>>> task(AigcRequest<I, O> request) {
        return task(request, List.of());
    }

}
