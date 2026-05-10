package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 预处理拦截器：注入关键信息
 */
class InjectInterceptor implements ChatInterceptor {

    private static final Message REACT_SYSTEM_MESSAGE = Message
            .system(PromptTemplate.newBuilder()
                    .template(ReActAgent.class.getResourceAsStream("/prompt/REACT_AGENT.md"))
                    .build()
                    .render())
            .withCache();

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        return chain.proceed(newReActRequest(request));
    }

    /**
     * 重新构建适合 ReAct 的请求
     *
     * @param request 原始请求
     * @return 重新构建的请求
     */
    private AigcRequest<Input, Output> newReActRequest(AigcRequest<Input, Output> request) {

        //noinspection unchecked
        final var tools = (List<Tool>) (request.parameters().getOrDefault("tools", List.of()));

        return AigcRequest.newBuilder(request)
                .input(input -> Input.newBuilder(input)
                        .messages(messages -> {

                            // 添加到 SystemMessage
                            messages.add(0, REACT_SYSTEM_MESSAGE);

                            return messages;

                        })
                        .build())

                .parameters(parameters -> {

                    // 关闭工具调用，交由ReAct完成
                    parameters.put("tool_choice", "none");

                    // 停止词
                    parameters.put("stop", List.of(
                            "\n" + ReAct.OBSERVATION + ":"
                    ));

                    return parameters;
                })
                .build();
    }

}
