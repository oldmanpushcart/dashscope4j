package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.api.ApiResponse;
import io.reactivex.rxjava3.core.Flowable;

import java.util.concurrent.CompletionStage;

@FunctionalInterface
public interface OpFlow<R extends ApiResponse<?>> {

    CompletionStage<Flowable<R>> flow(ApiRequest<?, R> request);

}
