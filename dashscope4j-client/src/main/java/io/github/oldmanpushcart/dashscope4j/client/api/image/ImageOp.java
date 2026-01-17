package io.github.oldmanpushcart.dashscope4j.client.api.image;

import io.github.oldmanpushcart.dashscope4j.client.api.image.text2image.Text2ImageRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.image.text2image.Text2ImageResponse;
import io.github.oldmanpushcart.dashscope4j.client.task.Task;

import java.util.concurrent.CompletionStage;

public interface ImageOp {

    CompletionStage<? extends Task.Half<Text2ImageResponse>> text2image(Text2ImageRequest request);

}
