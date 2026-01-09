package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT;

public class NewToolCallFlowHandler implements UnaryOperator<Flow.Publisher<ChatResponse>> {

    private final ChatOp chatOp;

    NewToolCallFlowHandler(ChatOp chatOp) {
        this.chatOp = chatOp;
    }


    @Override
    public Flow.Publisher<ChatResponse> apply(Flow.Publisher<ChatResponse> publisher) {
        final var requestRef = new AtomicReference<ChatRequest>();
        final var segments = new ArrayList<AssistantMessage>();
        return FlowX.fromPublisher(publisher)
                .filter(response -> !response.output().choices().isEmpty())
                .filter(response -> {

                    /*
                     * 补充上ChatRequest
                     * 整个流中取第一个即可，整个流的都是同一个request
                     */
                    if (requestRef.get() == null) {
                        requestRef.set(response.request());
                    }

                    final var choice = response.output().best();
                    final var message = choice.message();

                    /*
                     * 如果有ToolCall，则讲片段缓存起来
                     * 在onCompleted的时候再合并起来使用
                     */
                    if (message.isToolCall()) {
                        segments.add(message);
                        return false;
                    } else {
                        return true;
                    }

                })
                .concat(FlowX
                        .defer(() -> {
                            final var request = requestRef.get();
                            final var tcMessage = mergeSegments(request, segments);
                            if (null == tcMessage) {
                                return FlowX
                                        .<ChatResponse>empty()
                                        .publisher();
                            }
                            return FlowX
                                    .fromCompletionStage(() -> new FunctionToolCaller(chatOp, request, tcMessage).flowCall())
                                    .publisher();
                        })
                        .publisher()
                )
                .publisher();
    }

    private AssistantMessage mergeSegments(ChatRequest request, List<AssistantMessage> segments) {
        if (null == request) {
            return null;
        }
        final var incremental = request.parameters().has(ENABLE_INCREMENTAL_OUTPUT, true);
        return segments.stream()
                .reduce((c1, c2) -> incremental ? c1.accumulate(c2) : c2)
                .orElse(null);
    }

}
