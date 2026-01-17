package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.task.Task;

import java.util.concurrent.CompletionStage;

public interface TaskApi {

    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> execute(T request);

}
