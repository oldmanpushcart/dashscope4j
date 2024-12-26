package io.github.oldmanpushcart.dashscope4j.internal.api.image;

import io.github.oldmanpushcart.dashscope4j.OpTask;
import io.github.oldmanpushcart.dashscope4j.api.ApiOp;
import io.github.oldmanpushcart.dashscope4j.api.image.ImageOp;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageResponse;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ImageOpImpl implements ImageOp {

    private final ApiOp apiOp;

    @Override
    public OpTask<GenImageRequest, GenImageResponse> generation() {
        return apiOp::executeTask;
    }

}
