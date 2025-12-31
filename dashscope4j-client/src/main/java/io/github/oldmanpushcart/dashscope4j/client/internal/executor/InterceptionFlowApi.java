package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.DeferredPublisher;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.ErrorPublisher;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

public class InterceptionFlowApi implements FlowApi {

    private final DashscopeClient client;
    private final FlowApi delegate;
    private final Interceptor interceptor;

    public InterceptionFlowApi(DashscopeClient client, FlowApi delegate, Interceptor interceptor) {
        this.client = client;
        this.delegate = delegate;
        this.interceptor = interceptor;
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> execute(URI endpoint, T request) {
        final var chain = new Interceptor.Chain(client, request, r -> CompletableFuture.completedStage(delegate.execute(endpoint, r)));
        try {
            //noinspection unchecked
            return new DeferredPublisher<>(() -> interceptor.intercept(chain).thenApply(r -> (Flow.Publisher<R>) r));
        } catch (Throwable ex) {
            return new ErrorPublisher<>(ex);
        }
    }

    public static FlowApi group(DashscopeClient client, FlowApi delegate, List<Interceptor> interceptors) {
        FlowApi api = delegate;
        for (final var interceptor : interceptors) {
            api = new InterceptionFlowApi(client, api, interceptor);
        }
        return api;
    }

}
