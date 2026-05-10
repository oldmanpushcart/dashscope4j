package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.concurrent.CompletionStage;

class SettingInterceptor implements ChatInterceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        final var newRequest = AigcRequest.newBuilder(request)
                .parameters(parameters -> {

                    /*
                     * 暂时先关闭并行调用
                     * 目前大模型还不够聪明，一些调用其实是有先后关系的。
                     */
                    parameters.put("parallel_tool_calls", false);

                    return parameters;
                })
                .build();
        return chain.proceed(newRequest);
    }

}
