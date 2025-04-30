package io.github.oldmanpushcart.dashscope4j.client.internal.api.image;

import io.github.oldmanpushcart.dashscope4j.client.OpTask;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.client.api.image.ImageOp;
import io.github.oldmanpushcart.dashscope4j.client.api.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.image.generation.GenImageResponse;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ImageOpImpl implements ImageOp {

    private final ApiOp apiOp;

    @Override
    public OpTask<GenImageRequest, GenImageResponse> generation() {
        return apiOp::executeTask;
    }

}
