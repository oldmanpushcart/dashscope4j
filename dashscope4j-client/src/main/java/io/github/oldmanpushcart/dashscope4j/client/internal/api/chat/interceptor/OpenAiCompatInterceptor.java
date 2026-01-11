package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.compat.openai.OpenAiChatHelper;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.compat.openai.OpenAiChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.api.chat.compat.openai.OpenAiChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import static io.github.oldmanpushcart.dashscope4j.common.util.CommonUtils.hasKeyValue;

public class OpenAiCompatInterceptor implements FlowInterceptor, AsyncInterceptor {

    @Override
    public CompletionStage<?> intercept(AsyncInterceptor.Chain chain) {

        if (!(chain.request() instanceof ChatRequest chatRequest)) {
            return chain.proceed();
        }

        final var features = chatRequest.model().features();
        if (!hasKeyValue(features, "compat", "openai")) {
            return chain.proceed();
        }

        return CompletableFuture.completedStage(chatRequest)
                .thenApply(OpenAiChatHelper::toOpenAiChatRequest)
                .thenCompose(chain::proceed)
                .thenApply(v -> (OpenAiChatResponse) v)
                .thenApply(OpenAiChatHelper::toChatResponse);
    }

    @Override
    public CompletionStage<? extends Flow.Publisher<?>> intercept(FlowInterceptor.Chain chain) {

        if (!(chain.request() instanceof ChatRequest chatRequest)) {
            return chain.proceed();
        }

        final var featureMap = chatRequest.model().features();
        if (!(featureMap.containsKey("compat") && "openai".equals(featureMap.get("compat")))) {
            return chain.proceed();
        }

        return CompletableFuture.completedStage(chatRequest)
                .thenApply(OpenAiChatHelper::toOpenAiChatRequest)
                .thenApply(r ->
                        OpenAiChatRequest.newBuilder(r)
                                .parameter("stream", true)
                                .parameter("stream_options", Map.of("include_usage", true))
                                .parameter("enable_omni_output_audio_url", true)
                                .build())
                .thenCompose(chain::proceed)
                .thenApply(v -> {
                    //noinspection unchecked
                    return (Flow.Publisher<OpenAiChatResponse>) v;
                })
                .thenApply(publisher ->
                        FlowX.fromPublisher(publisher)
                                .map(OpenAiChatHelper::toChatResponse)
                                .publisher()
                );
    }

}
