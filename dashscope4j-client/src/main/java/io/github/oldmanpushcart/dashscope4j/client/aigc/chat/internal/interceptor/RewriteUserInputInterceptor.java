package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface RewriteUserInputInterceptor extends Interceptor {

    @Override
    default CompletionStage<?> intercept(Interceptor.Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> request)
                || !(request.model() instanceof ChatModel model)) {
            return chain.proceed();
        }

        return rewriteRequest(chain, request.as(model))
                .thenCompose(chain::proceed);

    }

    private CompletionStage<AigcRequest<Input, Output>> rewriteRequest(Interceptor.Chain chain, AigcRequest<Input, Output> request) {
        if(!request.input().hasUserInputMessage()) {
            return CompletableFuture.completedStage(request);
        }
        final var inputMessage = request.input().userInputMessage();
        return CompletableFuture.completedStage(null)
                .thenCompose(v -> rewriteUserInputMessage(chain, request, inputMessage))
                .thenApply(newInputMessage ->
                        AigcRequest.newBuilder(request)
                                .input(Input.newBuilder(request.input())
                                        .messages(request.input().historyMessages())
                                        .addMessage(newInputMessage)
                                        .build())
                                .build());
    }

    CompletionStage<Message> rewriteUserInputMessage(Interceptor.Chain chain, AigcRequest<Input, Output> request, UserMessage message);


}
