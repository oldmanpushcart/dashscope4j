package io.github.oldmanpushcart.dashscope4j.client.internal.executor;

import io.github.oldmanpushcart.dashscope4j.client.api.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.ApiResponse;

import java.net.URI;
import java.util.concurrent.Flow;

/**
 * 流式执行器
 */
public interface FlowApi {

    <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> execute(T request);

}
