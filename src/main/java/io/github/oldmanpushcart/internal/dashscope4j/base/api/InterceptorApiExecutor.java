package io.github.oldmanpushcart.internal.dashscope4j.base.api;

import io.github.oldmanpushcart.dashscope4j.base.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.base.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.base.interceptor.InvocationContext;
import io.github.oldmanpushcart.dashscope4j.base.task.Task;
import io.github.oldmanpushcart.internal.dashscope4j.base.interceptor.InterceptorHelper;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

import static io.github.oldmanpushcart.internal.dashscope4j.util.CompletableFutureUtils.handleCompose;

public class InterceptorApiExecutor extends ApiExecutor {

    private final InterceptorHelper interceptorHelper;

    /**
     * 构造API执行器
     */
    public InterceptorApiExecutor(final String ak,
                                  final HttpClient http,
                                  final Executor executor,
                                  final Duration timeout,
                                  final InterceptorHelper interceptorHelper) {
        super(ak, http, executor, timeout);
        this.interceptorHelper = interceptorHelper;
    }

    @Override
    public <R extends ApiResponse<?>> CompletableFuture<R> async(ApiRequest<R> request) {
        final var context = interceptorHelper.newInvocationContext();
        final var future = interceptorHelper.preHandle(context, request).thenCompose(super::async);
        return handleCompose(future, (r, ex) -> interceptorHelper.postHandle(context, r, ex));
    }

    @Override
    public <R extends ApiResponse<?>> CompletableFuture<Flow.Publisher<R>> flow(ApiRequest<R> request) {
        final var context = interceptorHelper.newInvocationContext();
        return interceptorHelper.preHandle(context, request)
                .thenCompose(super::flow)
                .thenApply(publisher -> {
                    final var processor = new PostHandleProcessor<R>(context);
                    publisher.subscribe(processor);
                    return processor;
                });
    }

    @Override
    public <R extends ApiResponse<?>> CompletableFuture<Task.Half<R>> task(ApiRequest<R> request) {
        final var context = interceptorHelper.newInvocationContext();
        return interceptorHelper.preHandle(context, request)
                .thenCompose(super::task)
                .thenApply(half -> strategy -> handleCompose(half.waitingFor(strategy), (r, ex) -> interceptorHelper.postHandle(context, r, ex)));
    }

    private class PostHandleProcessor<R extends ApiResponse<?>> implements Flow.Processor<R, R> {

        private final InvocationContext context;
        private final AtomicReference<Flow.Subscriber<? super R>> downstreamRef = new AtomicReference<>();
        private final AtomicReference<Flow.Subscription> upstreamRef = new AtomicReference<>();

        private PostHandleProcessor(InvocationContext context) {
            this.context = context;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super R> subscriber) {
            if (!downstreamRef.compareAndSet(null, subscriber)) {
                throw new IllegalStateException("already onSubscribed!");
            }
            subscriber.onSubscribe(new Flow.Subscription() {

                @Override
                public void request(long n) {
                    upstreamRef.get().request(n);
                }

                @Override
                public void cancel() {
                    upstreamRef.get().cancel();
                }

            });
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (!upstreamRef.compareAndSet(null, subscription)) {
                subscription.cancel();
                throw new IllegalStateException("already subscribed!");
            }
        }

        @Override
        public void onNext(R response) {
            interceptorHelper.postHandle(context, response, null)
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            downstreamRef.get().onError(ex);
                        } else {
                            downstreamRef.get().onNext(r);
                        }
                    });
        }

        @Override
        public void onError(Throwable throwable) {
            interceptorHelper.<R>postHandle(context, null, throwable)
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            downstreamRef.get().onError(ex);
                        } else {
                            downstreamRef.get().onNext(r);
                        }
                    });
        }

        @Override
        public void onComplete() {
            downstreamRef.get().onComplete();
        }

    }

}
