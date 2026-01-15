package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.compat.plaintext.PlaintextChatHelper;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class CompatPlaintextInterceptor implements FlowInterceptor, AsyncInterceptor {

    @Override
    public CompletionStage<?> intercept(AsyncInterceptor.Chain chain) {

        if (!(chain.request() instanceof ChatRequest chatRequest)) {
            return chain.proceed();
        }

        final var model = chatRequest.model();
        if (!model.tags().contains(ChatModelTags.COMPAT_PLAINTEXT)) {
            return chain.proceed();
        }

        return CompletableFuture.completedStage(chatRequest)
                .thenApply(PlaintextChatHelper::toPlaintextChatRequest)
                .thenCompose(chain::proceed);
    }

    @Override
    public CompletionStage<? extends Flow.Publisher<?>> intercept(FlowInterceptor.Chain chain) {

        if (!(chain.request() instanceof ChatRequest chatRequest)) {
            return chain.proceed();
        }

        final var model = chatRequest.model();
        if (!model.tags().contains(ChatModelTags.COMPAT_PLAINTEXT)) {
            return chain.proceed();
        }

        return CompletableFuture.completedStage(chatRequest)
                .thenApply(PlaintextChatHelper::toPlaintextChatRequest)
                .thenCompose(chain::proceed);
    }

}

