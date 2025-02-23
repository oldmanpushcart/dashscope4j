package io.github.oldmanpushcart.dashscope4j.internal.api.chat;

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
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

@AllArgsConstructor
class ToolCallOpFlowHandler implements UnaryOperator<Flowable<ChatResponse>> {

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
                                = new ToolCaller(chatOp, request, toolCallMessage)
                                .flowCall();
                        return Flowable
                                .fromCompletionStage(tcFlow)
                                .flatMap(Flowable::fromPublisher);
                    }));

        });
    }

    private ToolCallMessage newTollCallMessage(ChatRequest request, List<ToolCallMessage> toolCallMessages) {
        final StringBuilder nameBuilder = new StringBuilder();
        final StringBuilder argumentsBuilder = new StringBuilder();
        final boolean isIncrementalOutput = request.option().has(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true);
        toolCallMessages.forEach(message -> {
            message.calls().stream()
                    .filter(call -> call instanceof ChatFunctionTool.Call)
                    .map(ChatFunctionTool.Call.class::cast)
                    .forEach(call -> {
                        if (!isIncrementalOutput) {
                            nameBuilder.setLength(0);
                            argumentsBuilder.setLength(0);
                        }
                        if(null != call.stub().name()) {
                            nameBuilder.append(call.stub().name());
                        }
                        if(null != call.stub().arguments()) {
                            argumentsBuilder.append(call.stub().arguments());
                        }
                    });
        });

        final List<Tool.Call> calls = new ArrayList<>();
        calls.add(new ChatFunctionTool.Call(new ChatFunctionTool.Call.Stub(
                nameBuilder.toString(),
                argumentsBuilder.toString()
        )));

        return new ToolCallMessage("", calls);
    }

}
