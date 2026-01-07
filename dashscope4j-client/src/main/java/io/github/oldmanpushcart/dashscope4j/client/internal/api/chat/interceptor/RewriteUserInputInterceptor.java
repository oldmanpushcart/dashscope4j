package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.Interceptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public interface RewriteUserInputInterceptor extends AsyncInterceptor, FlowInterceptor {

    default CompletionStage<?> intercept(AsyncInterceptor.Chain chain) {
        if (!(chain.request() instanceof ChatRequest request)
                || !request.hasUserInputMessage()) {
            return chain.proceed();
        }

        return CompletableFuture.completedStage(null)
                .thenCompose(unused -> rewriteChatRequest(chain, request))
                .thenCompose(chain::proceed);
    }

    default CompletionStage<? extends Flow.Publisher<?>> intercept(FlowInterceptor.Chain chain) {
        if (!(chain.request() instanceof ChatRequest request)
                || !request.hasUserInputMessage()) {
            return chain.proceed();
        }

        return CompletableFuture.completedStage(null)
                .thenCompose(unused -> rewriteChatRequest(chain, request))
                .thenCompose(chain::proceed);
    }

    private CompletionStage<ChatRequest> rewriteChatRequest(Interceptor.Chain chain, ChatRequest request) {
        final var inputMessage = request.userInputMessage();
        return CompletableFuture.completedStage(null)
                .thenCompose(v -> rewriteUserInputMessage(chain, inputMessage))
                .thenApply(newInputMessage ->
                        ChatRequest.newBuilder(request)
                                .messages(request.historyMessages())
                                .addMessage(newInputMessage)
                                .build());
    }

    CompletionStage<Message> rewriteUserInputMessage(Interceptor.Chain chain, UserMessage message);

}
