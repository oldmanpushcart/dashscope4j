package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.ToolCallMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
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
        return subscriber -> {
            final var output = new SubmissionPublisher<ChatResponse>();
            output.subscribe(subscriber);
            source.subscribe(new ToolCallSubscriber(output));
        };
    }


    private class ToolCallSubscriber implements Flow.Subscriber<ChatResponse> {

        private final SubmissionPublisher<ChatResponse> output;
        private final List<ToolCallMessage> tcMessageSegments = new ArrayList<>();
        private ChatRequest request;

        private ToolCallSubscriber(SubmissionPublisher<ChatResponse> output) {
            this.output = output;
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
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
                    output.submit(response);
                }

            } catch (Throwable ex) {
                onError(ex);
            }
        }

        @Override
        public void onError(Throwable ex) {
            output.closeExceptionally(ex);
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
                            .thenAccept(publisher ->
                                    publisher.subscribe(new Flow.Subscriber<>() {

                                        @Override
                                        public void onSubscribe(Flow.Subscription subscription) {
                                            subscription.request(Long.MAX_VALUE);
                                        }

                                        @Override
                                        public void onNext(ChatResponse item) {
                                            output.submit(item);
                                        }

                                        @Override
                                        public void onError(Throwable ex) {
                                            output.closeExceptionally(ex);
                                        }

                                        @Override
                                        public void onComplete() {
                                            output.close();
                                        }
                                        
                                    }))
                            .exceptionally(ex -> {
                                output.closeExceptionally(ex);
                                return null;
                            });
                } else {
                    output.close();
                }

            } catch (Throwable ex) {
                onError(ex);
            }

        }

    }

}
