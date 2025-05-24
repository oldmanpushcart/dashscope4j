package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.ToolCallMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionTool;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import static java.util.Collections.synchronizedList;

@AllArgsConstructor
class FunctionToolCallOpFlowHandler implements UnaryOperator<Flowable<ChatResponse>> {

    private static final Flowable<ChatResponse> emptyFlow = Flowable.empty();
    private final DashscopeClient client;
    private final ChatOp chatOp;

    @Override
    public Flowable<ChatResponse> apply(Flowable<ChatResponse> flow) {

        // 工具调用消息集合
        final List<ToolCallMessage> toolCallMessages = synchronizedList(new ArrayList<>());

        // 对话流与工具调用结果流合并
        return flow.concatMap(response -> {

            final ChatResponse.Choice choice = response.output().best();
            final ChatResponse.Finish finish = choice.finish();
            final Message message = choice.message();

            /*
             * 判断当前消息是否是工具调用消息
             * - 如果不是，则说明是一个普通对话消息，应该纳入到对话流中
             * - 如果是，则需要添加到工具调用消息集合，以待后续合并
             */
            if (!(message instanceof ToolCallMessage toolCallMessage)) {
                return Flowable.just(response);
            }

            // 所有的工具调用消息都必须添加到集合中，后续合并需要
            toolCallMessages.add(toolCallMessage);

            /*
             * 如果当前消息已经是工具调用的最后一条消息，则发起工具调用。
             * 否则继续等待
             */
            return finish == ChatResponse.Finish.TOOL_CALLS
                    ? emptyFlow.concatWith(Flowable.defer(() -> callingTool(response, toolCallMessages)))
                    : emptyFlow;

        });
    }

    private Flowable<ChatResponse> callingTool(ChatResponse response, List<ToolCallMessage> toolCallMessages) {
        final ChatRequest request = (ChatRequest) response.request();
        final boolean isIncrementalOutput = request.option().has(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true);
        final ToolCallMessage mergeToolCallMessage = mergeToolCallMessage(isIncrementalOutput, toolCallMessages);
        final CompletionStage<Flowable<ChatResponse>> tcFlow
                = new FunctionToolCaller(client, chatOp, request, mergeToolCallMessage)
                .flowCall();
        return Flowable
                .fromCompletionStage(tcFlow)
                .flatMap(Flowable::fromPublisher);
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
                .filter(FunctionTool.Call.class::isInstance)
                .map(FunctionTool.Call.class::cast)
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
        public void reduce(boolean isIncrementalOutput, FunctionTool.Call call) {

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
            return new FunctionTool.Call(
                    index,
                    idBuf.toString(),
                    new FunctionTool.Call.Stub(
                            nameBuf.toString(),
                            argsBuf.toString()
                    ));
        }

    }

}
