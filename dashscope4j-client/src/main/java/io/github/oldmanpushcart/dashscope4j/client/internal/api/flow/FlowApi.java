package io.github.oldmanpushcart.dashscope4j.client.internal.api.flow;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;
import org.reactivestreams.Publisher;

public interface FlowApi {

    <T extends ApiRequest<R>, R extends ApiResponse> Publisher<R> execute(T request);

}
