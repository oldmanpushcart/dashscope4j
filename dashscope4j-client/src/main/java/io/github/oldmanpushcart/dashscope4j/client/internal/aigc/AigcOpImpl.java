package io.github.oldmanpushcart.dashscope4j.client.internal.aigc;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Task;
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

    /**
     * 合并拦截器
     * <p>
     * 会严格按照以下顺序拼接拦截器链条
     * <li>1. 全局拦截器（目前没有）</li>
     * <li>2. 请求拦截器</li>
     * <li>3. 模型拦截器</li>
     * </p>
     *
     * @param interceptorsFromRequest 来自请求传入的拦截器
     * @param interceptorsFromModel   来自模型声明的拦截器
     * @param <T>                     拦截器类型
     * @return 合并后的拦截器链
     */
    private static <T extends Interceptor> List<T> mergeInterceptors(List<T> interceptorsFromRequest, List<T> interceptorsFromModel) {
        final var merged = new ArrayList<T>();
        merged.addAll(interceptorsFromRequest);
        merged.addAll(interceptorsFromModel);
        return merged;
    }

    @Override
    public <I, O> CompletionStage<AigcResponse<O>> async(AigcRequest<I, O> request, List<AsyncInterceptor> interceptors) {
        final var merged = mergeInterceptors(
                interceptors,
                request.model().interceptors().stream()
                        .filter(AsyncInterceptor.class::isInstance)
                        .map(AsyncInterceptor.class::cast)
                        .toList()
        );
        return client.base().api().async(request, merged);
    }

    @Override
    public <I, O> Flow.Publisher<AigcResponse<O>> flow(AigcRequest<I, O> request, List<FlowInterceptor> interceptors) {
        final var merged = mergeInterceptors(
                interceptors,
                request.model().interceptors().stream()
                        .filter(FlowInterceptor.class::isInstance)
                        .map(FlowInterceptor.class::cast)
                        .toList()
        );
        return client.base().api().flow(request, merged);
    }

    @Override
    public <I, O> CompletionStage<? extends Task.Half<AigcResponse<O>>> task(AigcRequest<I, O> request, List<TaskInterceptor> interceptors) {
        final var merged = mergeInterceptors(
                interceptors,
                request.model().interceptors().stream()
                        .filter(TaskInterceptor.class::isInstance)
                        .map(TaskInterceptor.class::cast)
                        .toList()
        );
        return client.base().api().task(request, merged);
    }
}
