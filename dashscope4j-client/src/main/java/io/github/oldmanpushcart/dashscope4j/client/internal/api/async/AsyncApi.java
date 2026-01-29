package io.github.oldmanpushcart.dashscope4j.client.internal.api.async;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;

import java.util.concurrent.CompletionStage;

public interface AsyncApi {

    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(T request);

}
