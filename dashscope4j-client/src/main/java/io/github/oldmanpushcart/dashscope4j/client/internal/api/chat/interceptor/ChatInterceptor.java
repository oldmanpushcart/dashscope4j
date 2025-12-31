package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.Interceptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public interface ChatInterceptor extends Interceptor {

    @Override
    default CompletionStage<?> intercept(Chain chain) {
        if (chain.request() instanceof ChatRequest chatRequest) {
            try {
                return intercept(chain, chatRequest);
            } catch (Throwable ex) {
                return CompletableFuture.failedStage(ex);
            }
        } else {
            return chain.proceed();
        }
    }

    CompletionStage<?> intercept(Chain chain, ChatRequest request);

}
