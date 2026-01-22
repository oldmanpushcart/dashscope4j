package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.Task;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.TaskInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModelTags;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class BridgeTaskInterceptor implements TaskInterceptor {

    @Override
    public CompletionStage<? extends Task.Half<?>> intercept(Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)) {
            return chain.proceed();
        }

        // TASK 模式不用桥接，直接输出
        if (model.tags().contains(ChatModelTags.RESPONSE_MODE_TASK)) {
            return chain.proceed();
        }

        // 桥接 ASYNC 式输出
        else if (model.tags().contains(ChatModelTags.RESPONSE_MODE_ASYNC)) {
            return bridgeAsync(chain, aigcRequest);
        }

        // 桥接 FLOW 式输出
        else if (model.tags().contains(ChatModelTags.RESPONSE_MODE_FLOW)) {
            return bridgeFlow(chain, aigcRequest);
        }

        // 不用桥接，直接输出
        else {
            return chain.proceed();
        }
    }

    private CompletionStage<? extends Task.Half<?>> bridgeAsync(Chain chain, AigcRequest<?, ?> request) {
        return CompletableFuture.failedStage(new UnsupportedOperationException("Not supported"));
    }

    private CompletionStage<? extends Task.Half<?>> bridgeFlow(Chain chain, AigcRequest<?, ?> request) {
        return CompletableFuture.failedStage(new UnsupportedOperationException("Not supported"));
    }

}
