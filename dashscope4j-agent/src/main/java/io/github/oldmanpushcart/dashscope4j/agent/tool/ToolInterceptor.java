package io.github.oldmanpushcart.dashscope4j.agent.tool;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ToolInterceptor implements ChatInterceptor {

    private final ToolRegistry registry;

    public ToolInterceptor(ToolRegistry registry) {
        this.registry = registry;
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        return rwRequest(request)
                .thenCompose(chain::proceed);
    }

    private CompletionStage<AigcRequest<Input, Output>> rwRequest(AigcRequest<Input, Output> request) {
        final var userInputMessage = request.input().userInputMessage();
        if(userInputMessage == null) {
            return CompletableFuture.completedStage(request);
        }
        return registry.routing(request.input().userInputMessage().text())
                .thenApply(tools -> AigcRequest.newBuilder(request)
                        .parameters(parameters -> {
                            parameters.put("tools", tools);
                            return parameters;
                        })
                        .build());
    }

}
