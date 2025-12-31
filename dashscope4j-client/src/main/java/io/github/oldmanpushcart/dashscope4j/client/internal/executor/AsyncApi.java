package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface AsyncApi {

    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(URI endpoint, T request);

    <T extends ApiRequest<R>, R extends ApiResponse> CompletionStage<R> execute(T request, Function<T, HttpRequest> transformer);

}
