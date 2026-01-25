package io.github.oldmanpushcart.dashscope4j.client.internal.aigc;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.Task;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcOp;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.aigc.interceptor.BridgeAsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.interceptor.BridgeFlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.interceptor.BridgeTaskInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.interceptor.IncrementalOutputOnlyInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.TaskInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.stream.Stream;

public class AigcOpImpl implements AigcOp {

    /**
     * 全局拦截器
     */
    private static final List<Interceptor> globalInterceptors = List.of(
            new BridgeAsyncInterceptor(),
            new BridgeTaskInterceptor(),
            new BridgeFlowInterceptor(),
            new IncrementalOutputOnlyInterceptor()
    );

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
    private static <T extends Interceptor> List<T> combineInterceptors(Class<T> type, List<? extends Interceptor> interceptorsFromRequest, List<Interceptor> interceptorsFromModel) {
        return Stream.of(globalInterceptors, interceptorsFromRequest, interceptorsFromModel)
                .flatMap(List::stream)
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    @Override
    public <I, O> CompletionStage<AigcResponse<O>> async(AigcRequest<I, O> request, List<AsyncInterceptor> interceptors) {
        final var combined = combineInterceptors(
                AsyncInterceptor.class,
                interceptors,
                request.model().interceptors()
        );
        return client.base().api()
                .async(request, combined);
    }

    @Override
    public <I, O> Flow.Publisher<AigcResponse<O>> flow(AigcRequest<I, O> request, List<FlowInterceptor> interceptors) {
        final var combined = combineInterceptors(
                FlowInterceptor.class,
                interceptors,
                request.model().interceptors()
        );
        return client.base().api()
                .flow(request, combined);
    }

    @Override
    public <I, O> CompletionStage<? extends Task.Half<AigcResponse<O>>> task(AigcRequest<I, O> request, List<TaskInterceptor> interceptors) {
        final var combined = combineInterceptors(
                TaskInterceptor.class,
                interceptors,
                request.model().interceptors()
        );
        return client.base().api()
                .task(request, combined);
    }
}
