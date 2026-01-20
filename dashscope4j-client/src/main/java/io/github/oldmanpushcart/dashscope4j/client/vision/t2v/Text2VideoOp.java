package io.github.oldmanpushcart.dashscope4j.client.vision.t2v;

import io.github.oldmanpushcart.dashscope4j.client.Task;

import java.util.concurrent.CompletionStage;

public interface Text2VideoOp {

    CompletionStage<? extends Task.Half<Text2VideoResponse>> task(Text2VideoRequest request);

}
