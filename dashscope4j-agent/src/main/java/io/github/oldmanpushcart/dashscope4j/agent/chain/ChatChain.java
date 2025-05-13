package io.github.oldmanpushcart.dashscope4j.agent.chain;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

public interface ChatChain {

    CompletionStage<ChatResponse> chainAsync(Processor<ChatResponse> processor);

    CompletionStage<Flowable<ChatResponse>> chainFlow(Processor<Flowable<ChatResponse>> processor);

    interface Processor<R> {

        ChatRequest request();

        CompletionStage<R> process(ChatRequest request);

    }

}
