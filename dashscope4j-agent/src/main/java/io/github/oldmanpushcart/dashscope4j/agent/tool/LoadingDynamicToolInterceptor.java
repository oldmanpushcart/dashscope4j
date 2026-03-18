package io.github.oldmanpushcart.dashscope4j.agent.tool;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class LoadingDynamicToolInterceptor implements ChatInterceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {

        final var value = request.context().get("DYNAMIC_TOOLS");
        if (value == null) {
            return chain.proceed();
        }

        //noinspection unchecked
        final var tools = (List<Tool>) value;

        final var newRequest = AigcRequest.newBuilder(request)
                .parameters(parameters -> {
                    final var existed = parameters.get("tools");
                    if (null != existed) {

                        final var uniqueSet = new HashSet<Tool>();

                        //noinspection unchecked
                        uniqueSet.addAll(((List<Tool>) existed));
                        uniqueSet.addAll(tools);

                        parameters.put("tools", new ArrayList<>(uniqueSet));
                    } else {
                        parameters.put("tools", tools);
                    }
                    return parameters;
                })
                .build();
        return chain.proceed(newRequest);
    }

}
