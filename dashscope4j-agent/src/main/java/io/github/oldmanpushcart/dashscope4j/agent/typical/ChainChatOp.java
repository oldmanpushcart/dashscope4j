package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.chain.ChatChain;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

@Value
public class ChainChatOp implements ChatOp {

    ChatOp chatOp;
    ChatChain chain;

    @Override
    public CompletionStage<ChatResponse> async(ChatRequest chatRequest) {
        final ChatChain.Processor<ChatResponse> processor = new ProcessorImpl<>(chatRequest, chatOp::async);
        return chain.chainAsync(processor);
    }

    @Override
    public CompletionStage<Flowable<ChatResponse>> flow(ChatRequest chatRequest) {
        final ChatChain.Processor<Flowable<ChatResponse>> processor = new ProcessorImpl<>(chatRequest, chatOp::flow);
        return chain.chainFlow(processor);
    }

    public static ChatOp group(ChatOp chatOp, List<ChatChain> chains) {
        final List<ChatChain> clones = new ArrayList<>(chains);
        Collections.reverse(clones);
        ChatOp op = chatOp;
        for (final ChatChain chain : clones) {
            op = new ChainChatOp(op, chain);
        }
        return op;
    }

    @AllArgsConstructor
    @Accessors(fluent = true)
    private static class ProcessorImpl<R> implements ChatChain.Processor<R> {

        @Getter
        private final ChatRequest request;

        private final Function<ChatRequest, CompletionStage<R>> operator;

        @Override
        public CompletionStage<R> process(ChatRequest request) {
            return operator.apply(request);
        }

    }

}
