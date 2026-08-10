package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.Interceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.util.Objects;
import java.util.concurrent.CompletionStage;

/**
 * 功能拦截器：设置对话模型的必要参数
 */
public class SettingInterceptor implements Interceptor {

    @Override
    public CompletionStage<?> intercept(Chain chain) {

        if (!(chain.request() instanceof AigcRequest<?, ?> request)
                || !(request.model() instanceof ChatModel model)) {
            return chain.proceed();
        }

        final var chatRequest = request.as(model);

        final var newRequest = AigcRequest.newBuilder(chatRequest)
                .parameters(parameters -> {

                    // 设置结果消息格式为：MESSAGE
                    parameters.put("result_format", "message");

                    /*
                     * 设置工具集
                     * 如果指定了：tool_choice=none，则说明当前请求不需要TOOL
                     */
                    if (!Objects.equals("none", parameters.get("tool_choice"))) {
                        final var tools = chatRequest.input().tools();
                        if (CommonUtils.isNotEmpty(tools)) {
                            parameters.put("tools", tools);
                        }
                    }

                    return parameters;
                })
                .build();
        return chain.proceed(newRequest);
    }

}
