package io.github.oldmanpushcart.dashscope4j.image;

import io.github.oldmanpushcart.dashscope4j.OpTask;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageRequest;
import io.github.oldmanpushcart.dashscope4j.image.generation.GenImageResponse;

/**
 * 图像操作
 */
public interface ImageOp {

    /**
     * 文生图
     *
     * @param request 文生图请求
     * @return 操作
     */
    OpTask<GenImageResponse> generation(GenImageRequest request);

}
