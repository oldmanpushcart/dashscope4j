package io.github.oldmanpushcart.dashscope4j.client.internal.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.chat.compat.openai.OpenAiChatHelper;
import io.github.oldmanpushcart.dashscope4j.client.internal.chat.compat.openai.OpenAiChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.chat.compat.openai.OpenAiChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class CompatOpenAiInterceptor implements FlowInterceptor, AsyncInterceptor {

    @Override
    public CompletionStage<?> intercept(AsyncInterceptor.Chain chain) {

        if (!(chain.request() instanceof ChatRequest chatRequest)) {
            return chain.proceed();
        }

        final var model = chatRequest.model();
        if(!model.tags().contains(ChatModelTags.COMPAT_OPENAI)) {
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

        final var model = chatRequest.model();
        if(!model.tags().contains(ChatModelTags.COMPAT_OPENAI)) {
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
                );
    }

}
