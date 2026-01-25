package io.github.oldmanpushcart.dashscope4j.client.internal.base.api.flow;

import io.github.oldmanpushcart.dashscope4j.client.ApiRequest;
import io.github.oldmanpushcart.dashscope4j.client.ApiResponse;

import java.util.concurrent.Flow;

/**
 * 流式执行器
 */
public interface FlowApi {

    <T extends ApiRequest<R>, R extends ApiResponse> Flow.Publisher<R> execute(T request);

}
