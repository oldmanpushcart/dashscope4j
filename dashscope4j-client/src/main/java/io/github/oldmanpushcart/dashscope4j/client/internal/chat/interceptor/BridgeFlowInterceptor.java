package io.github.oldmanpushcart.dashscope4j.client.internal.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.Task;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class BridgeFlowInterceptor implements FlowInterceptor {

    @Override
    public CompletionStage<? extends Flow.Publisher<?>> intercept(Chain chain) {

        if (!(chain.request() instanceof ChatRequest request)) {
            return chain.proceed();
        }

        final var model = request.model();

        // FLOW 模式不用桥接，直接返回
        if (model.tags().contains(ChatModelTags.RESPONSE_MODE_FLOW)) {
            return chain.proceed();
        }

        // 桥接 ASYNC 式输出
        else if (model.tags().contains(ChatModelTags.RESPONSE_MODE_ASYNC)) {
            return bridgeAsync(chain, request);
        }

        // 桥接 TASK 式输出
        else if (model.tags().contains(ChatModelTags.RESPONSE_MODE_TASK)) {
            return bridgeTask(chain, request);
        }

        // 不用桥接，直接输出
        else {
            return chain.proceed();
        }
    }

    private CompletionStage<? extends Flow.Publisher<?>> bridgeAsync(Chain chain, ChatRequest request) {
        final var flow = FlowX.defer(() -> {
            final var chatOp = chain.client().chat();
            final var newRequest = ChatRequest.newBuilder(request)
                    .parameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, false)
                    .build();
            return FlowX.fromCompletionStage(chatOp.async(newRequest).thenApply(FlowX::just));
        });
        return CompletableFuture.completedStage(flow);
    }

    private CompletionStage<? extends Flow.Publisher<?>> bridgeTask(Chain chain, ChatRequest request) {
        final var flow = FlowX.defer(() -> {
            final var chatOp = chain.client().chat();
            final var newRequest = ChatRequest.newBuilder(request)
                    .parameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, false)
                    .build();
            final var stage = chatOp.task(newRequest)
                    .thenCompose(half -> half.waitingFor(Task.WaitStrategies.always(Duration.ofSeconds(1L))));
            return FlowX.fromCompletionStage(stage.thenApply(FlowX::just));
        });
        return CompletableFuture.completedStage(flow);
    }

}
