package io.github.oldmanpushcart.dashscope4j.client.internal.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.Interceptor;

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
