package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.function.UnaryOperator;

import static io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT;

/**
 * 流式工具调用处理器
 */
class ToolCallFlowHandler implements UnaryOperator<Flow.Publisher<ChatResponse>> {

    private final ChatOp chatOp;

    ToolCallFlowHandler(ChatOp chatOp) {
        this.chatOp = chatOp;
    }

    @Override
    public Flow.Publisher<ChatResponse> apply(Flow.Publisher<ChatResponse> publisher) {
        return new FlatMapPublisher<>(publisher, new ToolCallFlatMapper(chatOp));
    }

    private static class ToolCallFlatMapper implements FlatMapPublisher.FlatMapper<ChatResponse, ChatResponse> {

        private final ChatOp chatOp;
        private final List<AssistantMessage> tcMessageSegments = new ArrayList<>();
        private volatile ChatRequest request;

        private ToolCallFlatMapper(ChatOp chatOp) {
            this.chatOp = chatOp;
        }

        @Override
        public Flow.Publisher<ChatResponse> map(ChatResponse response) {

            /*
             * 补充上ChatRequest
             * 整个流中取第一个即可，整个流的都是同一个request
             */
            if (request == null) {
                request = response.request();
            }

            // TODO : 需要修复choice可能为空的问题
            if(response.output().choices().isEmpty()) {
                return CollectionPublisher.of(response);
            }

            final var choice = response.output().best();
            final var message = choice.message();

            /*
             * 如果有ToolCall，则讲片段缓存起来
             * 在onCompleted的时候再合并起来使用
             */
            if (message.isToolCall()) {
                tcMessageSegments.add(message);
                return EmptyPublisher.empty();
            } else {
                return CollectionPublisher.of(response);
            }

        }

        private AssistantMessage mergeSegments() {
            if (null == request) {
                return null;
            }
            final var incremental = request.parameters().has(ENABLE_INCREMENTAL_OUTPUT, true);
            return tcMessageSegments.stream()
                    .reduce((c1, c2) -> incremental ? c1.accumulate(c2) : c2)
                    .orElse(null);
        }

        @Override
        public Flow.Publisher<ChatResponse> finish() {

            try {

                /*
                 * 发起工具调用，产生子流
                 * 并将工具调用产生的流转发到下游接收方
                 */
                final var tcMessage = mergeSegments();

                /*
                 * 如果没有找到工具调用消息，说明本次流中不需要进行工具调用处理。
                 * 这种情况下直接关闭输出流即可
                 */
                if (null == tcMessage) {
                    return EmptyPublisher.empty();
                }

                /*
                 * 找到了工具调用消息，则向LLM发起工具调用
                 * 输出流将交由工具调用处理程序处理
                 */
                return new DeferredPublisher<>(() -> new FunctionToolCaller(chatOp, request, tcMessage).flowCall());

            } catch (Throwable ex) {
                return ErrorPublisher.of(ex);
            }

        }

    }

}
