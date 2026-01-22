package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import static io.github.oldmanpushcart.dashscope4j.client.Task.WaitStrategies.always;
import static java.time.Duration.ofSeconds;

public class BridgeFlowInterceptor implements FlowInterceptor {

    @Override
    public CompletionStage<? extends Flow.Publisher<?>> intercept(Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)) {
            return chain.proceed();
        }

        // FLOW 模式不用桥接，直接返回
        if (model.tags().contains(ChatModelTags.RESPONSE_MODE_FLOW)) {
            return chain.proceed();
        }

        // 桥接 ASYNC 式输出
        else if (model.tags().contains(ChatModelTags.RESPONSE_MODE_ASYNC)) {
            return bridgeAsync(chain, aigcRequest);
        }

        // 桥接 TASK 式输出
        else if (model.tags().contains(ChatModelTags.RESPONSE_MODE_TASK)) {
            return bridgeTask(chain, aigcRequest);
        }

        // 不用桥接，直接输出
        else {
            return chain.proceed();
        }
    }

    private CompletionStage<? extends Flow.Publisher<?>> bridgeAsync(Chain chain, AigcRequest<?, ?, ?> request) {
        final var flow = FlowX.defer(() -> {
            final var newRequest = AigcRequest.newBuilder(request)
                    .addParameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, false)
                    .build();
            return FlowX.fromCompletionStage(chain.client().async(newRequest).thenApply(FlowX::just));
        });
        return CompletableFuture.completedStage(flow);
    }

    private CompletionStage<? extends Flow.Publisher<?>> bridgeTask(Chain chain, AigcRequest<?, ?, ?> request) {
        final var flow = FlowX.defer(() -> {
            final var newRequest = AigcRequest.newBuilder(request)
                    .addParameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, false)
                    .build();
            final var stage = chain.client().task(newRequest)
                    .thenCompose(half -> half.waitingFor(always(ofSeconds(1L))));
            return FlowX.fromCompletionStage(stage.thenApply(FlowX::just));
        });
        return CompletableFuture.completedStage(flow);
    }

}
