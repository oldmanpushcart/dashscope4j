package io.github.oldmanpushcart.dashscope4j.client.aigc.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcModelTags;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import static io.github.oldmanpushcart.dashscope4j.client.Task.WaitStrategies.always;
import static java.time.Duration.ofSeconds;

public class BridgeFlowInterceptor implements FlowInterceptor {

    @Override
    public CompletionStage<? extends Flow.Publisher<?>> intercept(Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)) {
            return chain.proceed();
        }

        final var model = aigcRequest.model();

        // FLOW 模式不用桥接，直接返回
        if (model.tags().contains(AigcModelTags.RESPONSE_MODE_FLOW)) {
            return chain.proceed();
        }

        // 桥接 ASYNC 式输出
        else if (model.tags().contains(AigcModelTags.RESPONSE_MODE_ASYNC)) {
            return bridgeAsync(chain, aigcRequest);
        }

        // 桥接 TASK 式输出
        else if (model.tags().contains(AigcModelTags.RESPONSE_MODE_TASK)) {
            return bridgeTask(chain, aigcRequest);
        }

        // 不用桥接，直接输出
        else {
            return chain.proceed();
        }
    }

    private CompletionStage<? extends Flow.Publisher<?>> bridgeAsync(Chain chain, AigcRequest<?, ?> request) {
        final var flow = FlowX.defer(() -> {
            final var newRequest = AigcRequest.newBuilder(request)
                    .addParameter(AigcParameterKeys.INCREMENTAL_OUTPUT, false)
                    .build();
            return FlowX.fromCompletionStage(chain.client().aigc().async(newRequest).thenApply(FlowX::just));
        });
        return CompletableFuture.completedStage(flow);
    }

    private CompletionStage<? extends Flow.Publisher<?>> bridgeTask(Chain chain, AigcRequest<?, ?> request) {
        final var flow = FlowX.defer(() -> {
            final var newRequest = AigcRequest.newBuilder(request)
                    .addParameter(AigcParameterKeys.INCREMENTAL_OUTPUT, false)
                    .build();
            final var stage = chain.client().aigc().task(newRequest)
                    .thenCompose(half -> half.waitingFor(always(ofSeconds(1L))));
            return FlowX.fromCompletionStage(stage.thenApply(FlowX::just));
        });
        return CompletableFuture.completedStage(flow);
    }

}
