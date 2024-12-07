package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface OpAsync<R extends ApiResponse<?>> {

     CompletionStage<R> async(ApiRequest<?, R> request);

}
