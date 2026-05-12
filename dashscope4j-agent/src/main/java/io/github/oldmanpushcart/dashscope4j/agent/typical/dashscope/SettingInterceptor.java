package io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.concurrent.CompletionStage;

/**
 * DashscopeAgent 设置拦截器
 * <p>
 * 负责在请求发送前进行预处理：
 * <ul>
 *     <li>注入系统提示词（DASHSCOPE_AGENT.md）</li>
 *     <li>配置模型参数（关闭并行工具调用）</li>
 * </ul>
 */
class SettingInterceptor implements ChatInterceptor {

    /**
     * DashscopeAgent 系统提示词消息
     */
    private static final Message DASHSCOPE_SYSTEM_MESSAGE = Message
            .system(PromptTemplate.newBuilder()
                    .resource("/prompt/DASHSCOPE_AGENT.md")
                    .build()
                    .render())
            .withCache();

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        final var newRequest = AigcRequest.newBuilder(request)
                .input(input -> Input.newBuilder(input)
                        .messages(messages -> {
                            // 注入系统提示词到消息列表首位
                            messages.add(0, DASHSCOPE_SYSTEM_MESSAGE);
                            return messages;
                        })
                        .build())
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
