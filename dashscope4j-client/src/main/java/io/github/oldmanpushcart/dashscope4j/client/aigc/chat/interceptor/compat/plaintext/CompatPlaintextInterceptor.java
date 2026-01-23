package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.interceptor.compat.plaintext;

import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModelTags;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.FlowInterceptor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class CompatPlaintextInterceptor implements FlowInterceptor, AsyncInterceptor {

    @Override
    public CompletionStage<?> intercept(AsyncInterceptor.Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)
                || !model.tags().contains(ChatModelTags.COMPAT_PLAINTEXT)) {
            return chain.proceed();
        }

        //noinspection unchecked
        final var chatRequest = (AigcRequest<Input, Output>) aigcRequest;

        return CompletableFuture.completedStage(chatRequest)
                .thenApply(r -> {
                    final var plaintextChatModel = new PlaintextChatModel(model.name(), model.path());
                    return AigcRequest.newBuilder(plaintextChatModel)
                            .parameters(chatRequest.parameters())
                            .input(new PlaintextChatModel.Input(chatRequest.input().messages()))
                            .build();
                })
                .thenCompose(chain::proceed);
    }

    @Override
    public CompletionStage<? extends Flow.Publisher<?>> intercept(FlowInterceptor.Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)
                || !model.tags().contains(ChatModelTags.COMPAT_PLAINTEXT)) {
            return chain.proceed();
        }

        //noinspection unchecked
        final var chatRequest = (AigcRequest<Input, Output>) aigcRequest;

        return CompletableFuture.completedStage(chatRequest)
                .thenApply(r -> {
                    final var plaintextChatModel = new PlaintextChatModel(model.name(), model.path());
                    return AigcRequest.newBuilder(plaintextChatModel)
                            .parameters(chatRequest.parameters())
                            .input(new PlaintextChatModel.Input(chatRequest.input().messages()))
                            .build();
                })
                .thenCompose(chain::proceed);
    }

}

