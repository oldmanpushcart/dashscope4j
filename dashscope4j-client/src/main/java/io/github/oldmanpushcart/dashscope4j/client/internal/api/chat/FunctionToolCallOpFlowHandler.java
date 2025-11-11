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
        private final List<ToolCallMessage> tcMessages = new ArrayList<>();
        private Flow.Subscription upstream;
        private volatile boolean forwarded = false;

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

                final var request = (ChatRequest) response.request();
                final var incremental = request.parameters().has(ENABLE_INCREMENTAL_OUTPUT, true);
                final var choice = response.output().best();
                final var finish = choice.finish();
                final var message = choice.message();

                // 如果是普通消息，则直接下发
                if (!(message instanceof ToolCallMessage tcMessage)) {
                    downstream.onNext(response);
                    upstream.request(1);
                    return;
                }

                // 工具调用消息则缓存起来，准备进行合并后拼接，方便后续工具调用
                tcMessages.add(tcMessage);

                // 遇到最后一个工具调用消息，则进行合并，并发起工具调用
                if (finish == ChatResponse.Finish.TOOL_CALLS) {

                    // 合并为最终的TcMessage
                    final var finishTcMessage = tcMessages.stream()
                            .reduce((c1, c2) -> incremental ? c1.accumulate(c2) : c2)
                            .orElseThrow(() -> new IllegalStateException("Non tool call message collected!"));

                    /*
                     * 标记当前流已被转发到子流
                     * 当流被转发后，流的完成将由子流触发
                     */
                    forwarded = true;

                    /*
                     * 发起工具调用，产生子流
                     * 并将工具调用产生的流转发到下游接收方
                     */
                    new FunctionToolCaller(chatOp, request, finishTcMessage)
                            .flowCall()
                            .thenAccept(publisher -> {
                                final var forwarding = new ForwardingSubscriber(downstream);
                                publisher.subscribe(forwarding);
                            })
                            .exceptionally(ex -> {
                                downstream.onError(ex);
                                return null;
                            });

                } else {
                    upstream.request(1);
                }

            } catch (Throwable ex) {
                downstream.onError(ex);
            }

        }

        @Override
        public void onError(Throwable ex) {
            downstream.onError(ex);
        }

        @Override
        public void onComplete() {

            /*
             * 如果当期流已经被转到子流，则主流结束并不会触发下游玩完成。
             * 下游完成将会由子流处理
             */
            if (!forwarded) {
                downstream.onComplete();
            }

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
