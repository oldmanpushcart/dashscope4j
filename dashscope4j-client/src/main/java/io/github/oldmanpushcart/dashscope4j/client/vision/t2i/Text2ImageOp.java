package io.github.oldmanpushcart.dashscope4j.client.vision.t2i;

import io.github.oldmanpushcart.dashscope4j.client.Task;

import java.util.concurrent.CompletionStage;

public interface Text2ImageOp {

    CompletionStage<? extends Task.Half<Text2ImageResponse>> task(Text2ImageRequest request);

}
