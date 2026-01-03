package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.SystemMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.UserMessage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface RewriteUserInputInterceptor extends ChatInterceptor {

    default CompletionStage<?> intercept(Chain chain, ChatRequest request) {

        if (!request.hasUserInputMessage()) {
            return chain.proceed();
        }

        final var inputMessage = request.userInputMessage();
        return CompletableFuture.completedStage(null)
                .thenCompose(v -> rewrite(chain, inputMessage))
                .thenCompose(newInputMessage -> {
                    final var newRequest = ChatRequest.newBuilder(request)
                            .messages(request.historyMessages())
                            .addMessage(newInputMessage)
                            .build();
                    return chain.proceed(newRequest);
                });

    }

    CompletionStage<Message> rewrite(Chain chain, UserMessage message);

}
