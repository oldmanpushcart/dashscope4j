package io.github.oldmanpushcart.internal.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.OpChat;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.ToolCallMessage;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;

import java.util.concurrent.CompletionStage;
import java.util.function.UnaryOperator;

@AllArgsConstructor
public class ToolCallOpFlowHandler implements UnaryOperator<Flowable<ChatResponse>> {

    private final OpChat opChat;
    private final ChatRequest request;

    @Override
    public Flowable<ChatResponse> apply(Flowable<ChatResponse> flow) {
        return flow.concatMap(response -> {

            final ChatResponse.Choice choice = response.output().best();

            if (!isRequired(choice)) {
                return Flowable.just(response);
            }

            return Flowable
                    .just(response)
                    .concatWith(Flowable.defer(() -> {
                        final ToolCallMessage message = (ToolCallMessage) choice.message();
                        final CompletionStage<Flowable<ChatResponse>> tcFlow
                                = new ToolCaller(opChat, request, message)
                                .flowCall();
                        return Flowable
                                .fromCompletionStage(tcFlow)
                                .flatMap(Flowable::fromPublisher);
                    }));

        });
    }

    private boolean isRequired(ChatResponse.Choice choice) {
        return null != choice
               && choice.finish() == ChatResponse.Finish.TOOL_CALLS
               && choice.message() instanceof ToolCallMessage;
    }

}
