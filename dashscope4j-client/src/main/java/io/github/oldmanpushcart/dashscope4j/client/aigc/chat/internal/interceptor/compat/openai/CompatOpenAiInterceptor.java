package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor.compat.openai;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.common.util.flow.FlowX;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class CompatOpenAiInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Interceptor.Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)
                || !model.tags().contains(ChatModelTags.COMPAT_OPENAI)) {
            return chain.proceed();
        }

        //noinspection unchecked
        final var chatRequest = (AigcRequest<Input, Output>) aigcRequest;

        return switch (chain.type()) {

            /*
             * 处理 ASYNC
             */
            case ASYNC -> CompletableFuture.completedStage(chatRequest)
                    .thenApply(OpenAiChatHelper::toOpenAiChatRequest)
                    .thenCompose(chain::proceed)
                    .thenApply(v -> (OpenAiChatResponse) v)
                    .thenApply(OpenAiChatHelper::toAigcResponse);

            /*
             * 处理 FLOW
             */
            case FLOW -> CompletableFuture.completedStage(chatRequest)
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

            /*
             * 其他不处理
             */
            default -> chain.proceed();

        };

    }

}
