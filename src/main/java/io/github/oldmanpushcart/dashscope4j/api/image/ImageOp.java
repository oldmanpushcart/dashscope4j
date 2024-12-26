package io.github.oldmanpushcart.dashscope4j.api.image;

import io.github.oldmanpushcart.dashscope4j.OpTask;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.api.image.generation.GenImageResponse;

/**
 * 图像操作
 */
public interface ImageOp {

    /**
     * @return 文生图
     */
    OpTask<GenImageRequest, GenImageResponse> generation();

}
