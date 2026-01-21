package io.github.oldmanpushcart.dashscope4j.client.internal.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.Task;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class BridgeAsyncInterceptor implements AsyncInterceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof ChatRequest request)) {
            return chain.proceed();
        }

        final var model = request.model();

        // ASYNC 模式不用桥接，直接返回
        if (model.tags().contains(ChatModelTags.RESPONSE_MODE_ASYNC)) {
            return chain.proceed();
        }

        // 桥接 FLOW 式输出
        else if (model.tags().contains(ChatModelTags.RESPONSE_MODE_FLOW)) {
            return bridgeFlow(chain, request);
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

    private CompletionStage<?> bridgeFlow(Chain chain, ChatRequest request) {
        final var newRequest = ChatRequest.newBuilder(request)
                .parameter(ChatParameterKeys.ENABLE_INCREMENTAL_OUTPUT, true)
                .build();
        return FlowX.fromPublisher(chain.client().chat().flow(newRequest))
                .collect(Collectors.toList())
                .thenApply(responses ->
                        responses.stream()
                                .reduce(ChatResponse::accumulate)
                                .orElseThrow());
    }

    private CompletionStage<?> bridgeTask(Chain chain, ChatRequest request) {
        return chain.client().chat().task(request)
                .thenCompose(half -> half.waitingFor(Task.WaitStrategies.always(Duration.ofSeconds(1L))));
    }

}
