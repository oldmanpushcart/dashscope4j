package io.github.oldmanpushcart.dashscope4j.client.api.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;

import java.util.concurrent.CompletionStage;

public interface ChatInterceptor extends Interceptor {

    @Override
    default CompletionStage<?> intercept(Chain chain) {
        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)) {
            return chain.proceed();
        }
        final var request = aigcRequest.as(model);
        return intercept(chain, request);
    }

    CompletionStage<?> intercept(Chain chain, AigcRequest<ChatModel.Input, ChatModel.Output> request);

}
