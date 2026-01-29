package io.github.oldmanpushcart.dashscope4j.client.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcModelTags;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.client.Task.WaitStrategies.always;
import static java.time.Duration.ofSeconds;

public class BridgeInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> request)) {
            return chain.proceed();
        }

        final var model = request.model();

        // process async request
        if (chain.type() == Type.ASYNC) {

            // bridge async tp async
            if (model.tags().contains(AigcModelTags.RESPONSE_MODE_ASYNC)) {
                return chain.proceed();
            }

            // bridge async to flow
            else if (model.tags().contains(AigcModelTags.RESPONSE_MODE_FLOW)) {
                final var newRequest = AigcRequest.newBuilder(request)
                        .addParameter(AigcParameterKeys.INCREMENTAL_OUTPUT, true)
                        .build();
                return FlowX.fromPublisher(chain.client().flow(newRequest))
                        .reduce(AigcResponse::accumulate);
            }

            // bridge async to task
            else if (model.tags().contains(AigcModelTags.RESPONSE_MODE_TASK)) {
                return chain.client().task(request)
                        .thenCompose(half -> half.waitingFor(always(ofSeconds(1L))));
            }

        }

        // process flow request
        else if (chain.type() == Type.FLOW) {

            // bridge flow to async
            if (model.tags().contains(AigcModelTags.RESPONSE_MODE_FLOW)) {
                return chain.proceed();
            }

            // bridge flow to flow
            else if (model.tags().contains(AigcModelTags.RESPONSE_MODE_ASYNC)) {
                final var flow = FlowX.defer(() -> {
                    final var newRequest = AigcRequest.newBuilder(request)

                            /*
                             * 这里是修复 qwen-long 对增量输出的问题
                             * 如果不设置 INCREMENTAL_OUTPUT 为 false，那么 qwen-long 会返回一个空的结果，导致无法获取到结果
                             */
                            .addParameter(AigcParameterKeys.INCREMENTAL_OUTPUT, false)

                            .build();
                    return FlowX.fromCompletionStage(chain.client().async(newRequest).thenApply(FlowX::just));
                });
                return CompletableFuture.completedStage(flow);
            }

            // bridge flow to task
            else if (model.tags().contains(AigcModelTags.RESPONSE_MODE_TASK)) {
                final var flow = FlowX.defer(() -> {
                    final var newRequest = AigcRequest.newBuilder(request)
                            .addParameter(AigcParameterKeys.INCREMENTAL_OUTPUT, false)
                            .build();
                    final var stage = chain.client().task(newRequest)
                            .thenCompose(half -> half.waitingFor(always(ofSeconds(1L))));
                    return FlowX.fromCompletionStage(stage.thenApply(FlowX::just));
                });
                return CompletableFuture.completedStage(flow);
            }

        }

        // process task request
        else if (chain.type() == Type.TASK) {

            // bridge task to task
            if (model.tags().contains(AigcModelTags.RESPONSE_MODE_TASK)) {
                return chain.proceed();
            }

            // bridge task to async or flow
            else if (model.tags().contains(AigcModelTags.RESPONSE_MODE_ASYNC)
                    || model.tags().contains(AigcModelTags.RESPONSE_MODE_FLOW)) {
                return CompletableFuture.failedStage(new UnsupportedOperationException("Task requests are not supported; model requires async or flow."));
            }

        }

        /*
         * 其他情况就不处理了，能走到这里的主要原因是
         * 1. 模型上没有任何响应模式限制标签
         * 2. 出现了新的响应模式但这里忘记修改
         */
        return chain.proceed();
    }

}
