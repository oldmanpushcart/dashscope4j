package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.ToolCallMessage;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

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
        final ToolCallMessage toolCallMessage = toolCallMessages.stream()
                .reduce((c1,c2)  -> isIncrementalOutput ? c1.accumulate(c2) : c2)
                .orElseThrow();
        final CompletionStage<Flowable<ChatResponse>> tcFlow
                = new FunctionToolCaller(client, chatOp, request, toolCallMessage)
                .flowCall();
        return Flowable
                .fromCompletionStage(tcFlow)
                .flatMap(Flowable::fromPublisher);
    }

}
