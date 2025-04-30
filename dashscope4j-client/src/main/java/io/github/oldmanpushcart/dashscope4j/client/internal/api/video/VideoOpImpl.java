package io.github.oldmanpushcart.dashscope4j.client.internal.api.video;

import io.github.oldmanpushcart.dashscope4j.client.OpTask;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.api.video.VideoOp;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.ImageGenVideoRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.ImageGenVideoResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.TextGenVideoRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.video.generation.TextGenVideoResponse;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class VideoOpImpl implements VideoOp {

    private final ApiOp apiOp;

    @Override
    public OpTask<TextGenVideoRequest, TextGenVideoResponse> genByText() {
        return apiOp::executeTask;
    }

    @Override
    public OpTask<ImageGenVideoRequest, ImageGenVideoResponse> genByImage() {
        return apiOp::executeTask;
    }

}
