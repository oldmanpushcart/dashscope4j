package io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2v.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.Task;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.vision.t2v.TextToVideoModel;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.TaskInterceptor;

import java.util.concurrent.CompletionStage;

public class UploadFilesInterceptor implements TaskInterceptor {

    @Override
    public CompletionStage<? extends Task.Half<?>> intercept(Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof TextToVideoModel model)) {
            return chain.proceed();
        }

        final var request = aigcRequest.as(model);
        if (!request.input().uploadEnabled()) {
            return chain.proceed();
        }

        return chain.client().base().store().upload(request.input().audio(), request.model())
                .thenCompose(uploadedURI -> {
                    final var newRequest = AigcRequest.newBuilder(request)
                            .input(TextToVideoModel.Input.newBuilder()
                                    .audio(uploadedURI)
                                    .build())
                            .build();
                    return chain.proceed(newRequest);
                });
    }

}
