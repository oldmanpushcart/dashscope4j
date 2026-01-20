package io.github.oldmanpushcart.dashscope4j.client.internal.vision.t2i.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.vision.t2i.Text2ImageRequest;
import io.github.oldmanpushcart.dashscope4j.client.internal.executor.TaskInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.Task;

import java.net.URI;
import java.util.concurrent.CompletionStage;

public class UploadFilesInterceptor implements TaskInterceptor {

    private static boolean isFileURI(URI resourceURI) {
        return null != resourceURI && "file".equalsIgnoreCase(resourceURI.getScheme());
    }

    @Override
    public CompletionStage<? extends Task.Half<?>> intercept(Chain chain) {

        if (!(chain.request() instanceof Text2ImageRequest request)) {
            return chain.proceed();
        }

        if (!request.uploadEnabled() || !isFileURI(request.reference())) {
            return chain.proceed();
        }

        return chain.client().base().store().upload(request.reference(), request.model())
                .thenCompose(uploadedURI -> {
                    final var newRequest = Text2ImageRequest.newBuilder(request)
                            .reference(uploadedURI)
                            .build();
                    return chain.proceed(newRequest);
                });
    }

}
