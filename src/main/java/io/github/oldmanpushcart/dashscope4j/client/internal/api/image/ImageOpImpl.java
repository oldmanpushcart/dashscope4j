package io.github.oldmanpushcart.dashscope4j.client.internal.api.image;

import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.OpTask;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.api.image.ImageOp;
import io.github.oldmanpushcart.dashscope4j.client.api.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.image.generation.GenImageResponse;
import lombok.AllArgsConstructor;

import java.util.Collections;
import java.util.List;

@AllArgsConstructor
public class ImageOpImpl implements ImageOp {

    private static final List<Interceptor> interceptors = Collections.singletonList(
            new ProcessAutoUploadForGenImageInterceptor()
    );
    private final ApiOp apiOp;

    @Override
    public OpTask<GenImageRequest, GenImageResponse> generation() {
        return request -> {
            final GenImageRequest newRequest = GenImageRequest.newBuilder(request)
                    .addInterceptors(interceptors)
                    .build();
            return apiOp.executeTask(newRequest);
        };
    }

}
