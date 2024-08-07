package io.github.oldmanpushcart.internal.dashscope4j.base.api;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.internal.dashscope4j.util.CommonUtils;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;

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
    public <R extends ApiResponse<?>> CompletableFuture<R> async(ApiRequest<R> request) {
        final var context = new CtxImpl(client, executor);
        return CompletableFuture.completedFuture(null)

                // pre-handle
                .thenCompose(unused -> interceptor.preHandle(context, request))
                .thenApply(CommonUtils::<ApiRequest<R>>cast)

                // handle
                .thenCompose(req -> interceptor.handle(context, req, target::async))
                .thenApply(CommonUtils::<R>cast)

                // post-handle
                .handle((resp, ex) -> interceptor.postHandle(context, resp, ex))
                .thenCompose(v -> v)
                .thenApply(CommonUtils::cast);

    }

    @Override
    public <R extends ApiResponse<?>> CompletableFuture<Flow.Publisher<R>> flow(ApiRequest<R> request) {
        final var context = new CtxImpl(client, executor);
        return CompletableFuture.completedFuture(null)

                // pre-handle
                .thenCompose(unused -> interceptor.preHandle(context, request))
                .thenApply(CommonUtils::<ApiRequest<R>>cast)

                // handle
                .thenCompose(req -> interceptor.handle(context, req, target::flow))
                .thenApply(CommonUtils::<Flow.Publisher<R>>cast)

                // post-handle
                .thenApply(p -> asyncOneToOne(p, (r, ex) -> interceptor.postHandle(context, r, ex).thenApply(CommonUtils::<R>cast)));

    }

    @Override
    public <R extends ApiResponse<?>> CompletableFuture<Task.Half<R>> task(ApiRequest<R> request) {
        final var context = new CtxImpl(client, executor);
        return CompletableFuture.completedFuture(null)

                // pre-handle
                .thenCompose(unused -> interceptor.preHandle(context, request))
                .thenApply(CommonUtils::<ApiRequest<R>>cast)

                // handle
                .thenCompose(req -> interceptor.handle(context, req, target::task))
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

}
