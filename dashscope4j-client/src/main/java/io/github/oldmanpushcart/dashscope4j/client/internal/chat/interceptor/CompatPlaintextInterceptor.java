package io.github.oldmanpushcart.dashscope4j.client.internal.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.chat.compat.plaintext.PlaintextChatHelper;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.TagUtils;

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
        if (!TagUtils.contains(model.tags(), "compat","plaintext")) {
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
        if (!TagUtils.contains(model.tags(), "compat","plaintext")) {
            return chain.proceed();
        }

        return CompletableFuture.completedStage(chatRequest)
                .thenApply(PlaintextChatHelper::toPlaintextChatRequest)
                .thenCompose(chain::proceed);
    }

}

