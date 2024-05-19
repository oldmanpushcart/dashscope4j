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

    private <T extends ApiRequest<?>> CompletableFuture<T> preHandle(InvocationContext context, T request) {
        return interceptorHelper.preHandle(context, request);
    }

    private <T extends ApiResponse<?>> CompletableFuture<T> postHandle(InvocationContext context, T response) {
        return interceptorHelper.postHandle(context, response);
    }

    @Override
    public <R extends ApiResponse<?>> CompletableFuture<R> async(ApiRequest<R> request) {
        final var context = interceptorHelper.newInvocationContext();
        return preHandle(context, request)
                .thenCompose(super::async)
                .thenCompose(response -> postHandle(context, response));
    }

    @Override
    public <R extends ApiResponse<?>> CompletableFuture<Flow.Publisher<R>> flow(ApiRequest<R> request) {
        final var context = interceptorHelper.newInvocationContext();
        return preHandle(context, request)
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
        return preHandle(context, request)
                .thenCompose(super::task)
                .thenApply(half -> strategy -> half.waitingFor(strategy)
                        .thenCompose(response -> postHandle(context, response))
                );
    }

    private class PostHandleProcessor<R extends ApiResponse<?>> implements Flow.Processor<R, R> {

        private final InvocationContext context;
        private final AtomicReference<Flow.Subscriber<? super R>> subscriberRef = new AtomicReference<>();
        private final AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();

        private PostHandleProcessor(InvocationContext context) {
            this.context = context;
        }

        @Override
        public void subscribe(Flow.Subscriber<? super R> subscriber) {
            if (!subscriberRef.compareAndSet(null, subscriber)) {
                throw new IllegalStateException("processor publisher already subscribed");
            }
            subscriber.onSubscribe(new Flow.Subscription() {

                @Override
                public void request(long n) {
                    subscriptionRef.get().request(n);
                }

                @Override
                public void cancel() {
                    subscriptionRef.get().cancel();
                }

            });
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (!subscriptionRef.compareAndSet(null, subscription)) {
                subscription.cancel();
                throw new IllegalStateException("already subscribed");
            }
        }

        @Override
        public void onNext(R response) {
            interceptorHelper.postHandle(context, response)
                    .whenComplete((r, ex) -> {
                        if (ex != null) {
                            onError(ex);
                        } else {
                            subscriberRef.get().onNext(r);
                        }
                    });
        }

        @Override
        public void onError(Throwable throwable) {
            subscriberRef.get().onError(throwable);
        }

        @Override
        public void onComplete() {
            subscriberRef.get().onComplete();
        }

    }

}
