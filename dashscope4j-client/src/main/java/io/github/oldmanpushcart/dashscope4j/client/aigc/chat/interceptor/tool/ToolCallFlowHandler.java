package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.interceptor.tool;

import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcOp;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT;

/**
 * 流式工具调用处理器
 */
class ToolCallFlowHandler implements UnaryOperator<Flow.Publisher<AigcResponse<Output>>> {

    private final AigcOp aigcOp;

    ToolCallFlowHandler(AigcOp aigcOp) {
        this.aigcOp = aigcOp;
    }


    @Override
    public Flow.Publisher<AigcResponse<Output>> apply(Flow.Publisher<AigcResponse<Output>> publisher) {
        final var requestRef = new AtomicReference<AigcRequest<Input, Output>>();
        final var segments = new ArrayList<AssistantMessage>();
        return FlowX.fromPublisher(publisher)

//                /*
//                 * 过滤掉返回内容为空的响应
//                 * 在兼容 openai 协议的场景中，会返回一个空的响应，导致后续的流中断
//                 */
//                .filter(response -> !response.output().choices().isEmpty())

                /*
                 * 完成 ToolCall 消息的收集，并将在后续进行处理，
                 * 合并成为一个可用的 ToolCall
                 */
                .filter(response -> {

                    /*
                     * 补充上ChatRequest
                     * 整个流中取第一个即可，整个流的都是同一个request
                     */
                    if (requestRef.get() == null) {
                        //noinspection unchecked
                        requestRef.set((AigcRequest<Input, Output>) response.request());
                    }

                    if (response.output().choices().isEmpty()) {
                        return true;
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

                /*
                 * 对合并的 ToolCall 进行处理，正式发起函数调用
                 */
                .concat(FlowX.defer(() -> {
                    final var request = requestRef.get();
                    final var tcMessage = mergeSegments(request, segments);
                    if (null == tcMessage) {
                        return FlowX.empty();
                    }
                    return new FunctionToolCaller(aigcOp, request, tcMessage).flowCall();
                }));
    }

    private AssistantMessage mergeSegments(AigcRequest<Input, Output> request, List<AssistantMessage> segments) {
        if (null == request) {
            return null;
        }
        final var incremental = request.parameters().has(ENABLE_INCREMENTAL_OUTPUT, true);
        return segments.stream()
                .reduce((c1, c2) -> incremental ? c1.accumulate(c2) : c2)
                .orElse(null);
    }

}
