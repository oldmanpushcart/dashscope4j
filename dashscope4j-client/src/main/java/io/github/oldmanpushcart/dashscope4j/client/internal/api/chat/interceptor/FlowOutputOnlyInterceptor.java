package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class FlowOutputOnlyInterceptor implements AsyncInterceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof ChatRequest request)) {
            return chain.proceed();
        }

        final var model = request.model();
        if(!model.tags().contains(ChatModelTags.FLOW_OUTPUT_ONLY)) {
            return chain.proceed();
        }

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

}
