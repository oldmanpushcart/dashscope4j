package io.github.oldmanpushcart.dashscope4j.client.internal.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.Task;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.TaskInterceptor;

import java.util.concurrent.CompletionStage;

public class BridgeTaskInterceptor implements TaskInterceptor {

    @Override
    public CompletionStage<? extends Task.Half<?>> intercept(Chain chain) {

        if (!(chain.request() instanceof ChatRequest request)) {
            return chain.proceed();
        }

        final var model = request.model();

        // 桥接 ASYNC 式输出
        if (model.tags().contains(ChatModelTags.ASYNC_OUTPUT_ONLY)) {
            return bridgeAsync(chain, request);
        }

        // 桥接 TASK 式输出
        else if (model.tags().contains(ChatModelTags.FLOW_OUTPUT_ONLY)) {
            return bridgeFlow(chain, request);
        }

        // 不用桥接，直接输出
        else {
            return chain.proceed();
        }
    }

    private CompletionStage<? extends Task.Half<?>> bridgeAsync(Chain chain, ChatRequest request) {
        final var task = new Task.Half<>()
        final var half = new Task.Half<ChatResponse>() {

            @Override
            public CompletionStage<ChatResponse> waitingFor(Task.WaitStrategy strategy) {
                strategy.performWait()
                return null;
            }

        };
    }

    private CompletionStage<? extends Task.Half<?>> bridgeFlow(Chain chain, ChatRequest request) {
    }

}
