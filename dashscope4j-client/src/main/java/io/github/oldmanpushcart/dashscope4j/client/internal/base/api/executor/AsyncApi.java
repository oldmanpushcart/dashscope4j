package io.github.oldmanpushcart.dashscope4j.client.internal.base.api.executor;

import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.ApiResponse;

import java.util.concurrent.CompletionStage;

public interface AsyncApi {

    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(T request);

}
