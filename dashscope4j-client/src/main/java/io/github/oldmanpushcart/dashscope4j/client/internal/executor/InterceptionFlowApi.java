package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public class InterceptionFlowApi implements FlowApi {

    private final DashscopeClient client;
    private final FlowApi delegate;
    private final FlowInterceptor interceptor;

    public InterceptionFlowApi(DashscopeClient client, FlowApi delegate, FlowInterceptor interceptor) {
        this.client = client;
        this.delegate = delegate;
        this.interceptor = interceptor;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> execute(T request) {
        return FlowX.defer(() -> {
            final var chain = new FlowInterceptor.Chain(client, request, r -> CompletableFuture.completedStage(delegate.execute(r)));
            final var stage = interceptor.intercept(chain)
                    .thenApply(r -> {
                        //noinspection unchecked
                        return (Flow.Publisher<R>) r;
                    });
            return FlowX.fromCompletionStage(stage);
        });
    }

    public static FlowApi group(DashscopeClient client, FlowApi delegate, List<FlowInterceptor> interceptors) {
        FlowApi api = delegate;
        for (final var interceptor : interceptors) {
            api = new InterceptionFlowApi(client, api, interceptor);
        }
        return api;
    }

}
