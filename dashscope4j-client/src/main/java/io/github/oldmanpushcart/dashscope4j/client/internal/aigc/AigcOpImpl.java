package io.github.oldmanpushcart.dashscope4j.client.internal.aigc;

import io.github.oldmanpushcart.dashscope4j.client.*;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcOp;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.TaskInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class AigcOpImpl implements AigcOp {

    private final DashscopeClient client;

    public AigcOpImpl(DashscopeClient client) {
        this.client = client;
    }

    private static <T extends Interceptor> List<T> mergeInterceptors(Class<T> type, AigcModel<?, ?> model, List<T> interceptors) {
        final var merged = new ArrayList<T>();
        merged.addAll(interceptors);
        merged.addAll(model.interceptors().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList());
        return merged;
    }

    @Override
    public <I, O> CompletionStage<AigcResponse<O>> async(AigcRequest<I, O> request, List<AsyncInterceptor> interceptors) {
        final var merged = mergeInterceptors(AsyncInterceptor.class, request.model(), interceptors);
        return client.base().api().async(request, merged);
    }

    @Override
    public <I, O> Flow.Publisher<AigcResponse<O>> flow(AigcRequest<I, O> request, List<FlowInterceptor> interceptors) {
        final var merged = mergeInterceptors(FlowInterceptor.class, request.model(), interceptors);
        return client.base().api().flow(request, merged);
    }

    @Override
    public <I, O> CompletionStage<? extends Task.Half<AigcResponse<O>>> task(AigcRequest<I, O> request, List<TaskInterceptor> interceptors) {
        final var merged = mergeInterceptors(TaskInterceptor.class, request.model(), interceptors);
        return client.base().api().task(request, merged);
    }
}
