package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface OpAsync<T extends ApiRequest<?, R>, R extends ApiResponse<?>> {

     CompletionStage<R> async(T request);

}
