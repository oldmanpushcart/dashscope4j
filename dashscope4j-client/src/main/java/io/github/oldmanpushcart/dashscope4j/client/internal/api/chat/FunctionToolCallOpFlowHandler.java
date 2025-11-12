package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.ToolCallMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.UnaryOperator;

import static io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT;

/**
 * 流式工具调用处理器
 */
class FunctionToolCallOpFlowHandler implements UnaryOperator<Flow.Publisher<ChatResponse>> {

    private final ChatOp chatOp;


    FunctionToolCallOpFlowHandler(ChatOp chatOp) {
        this.chatOp = chatOp;
    }

    @Override
    public Flow.Publisher<ChatResponse> apply(Flow.Publisher<ChatResponse> source) {
        return downstream -> source.subscribe(new ToolCallSubscriber(downstream));
    }


    private class ToolCallSubscriber implements Flow.Subscriber<ChatResponse> {

        private final Flow.Subscriber<? super ChatResponse> downstream;
        private final List<ToolCallMessage> tcMessageSegments = new ArrayList<>();

        private Flow.Subscription upstream;
        private ChatRequest request;

        private ToolCallSubscriber(Flow.Subscriber<? super ChatResponse> downstream) {
            this.downstream = downstream;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.upstream = subscription;
            downstream.onSubscribe(new Flow.Subscription() {

                @Override
                public void request(long n) {
                    subscription.request(n);
                }

                @Override
                public void cancel() {
                    subscription.cancel();
                }

            });
        }

        @Override
        public void onNext(ChatResponse response) {
            try {

                /*
                 * 补充上ChatRequest
                 * 整个流中取第一个即可，整个流的都是同一个request
                 */
                if (null == request) {
                    request = (ChatRequest) response.request();
                }

                /*
                 * 如果有ToolCallMessage，则讲片段缓存起来
                 * 在onCompleted的时候再合并起来使用
                 */
                final var choice = response.output().best();
                final var message = choice.message();
                if (message instanceof ToolCallMessage tcMessage) {
                    tcMessageSegments.add(tcMessage);
                } else {
                    downstream.onNext(response);
                }

                upstream.request(1);

            } catch (Throwable ex) {
                downstream.onError(ex);
            }
        }

        @Override
        public void onError(Throwable ex) {
            downstream.onError(ex);
        }

        private ToolCallMessage parseToolCallMessage() {
            if (null == request) {
                return null;
            }
            final var incremental = request.parameters().has(ENABLE_INCREMENTAL_OUTPUT, true);
            return tcMessageSegments.stream()
                    .reduce((c1, c2) -> incremental ? c1.accumulate(c2) : c2)
                    .orElse(null);
        }

        @Override
        public void onComplete() {

            try {

                /*
                 * 发起工具调用，产生子流
                 * 并将工具调用产生的流转发到下游接收方
                 */
                final var tcMessage = parseToolCallMessage();
                if (null != tcMessage) {
                    new FunctionToolCaller(chatOp, request, tcMessage)
                            .flowCall()
                            .thenAccept(publisher -> {
                                final var forwarding = new ForwardingSubscriber(downstream);
                                publisher.subscribe(forwarding);
                            })
                            .exceptionally(ex -> {
                                downstream.onError(ex);
                                return null;
                            });
                    return;
                }

            } catch (Throwable ex) {
                onError(ex);
            }

            downstream.onComplete();
        }

    }


    /**
     * 简单的转发订阅者，用于将任意 Publisher<T> 的内容转发给 Subscriber<T>
     */
    private static class ForwardingSubscriber implements Flow.Subscriber<ChatResponse> {

        private final Flow.Subscriber<? super ChatResponse> target;
        private Flow.Subscription subscription;

        public ForwardingSubscriber(Flow.Subscriber<? super ChatResponse> target) {
            this.target = target;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        @Override
        public void onNext(ChatResponse item) {
            target.onNext(item);
            subscription.request(1);
        }

        @Override
        public void onError(Throwable ex) {
            target.onError(ex);
        }

        @Override
        public void onComplete() {
            target.onComplete();
        }

    }

}
