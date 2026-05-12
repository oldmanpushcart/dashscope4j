package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.concurrent.CompletionStage;

/**
 * 预处理拦截器：注入 Plan-Execute 系统提示词
 */
class SettingInterceptor implements ChatInterceptor {
    
    private static final Message PLAN_EXECUTE_SYSTEM_MESSAGE = Message
            .system(PromptTemplate.newBuilder()
                    .resource("/prompt/PLAN_EXECUTE_AGENT.md")
                    .build()
                    .render())
            .withCache();
    
    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        return chain.proceed(newPlanExecuteRequest(request));
    }
    
    /**
     * 重新构建适合 Plan-Execute 的请求
     */
    private AigcRequest<Input, Output> newPlanExecuteRequest(AigcRequest<Input, Output> request) {
        return AigcRequest.newBuilder(request)
                .input(input -> Input.newBuilder(input)
                        .messages(messages -> {
                            messages.add(0, PLAN_EXECUTE_SYSTEM_MESSAGE);
                            return messages;
                        })
                        .build())
                .parameters(parameters -> {
                    // 关闭工具调用，交由 Plan-Execute 模式完成
                    parameters.put("tool_choice", "none");
                    return parameters;
                })
                .build();
    }
}
