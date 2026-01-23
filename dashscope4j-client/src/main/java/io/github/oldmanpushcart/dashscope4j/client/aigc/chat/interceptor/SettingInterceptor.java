package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.Task;
import io.github.oldmanpushcart.dashscope4j.client.aigc.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.AsyncInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.FlowInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.interceptor.TaskInterceptor;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

public class SettingInterceptor implements AsyncInterceptor, FlowInterceptor, TaskInterceptor {

    @Override
    public CompletionStage<?> intercept(AsyncInterceptor.Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)) {
            return chain.proceed();
        }

        final var request = aigcRequest.as(model);
        final var newRequest = rewriteAigcRequest(request);
        return chain.proceed(newRequest);
    }

    @Override
    public CompletionStage<? extends Flow.Publisher<?>> intercept(FlowInterceptor.Chain chain) {
        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)) {
            return chain.proceed();
        }

        final var request = aigcRequest.as(model);
        final var newRequest = rewriteAigcRequest(request);
        return chain.proceed(newRequest);
    }

    @Override
    public CompletionStage<? extends Task.Half<?>> intercept(TaskInterceptor.Chain chain) {
        if (!(chain.request() instanceof AigcRequest<?, ?> aigcRequest)
                || !(aigcRequest.model() instanceof ChatModel model)) {
            return chain.proceed();
        }

        final var request = aigcRequest.as(model);
        final var newRequest = rewriteAigcRequest(request);
        return chain.proceed(newRequest);
    }

    private AigcRequest<Input, Output> rewriteAigcRequest(AigcRequest<Input, Output> request) {
        return AigcRequest.newBuilder(request)
                .addParameter("result_format", "message")
                .build();
    }

}
