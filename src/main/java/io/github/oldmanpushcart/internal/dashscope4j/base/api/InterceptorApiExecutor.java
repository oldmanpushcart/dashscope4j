package io.github.oldmanpushcart.internal.dashscope4j.base.api;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.base.api.ExchangeApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ExchangeApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.HttpApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.exchange.Exchange;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.internal.dashscope4j.base.exchange.ProxyExchange;
import io.github.oldmanpushcart.internal.dashscope4j.base.exchange.ProxyExchangeListener;
import io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.*;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils.cast;
import static io.github.oldmanpushcart.internal.dashscope4j.util.MapFlowProcessor.asyncOneToOne;

/**
 * API执行器（拦截器实现）
 */
public class InterceptorApiExecutor implements ApiExecutor {

    private final DashScopeClient client;
    private final Executor executor;
    private final Interceptor interceptor;
    private final ApiExecutor target;

    public InterceptorApiExecutor(DashScopeClient client, Executor executor, Interceptor interceptor, ApiExecutor target) {
        this.client = client;
        this.executor = executor;
        this.interceptor = interceptor;
        this.target = target;
    }

    @Override
    public <R extends HttpApiResponse<?>> CompletionStage<R> async(HttpApiRequest<R> request) {
        final var context = new CtxImpl(client, executor);
        return CompletableFuture.completedFuture(null)

                // pre-handle
                .thenCompose(unused -> interceptor.preHandle(context, request))

                // handle
                .thenCompose(req -> interceptor.handle(context, req, v -> target.async(cast(v))))
                .thenApply(CommonUtils::<R>cast)

                // post-handle
                .handle((resp, ex) -> interceptor.postHandle(context, resp, ex))
                .thenCompose(v -> v)
                .thenApply(CommonUtils::cast);

    }

    @Override
    public <R extends HttpApiResponse<?>> CompletionStage<Flow.Publisher<R>> flow(HttpApiRequest<R> request) {
        final var context = new CtxImpl(client, executor);
        return CompletableFuture.completedFuture(null)

                // pre-handle
                .thenCompose(unused -> interceptor.preHandle(context, request))

                // handle
                .thenCompose(req -> interceptor.handle(context, req, v -> target.flow(cast(v))))
                .thenApply(CommonUtils::<Flow.Publisher<R>>cast)

                // post-handle
                .thenApply(p -> asyncOneToOne(p, (r, ex) -> interceptor.postHandle(context, r, ex).thenApply(CommonUtils::<R>cast)));

    }

    @Override
    public <R extends HttpApiResponse<?>> CompletionStage<Task.Half<R>> task(HttpApiRequest<R> request) {
        final var context = new CtxImpl(client, executor);
        return CompletableFuture.completedFuture(null)

                // pre-handle
                .thenCompose(unused -> interceptor.preHandle(context, request))

                // handle
                .thenCompose(req -> interceptor.handle(context, req, v -> target.task(cast(v))))
                .thenApply(CommonUtils::<Task.Half<R>>cast)

                // post-handle
                .thenApply(half -> strategy -> half.waitingFor(strategy)
                        .handle((resp, ex) -> interceptor.postHandle(context, resp, ex))
                        .thenCompose(v -> v)
                        .thenApply(CommonUtils::cast)
                );

    }

    /**
     * 调用上下文
     *
     * @param client        DashScope客户端
     * @param executor      执行器
     * @param attachmentMap 附件集合
     */
    private record CtxImpl(
            DashScopeClient client,
            Executor executor,
            Map<String, Object> attachmentMap
    ) implements InvocationContext {

        public CtxImpl(DashScopeClient client, Executor executor) {
            this(client, executor, new ConcurrentHashMap<>());
        }

    }

    @Override
    public <T extends ExchangeApiRequest<R>, R extends ExchangeApiResponse<?>>
    CompletionStage<Exchange<T, R>> exchange(T request, Exchange.Mode mode, Exchange.Listener<T, R> listener) {
        final var context = new CtxImpl(client, executor);
        return CompletableFuture.completedFuture(null)
                .thenCompose(unused -> interceptor.preHandle(context, request))
                .thenCompose(req -> interceptor.handle(context, req, v -> target.exchange(cast(v), mode, new ProxyExchangeListener<>(listener) {

                    @Override
                    public CompletionStage<?> onData(Exchange<T, R> exchange, R data) {
                        return CompletableFuture.completedFuture(data)
                                .handle((r, ex) -> interceptor.postHandle(context, r, ex))
                                .thenCompose(r -> r)
                                .<R>thenApply(CommonUtils::cast)
                                .thenCompose(r -> listener.onData(new InterceptorExchange<>(context, exchange), r));
                    }

                    @Override
                    public void onOpen(Exchange<T, R> exchange) {
                        super.onOpen(new InterceptorExchange<>(context, exchange));
                    }

                    @Override
                    public CompletionStage<?> onByteBuffer(Exchange<T, R> exchange, ByteBuffer buf, boolean last) {
                        return super.onByteBuffer(new InterceptorExchange<>(context, exchange), buf, last);
                    }

                    @Override
                    public CompletionStage<?> onCompleted(Exchange<T, R> exchange, int status, String reason) {
                        return super.onCompleted(new InterceptorExchange<>(context, exchange), status, reason);
                    }

                    @Override
                    public void onError(Exchange<T, R> exchange, Throwable ex) {
                        super.onError(new InterceptorExchange<>(context, exchange), ex);
                    }

                })))
                .thenApply(CommonUtils::cast);
    }

    private class InterceptorExchange<T extends ExchangeApiRequest<R>, R extends ExchangeApiResponse<?>> extends ProxyExchange<T, R> {

        private final InvocationContext context;

        public InterceptorExchange(InvocationContext context, Exchange<T, R> target) {
            super(target);
            this.context = context;
        }

        @Override
        public CompletionStage<Exchange<T, R>> write(T data) {
            return CompletableFuture.completedFuture(null)
                    .thenCompose(unused -> interceptor.preHandle(context, data))
                    .thenCompose(req -> interceptor.handle(context, req, v -> super.write(CommonUtils.<T>cast(v))))
                    .thenApply(CommonUtils::cast);
        }

    }


}
