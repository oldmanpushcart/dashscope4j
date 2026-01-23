package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.interceptor.tool;

import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.FlowInterceptor;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class ToolCallInterceptor implements AsyncInterceptor, FlowInterceptor {

    @Override
    public CompletionStage<?> intercept(AsyncInterceptor.Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel)) {
            return chain.proceed();
        }

        return chain.proceed()
                .thenApply(r -> {
                    //noinspection unchecked
                    return (AigcResponse<Output>) r;
                })
                .thenCompose(new ToolCallHandler(chain.client().aigc()));
    }

    @Override
    public CompletionStage<? extends Flow.Publisher<?>> intercept(FlowInterceptor.Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel)) {
            return chain.proceed();
        }

        return chain.proceed()
                .thenApply(r -> {
                    //noinspection unchecked
                    return (Flow.Publisher<AigcResponse<Output>>) r;
                })
                .thenApply(new ToolCallFlowHandler(chain.client().aigc()));
    }

}
