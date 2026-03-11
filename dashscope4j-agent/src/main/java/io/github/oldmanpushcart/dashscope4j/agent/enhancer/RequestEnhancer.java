package io.github.oldmanpushcart.dashscope4j.agent.enhancer;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class RequestEnhancer implements Enhancer {

    private final Function<AigcRequest<Input, Output>, CompletionStage<AigcRequest<Input, Output>>> operator;

    public RequestEnhancer(Function<AigcRequest<Input, Output>, CompletionStage<AigcRequest<Input, Output>>> operator) {
        this.operator = operator;
    }

    @Override
    public CompletionStage<Agent> enhance(Agent agent) {
        return Agent.newBuilder(agent)
                .interceptors(interceptors -> {
                    interceptors.add(0, (ChatInterceptor) (chain, request) ->
                            CompletableFuture.completedStage(request)
                                    .thenCompose(operator)
                                    .thenCompose(chain::proceed));
                    return interceptors;
                })
                .buildAsync();
    }

}
