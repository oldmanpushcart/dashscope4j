package io.github.oldmanpushcart.internal.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.ToolCallMessageImpl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BinaryOperator;
import java.util.function.Function;

public class ChatResponseOpFlowHandler implements Function<Flow.Publisher<ChatResponse>, Flow.Publisher<ChatResponse>> {

    private final DashScopeClient client;
    private final ChatRequest request;

    public ChatResponseOpFlowHandler(DashScopeClient client, ChatRequest request) {
        this.client = client;
        this.request = request;
    }

    @Override
    public Flow.Publisher<ChatResponse> apply(Flow.Publisher<ChatResponse> source) {
        return concat(source, (a, b) -> b, response -> {

            // 只能处理自己内部实现的对话请求
            if (request instanceof ChatRequestImpl requestImpl) {

                // 处理工具调用场景
                final var choice = response.output().best();
                if (null != choice
                    && choice.finish() == ChatResponse.Finish.TOOL_CALLS
                    && choice.message() instanceof ToolCallMessageImpl messageImpl) {
                    return new OpToolCall(requestImpl, messageImpl)
                            .op(client)
                            .thenCompose(DashScopeClient.OpFlow::flow);
                }

            }

            return CompletableFuture.completedFuture(null);

        });
    }


    private static <T> Flow.Publisher<T> concat(Flow.Publisher<T> source, BinaryOperator<T> accumulator, Function<T, CompletableFuture<Flow.Publisher<T>>> finisher) {

        final var processor = new Flow.Processor<T, T>() {

            private final AtomicReference<Flow.Subscription> upstreamRef = new AtomicReference<>();
            private final AtomicReference<Flow.Subscriber<? super T>> downstreamRef = new AtomicReference<>();
            private final AtomicReference<T> resultRef = new AtomicReference<>();
            private final AtomicLong limitRef = new AtomicLong(0);
            private volatile boolean isCompleted = false;
            private volatile boolean isCancelled = false;

            private boolean isFinished() {
                return isCompleted && isCancelled;
            }

            @Override
            public void onSubscribe(Flow.Subscription upstream) {
                upstreamRef.set(upstream);
                final var limit = limitRef.get();
                if(limit > 0) {
                    upstream.request(limit);
                }
            }

            @Override
            public void onNext(T item) {
                try {
                    downstreamRef.get().onNext(item);
                    resultRef.accumulateAndGet(item, accumulator);
                    limitRef.decrementAndGet();
                } catch (Throwable t) {
                    onError(t);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (isFinished()) {
                    return;
                }
                isCompleted = true;
                upstreamRef.get().cancel();
                downstreamRef.get().onError(t);
            }

            @Override
            public void onComplete() {
                if (isFinished()) {
                    return;
                }
                finisher.apply(resultRef.get()).whenComplete((r, t) -> {

                    if (null != t) {
                        onError(t);
                        return;
                    }

                    if (null != r) {
                        r.subscribe(this);
                    } else {
                        isCompleted = true;
                        downstreamRef.get().onComplete();
                    }

                });

            }

            @Override
            public void subscribe(Flow.Subscriber<? super T> downstream) {
                if (!downstreamRef.compareAndSet(null, downstream)) {
                    throw new IllegalStateException("already subscribed!");
                }
                downstream.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                        upstreamRef.get().request(n);
                        limitRef.addAndGet(n);
                    }

                    @Override
                    public void cancel() {
                        isCancelled = true;
                        upstreamRef.get().cancel();
                    }
                });
            }

        };

        source.subscribe(processor);
        return processor;

    }

}
