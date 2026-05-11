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
 * 注入工具箱拦截器
 * <p>
 * 负责在请求准备阶段将工具箱中的工具注入到请求参数中。
 * </p>
 */
class InjectInterceptor implements ChatInterceptor {

    private static final Message SEARCH_TOOLS_MESSAGE = Message
            .system(PromptTemplate.newBuilder()
                    .template(InjectInterceptor.class.getResourceAsStream("/prompt/SEARCH_TOOLS.md"))
                    .build()
                    .render())
            .withCache();

    private final Toolbox toolbox;
    private final Tool searchToolsTool;

    public InjectInterceptor(Toolbox toolbox, Tool searchToolsTool) {
        this.toolbox = toolbox;
        this.searchToolsTool = searchToolsTool;
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        final var newRequest = AigcRequest.newBuilder(request)
                .input(input -> Input.newBuilder(input)
                        .messages(messages-> {
                            messages.add(0, SEARCH_TOOLS_MESSAGE);
                            return messages;
                        })
                        .lookups(lookups -> {
                            lookups.add(toolbox);
                            lookups.add(ToolLookup.single(searchToolsTool));
                            return lookups;
                        })
                        .build())
                .build();
        return chain.proceed(newRequest);
    }

}
