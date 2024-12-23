package io.github.oldmanpushcart.dashscope4j.internal.api;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.Exchange;
import io.github.oldmanpushcart.dashscope4j.Interceptor;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@AllArgsConstructor
public class InterceptionApiOp implements ApiOp {

    private final DashscopeClient client;
    private final ApiOp apiOp;
    private final Interceptor interceptor;

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<R> executeAsync(T request) {
        final Interceptor.Chain chain = new ChainImpl(client, request, apiOp::executeAsync);
        return interceptor.intercept(chain)
                .thenApply(InterceptionApiOp::cast);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<Flowable<R>> executeFlow(T request) {
        final Interceptor.Chain chain = new ChainImpl(client, request, apiOp::executeFlow);
        return interceptor.intercept(chain)
                .thenApply(InterceptionApiOp::cast);
    }

    @Override
    public <T extends ApiRequest<R>, R extends ApiResponse<?>>
    CompletionStage<Exchange<T>> executeExchange(T request, Exchange.Mode mode, Exchange.Listener<T, R> listener) {
        final Interceptor.Chain chain = new ChainImpl(client, request, r -> apiOp.executeExchange(cast(r), mode, listener));
        return interceptor.intercept(chain)
                .thenApply(InterceptionApiOp::cast);
    }

    @SuppressWarnings("unchecked")
    private static <V> V cast(Object obj) {
        return (V) obj;
    }

    @Value
    @Accessors(fluent = true)
    private static class ChainImpl implements Interceptor.Chain {

        DashscopeClient client;
        ApiRequest<?> request;
        Function<ApiRequest<?>, CompletionStage<?>> applier;

        @Override
        public CompletionStage<?> process(ApiRequest<?> request) {
            return applier.apply(request);
        }

    }

    public static ApiOp group(DashscopeClient client, ApiOp apiOp, Collection<Interceptor> interceptors) {
        ApiOp op = apiOp;
        for (Interceptor interceptor : interceptors) {
            op = new InterceptionApiOp(client, op, interceptor);
        }
        return op;
    }

}
