package io.github.oldmanpushcart.dashscope4j.agent.hook.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.List;
import java.util.concurrent.CompletionStage;

class SettingInterceptor implements ChatInterceptor {

    private static final Message SEARCH_TOOLS_MESSAGE = Message
            .system(PromptTemplate.newBuilder()
                    .resource("/prompt/SEARCH_TOOLS.md")
                    .build()
                    .render())
            .withCache();

    private final List<Tool> tools;
    private final Toolbox toolbox;

    public SettingInterceptor(List<Tool> tools, Toolbox toolbox) {
        this.tools = tools;
        this.toolbox = toolbox;
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        final var newRequest = AigcRequest.newBuilder(request)
                .input(input -> Input.newBuilder(input)

                        // 添加静态工具
                        .tools(tools -> {
                            tools.addAll(this.tools);
                            return tools;
                        })

                        // 添加动态工具能力
                        .building(inputBuilder -> {

                            if (null == toolbox) {
                                return;
                            }


                            inputBuilder

                                    // 添加动态工具使用提示信息
                                    .messages(messages -> {
                                        messages.add(0, SEARCH_TOOLS_MESSAGE);
                                        return messages;
                                    })

                                    // 添加动态工具（搜索和调用）
                                    .tools(tools -> {
                                        tools.addAll(List.of(
                                                new SearchToolFunction(toolbox).asTool(),
                                                new InvokeToolFunction(toolbox).asTool()
                                        ));
                                        return tools;
                                    });

                        })

                        .build())
                .build();
        return chain.proceed(newRequest);
    }

}
