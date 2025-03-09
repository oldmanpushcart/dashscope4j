package io.github.oldmanpushcart.dashscope4j.api.video;

import io.github.oldmanpushcart.dashscope4j.OpTask;
import io.github.oldmanpushcart.dashscope4j.api.video.generation.ImageGenVideoRequest;
import io.github.oldmanpushcart.dashscope4j.api.video.generation.ImageGenVideoResponse;
import io.github.oldmanpushcart.dashscope4j.api.video.generation.TextGenVideoRequest;
import io.github.oldmanpushcart.dashscope4j.api.video.generation.TextGenVideoResponse;

/**
 * 视频操作
 *
 * @since 3.1.0
 */
public interface VideoOp {

    /**
     * @return 文生视频
     */
    OpTask<TextGenVideoRequest, TextGenVideoResponse> genByText();

    /**
     * @return 图生视频
     */
    OpTask<ImageGenVideoRequest, ImageGenVideoResponse> genByImage();

}
