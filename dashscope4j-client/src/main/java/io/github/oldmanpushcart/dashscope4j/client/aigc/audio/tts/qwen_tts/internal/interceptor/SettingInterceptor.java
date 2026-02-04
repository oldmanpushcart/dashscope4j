package io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.aigc.audio.tts.qwen_tts.QwenTtsModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;

import java.util.concurrent.CompletionStage;

public class SettingInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof QwenTtsModel model)) {
            return chain.proceed();
        }

        final var request = aigcRequest.as(model);
        if (chain.type() == Type.FLOW) {
            final var newRequest = AigcRequest.newBuilder(request)
                    .input(QwenTtsModel.Input.newBuilder(request.input())
                            .stream(true)
                            .build())
                    .build();
            return chain.proceed(newRequest);
        }

        return chain.proceed();
    }

}
