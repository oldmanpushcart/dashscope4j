package io.github.oldmanpushcart.dashscope4j.client.internal.api.video;

import io.github.oldmanpushcart.dashscope4j.client.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.OpTask;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.api.video.VideoOp;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.ImageGenVideoRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.ImageGenVideoResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.TextGenVideoRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.TextGenVideoResponse;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
public class VideoOpImpl implements VideoOp {

    private static final List<Interceptor> interceptors = Arrays.asList(
            new ProcessAutoUploadForImageGenVideoInterceptor()
    );
    private final ApiOp apiOp;

    @Override
    public OpTask<TextGenVideoRequest, TextGenVideoResponse> genByText() {
        return apiOp::executeTask;
    }

    @Override
    public OpTask<ImageGenVideoRequest, ImageGenVideoResponse> genByImage() {
        return request -> {
            final ImageGenVideoRequest newRequest = ImageGenVideoRequest.newBuilder(request)
                    .interceptors(interceptors)
                    .build();
            return apiOp.executeTask(newRequest);
        };
    }

}
