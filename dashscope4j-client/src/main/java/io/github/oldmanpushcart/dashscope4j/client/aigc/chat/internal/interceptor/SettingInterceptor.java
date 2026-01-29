package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;

import java.util.concurrent.CompletionStage;

public class SettingInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Interceptor.Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> request)
                || !(request.model() instanceof ChatModel model)) {
            return chain.proceed();
        }

        final var newRequest = AigcRequest.newBuilder(request.as(model))
                .addParameter("result_format", "message")
                .build();
        return chain.proceed(newRequest);
    }

}
