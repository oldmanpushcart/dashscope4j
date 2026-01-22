package io.github.oldmanpushcart.dashscope4j.client.internal.base.api.executor;

import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.ApiResponse;
import io.github.oldmanpushcart.dashscope4j.client.Task;

import java.util.concurrent.CompletionStage;

public interface TaskApi {

    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<? extends Task.Half<R>> execute(T request);

}
