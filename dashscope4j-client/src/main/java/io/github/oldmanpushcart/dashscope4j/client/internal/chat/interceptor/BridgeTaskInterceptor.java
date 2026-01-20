package io.github.oldmanpushcart.dashscope4j.client.internal.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.Task;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.TaskInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.TagUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class BridgeTaskInterceptor implements TaskInterceptor {

    @Override
    public CompletionStage<? extends Task.Half<?>> intercept(Chain chain) {

        if (!(chain.request() instanceof ChatRequest request)) {
            return chain.proceed();
        }

        final var model = request.model();

        // TASK 模式不用桥接，直接输出
        if (model.tags().contains(ChatModelTags.RESPONSE_MODE_TASK)) {
            return chain.proceed();
        }

        // 桥接 ASYNC 式输出
        else if (model.tags().contains(ChatModelTags.RESPONSE_MODE_ASYNC)) {
            return bridgeAsync(chain, request);
        }

        // 桥接 FLOW 式输出
        else if (model.tags().contains(ChatModelTags.RESPONSE_MODE_FLOW)) {
            return bridgeFlow(chain, request);
        }

        // 不用桥接，直接输出
        else {
            return chain.proceed();
        }
    }

    private CompletionStage<? extends Task.Half<?>> bridgeAsync(Chain chain, ChatRequest request) {
        return CompletableFuture.failedStage(new UnsupportedOperationException("Not supported"));
    }

    private CompletionStage<? extends Task.Half<?>> bridgeFlow(Chain chain, ChatRequest request) {
        return CompletableFuture.failedStage(new UnsupportedOperationException("Not supported"));
    }

}
