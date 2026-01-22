package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.interceptor.compat.openai;

import io.github.oldmanpushcart.dashscope4j.client.interceptor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.TagUtils;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class CompatOpenAiInterceptor implements FlowInterceptor, AsyncInterceptor {

    @Override
    public CompletionStage<?> intercept(AsyncInterceptor.Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)
                || !TagUtils.contains(model.tags(), "compat", "openai")) {
            return chain.proceed();
        }

        //noinspection unchecked
        final var chatRequest = (AigcRequest<Input, Output>) aigcRequest;

        return CompletableFuture.completedStage(chatRequest)
                .thenApply(OpenAiChatHelper::toOpenAiChatRequest)
                .thenCompose(chain::proceed)
                .thenApply(v -> (OpenAiChatResponse) v)
                .thenApply(OpenAiChatHelper::toAigcResponse);
    }

    @Override
    public CompletionStage<? extends Flow.Publisher<?>> intercept(FlowInterceptor.Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)
                || !model.tags().contains(ChatModelTags.COMPAT_OPENAI)) {
            return chain.proceed();
        }

        //noinspection unchecked
        final var chatRequest = (AigcRequest<Input, Output>) aigcRequest;

        return CompletableFuture.completedStage(chatRequest)
                .thenApply(r ->
                        AigcRequest.newBuilder(r)
                                .addParameter("stream", true)
                                .addParameter("stream_options", Map.of("include_usage", true))
                                .addParameter("enable_omni_output_audio_url", true)
                                .build())
                .thenApply(OpenAiChatHelper::toOpenAiChatRequest)
                .thenCompose(chain::proceed)
                .thenApply(v -> {
                    //noinspection unchecked
                    return (Flow.Publisher<OpenAiChatResponse>) v;
                })
                .thenApply(publisher ->
                        FlowX.fromPublisher(publisher)
                                .map(OpenAiChatHelper::toAigcResponse)
                );
    }

}
