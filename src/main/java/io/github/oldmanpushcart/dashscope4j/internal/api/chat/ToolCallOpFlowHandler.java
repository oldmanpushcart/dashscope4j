package io.github.oldmanpushcart.dashscope4j.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.ToolCallMessage;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

@AllArgsConstructor
class ToolCallOpFlowHandler implements UnaryOperator<Flowable<ChatResponse>> {

    private final DashscopeClient client;
    private final ChatOp chatOp;

    @Override
    public Flowable<ChatResponse> apply(Flowable<ChatResponse> flow) {

        // 工具调用消息片段集合
        final List<ToolCallMessage> toolCallMessages = new ArrayList<>();

        // 对话流与函数调用流合并
        return flow.concatMap(response -> {

            final ChatResponse.Choice choice = response.output().best();
            final ChatResponse.Finish finish = choice.finish();
            final Message message = choice.message();

            // 将工具调用消息片段加入集合中，以便后续合并使用
            if (message instanceof ToolCallMessage) {
                toolCallMessages.add((ToolCallMessage) message);
            }

            // 如果不是最后一个消息，则直接返回当前对话流
            if (finish != ChatResponse.Finish.TOOL_CALLS) {
                return Flowable.just(response);
            }

            /*
             * 如果遇到FINISH == TOOL_CALLS，则说明遇到了函数调用最后一个片段
             * 最后一个片段到来，则可以发起工具调用请求，
             * 并将函数调用流合并到当前流
             */
            return Flowable
                    .just(response)
                    .concatWith(Flowable.defer(() -> {
                        final ChatRequest request = (ChatRequest) response.request();
                        final boolean isIncrementalOutput = request.option().has(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true);
                        final ToolCallMessage toolCallMessage = mergeToolCallMessage(isIncrementalOutput, toolCallMessages);
                        final CompletionStage<Flowable<ChatResponse>> tcFlow
                                = new ToolCaller(client, chatOp, request, toolCallMessage)
                                .flowCall();
                        return Flowable
                                .fromCompletionStage(tcFlow)
                                .flatMap(Flowable::fromPublisher);
                    }));

        });
    }


    /**
     * 合并工具调用消息
     *
     * @param isIncrementalOutput 是否为增量输出
     * @param toolCallMessages    工具调用消息集合
     * @return 工具调用消息
     */
    private ToolCallMessage mergeToolCallMessage(boolean isIncrementalOutput, List<ToolCallMessage> toolCallMessages) {
        final Map<Integer, FunctionToolCallBuilder> builderMap = new HashMap<>();
        toolCallMessages.stream()
                .flatMap(message -> message.calls().stream())
                .filter(ChatFunctionTool.Call.class::isInstance)
                .map(ChatFunctionTool.Call.class::cast)
                .forEach(call -> builderMap
                        .computeIfAbsent(call.index(), FunctionToolCallBuilder::new)
                        .reduce(isIncrementalOutput, call));

        final List<Tool.Call> calls = builderMap.values()
                .stream()
                .map(FunctionToolCallBuilder::build)
                .collect(Collectors.toList());

        return new ToolCallMessage("", calls);
    }

    /**
     * 函数调用构建器
     */
    @AllArgsConstructor
    private static class FunctionToolCallBuilder {

        private final int index;
        private final StringBuilder idBuf = new StringBuilder();
        private final StringBuilder nameBuf = new StringBuilder();
        private final StringBuilder argsBuf = new StringBuilder();

        /**
         * 规约函数调用
         *
         * @param isIncrementalOutput 是否为增量输出
         * @param call                函数调用
         */
        public void reduce(boolean isIncrementalOutput, ChatFunctionTool.Call call) {

            /*
             * 如果是非增量输出，则每次Call中携带的都是最新全量消息
             * 这里需要直接对原有内容缓存进行清空
             */
            if (!isIncrementalOutput) {
                idBuf.setLength(0);
                nameBuf.setLength(0);
                argsBuf.setLength(0);
            }

            // 合并ID
            if (null != call.id()) {
                idBuf.append(call.id());
            }

            // 合并NAME
            if (null != call.stub().name()) {
                nameBuf.append(call.stub().name());
            }

            // 合并ARGUMENTS
            if (null != call.stub().arguments()) {
                argsBuf.append(call.stub().arguments());
            }

        }

        /**
         * 构建函数调用
         *
         * @return 函数调用
         */
        public Tool.Call build() {
            return new ChatFunctionTool.Call(
                    index,
                    idBuf.toString(),
                    new ChatFunctionTool.Call.Stub(
                            nameBuf.toString(),
                            argsBuf.toString()
                    ));
        }

    }

}
