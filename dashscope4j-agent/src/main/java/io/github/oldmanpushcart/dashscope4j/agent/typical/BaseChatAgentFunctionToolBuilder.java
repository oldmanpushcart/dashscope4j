package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.prompt.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunctionTool;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.concurrent.atomic.AtomicInteger;

import static io.github.oldmanpushcart.dashscope4j.client.internal.util.CommonUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

/**
 * 记忆体函数工具构建器
 */
@Getter(AccessLevel.PACKAGE)
@Accessors(fluent = true)
class BaseChatAgentFunctionToolBuilder implements ChatAgent.FunctionToolBuilder {

    private static final AtomicInteger identityGen = new AtomicInteger(100);
    private final BaseChatAgent agent;
    private String name = String.format("base_chat_agent_function_" + identityGen.getAndIncrement());
    private String summary = "具备以下能力";

    public BaseChatAgentFunctionToolBuilder(BaseChatAgent agent) {
        requireNonNull(agent, "agent is required!");
        this.agent = agent;
    }

    @Override
    public BaseChatAgentFunctionToolBuilder name(String name) {
        requireNonBlankString(name, "name is blank!");
        this.name = name;
        return this;
    }

    @Override
    public BaseChatAgentFunctionToolBuilder summary(String summary) {
        requireNonBlankString(summary, "name is blank!");
        this.summary = summary;
        return this;
    }

    private String buildingDescription() {
        return PromptTemplate.newBuilder()
                .template("## ${summary}\n" +
                          "${detail}")
                .variable("summary", summary)
                .variable("detail", () -> {
                    final StringBuilder stringBuf = new StringBuilder();
                    for (final ChatFunctionTool tool : agent.functionTools()) {
                        stringBuf
                                .append("\n")
                                .append(tool.meta().description())
                                .append("\n\n");
                    }
                    return stringBuf;
                })
                .build()
                .render();
    }

    @Override
    public ChatFunctionTool build() {
        requireNonNull(agent);
        requireNonBlankString(name, "name is blank!");
        requireNonBlankString(summary, "summary is blank");
        return ChatFunctionTool.newBuilder()
                .name(name)
                .description(buildingDescription())
                .parameterType(BaseChatAgentFunction.Parameter.class)
                .function(new BaseChatAgentFunction(agent))
                .build();
    }

}
