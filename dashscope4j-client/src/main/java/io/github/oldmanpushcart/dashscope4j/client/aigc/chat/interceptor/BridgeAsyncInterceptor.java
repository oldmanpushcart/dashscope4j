package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.client.Task.WaitStrategies.always;
import static java.time.Duration.ofSeconds;

public class BridgeAsyncInterceptor implements AsyncInterceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)) {
            return chain.proceed();
        }

        // ASYNC 模式不用桥接，直接返回
        if (model.tags().contains(ChatModelTags.RESPONSE_MODE_ASYNC)) {
            return chain.proceed();
        }

        // 桥接 FLOW 式输出
        else if (model.tags().contains(ChatModelTags.RESPONSE_MODE_FLOW)) {
            return bridgeFlow(chain, aigcRequest);
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

    private CompletionStage<?> bridgeFlow(Chain chain, AigcRequest<?, ?> request) {
        final var newRequest = AigcRequest.newBuilder(request)
                .addParameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, true)
                .build();
        return FlowX.fromPublisher(chain.client().aigc().flow(newRequest))
                .collect(Collectors.toList())
                .thenApply(responses ->
                        responses.stream()
                                .reduce(AigcResponse::accumulate)
                                .orElseThrow());
    }

    private CompletionStage<?> bridgeTask(Chain chain, AigcRequest<?, ?> request) {
        return chain.client().aigc().task(request)
                .thenCompose(half -> half.waitingFor(always(ofSeconds(1L))));
    }

}
