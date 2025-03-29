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

        final List<ToolCallMessage> toolCallMessages = new ArrayList<>();

        return flow.concatMap(response -> {

            final ChatResponse.Choice choice = response.output().best();
            final ChatResponse.Finish finish = choice.finish();
            final Message message = choice.message();

            if (message instanceof ToolCallMessage) {
                toolCallMessages.add((ToolCallMessage) message);
            }

            if (finish != ChatResponse.Finish.TOOL_CALLS) {
                return Flowable.just(response);
            }

            return Flowable
                    .just(response)
                    .concatWith(Flowable.defer(() -> {
                        final ChatRequest request = (ChatRequest) response.request();
                        final ToolCallMessage toolCallMessage = newTollCallMessage(request, toolCallMessages);
                        final CompletionStage<Flowable<ChatResponse>> tcFlow
                                = new ToolCaller(client, chatOp, request, toolCallMessage)
                                .flowCall();
                        return Flowable
                                .fromCompletionStage(tcFlow)
                                .flatMap(Flowable::fromPublisher);
                    }));

        });
    }

    private ToolCallMessage newTollCallMessage(ChatRequest request, List<ToolCallMessage> toolCallMessages) {
        final boolean isIncrementalOutput = request.option().has(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true);
        final Map<String, FunctionToolCallBuilder> functionToolCallBuilderMap = new HashMap<>();
        toolCallMessages.stream()
                .flatMap(message -> message.calls().stream())
                .filter(call -> call instanceof ChatFunctionTool.Call)
                .map(ChatFunctionTool.Call.class::cast)
                .forEach(call -> {

                    final FunctionToolCallBuilder builder;
                    if (!functionToolCallBuilderMap.containsKey(call.id())) {
                        builder = new FunctionToolCallBuilder(call.id());
                        functionToolCallBuilderMap.put(call.id(), builder);
                    } else {
                        builder = functionToolCallBuilderMap.get(call.id());
                    }

                    if (!isIncrementalOutput) {
                        builder.nameBuilder.setLength(0);
                        builder.argsBuilder.setLength(0);
                    }
                    if (null != call.stub().name()) {
                        builder.nameBuilder.append(call.stub().name());
                    }
                    if (null != call.stub().arguments()) {
                        builder.argsBuilder.append(call.stub().arguments());
                    }

                });

        final List<Tool.Call> calls = functionToolCallBuilderMap.values()
                .stream()
                .map(FunctionToolCallBuilder::toToolCall)
                .collect(Collectors.toList());

        return new ToolCallMessage("", calls);
    }

    @AllArgsConstructor
    private static class FunctionToolCallBuilder {
        private final String id;
        private final StringBuilder nameBuilder = new StringBuilder();
        private final StringBuilder argsBuilder = new StringBuilder();

        public Tool.Call toToolCall() {
            return new ChatFunctionTool.Call(
                    id,
                    new ChatFunctionTool.Call.Stub(
                            nameBuilder.toString(),
                            argsBuilder.toString()
                    ));
        }

    }

}
