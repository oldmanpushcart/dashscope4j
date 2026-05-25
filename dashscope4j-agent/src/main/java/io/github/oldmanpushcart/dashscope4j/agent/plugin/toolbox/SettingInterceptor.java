package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolLookup;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.concurrent.CompletionStage;

/**
 * 预处理拦截器：设置
 */
class SettingInterceptor implements ChatInterceptor {

    private static final Message SEARCH_TOOLS_MESSAGE = Message
            .system(PromptTemplate.newBuilder()
                    .resource("/prompt/SEARCH_TOOLS.md")
                    .build()
                    .render())
            .withCache();

    private final Toolbox toolbox;
    private final Tool searchToolsTool;
    private final boolean enableSearchTools;

    public SettingInterceptor(Toolbox toolbox, boolean enableSearchTools) {
        this.toolbox = toolbox;
        this.searchToolsTool = new SearchToolsFunction(toolbox).asTool();
        this.enableSearchTools = enableSearchTools;
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        final var newRequest = AigcRequest.newBuilder(request)
                .input(input -> Input.newBuilder(input)
                        .toolLookups(lookups -> {
                            lookups.add(toolbox);
                            return lookups;
                        })
                        .building(inputBuilder -> {

                            // 如果设置了启用搜索工具，则需要设置上搜索工具的相关配置
                            if (enableSearchTools) {
                                inputBuilder
                                        .messages(messages -> {
                                            messages.add(0, SEARCH_TOOLS_MESSAGE);
                                            return messages;
                                        })
                                        .toolLookups(lookups -> {
                                            lookups.add(ToolLookup.single(searchToolsTool));
                                            return lookups;
                                        });
                            }

                        })
                        .build())
                .build();
        return chain.proceed(newRequest);
    }

}
