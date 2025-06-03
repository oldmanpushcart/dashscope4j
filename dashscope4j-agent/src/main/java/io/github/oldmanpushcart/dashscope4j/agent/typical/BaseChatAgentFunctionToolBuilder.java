package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionTool;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.common.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

/**
 * 记忆体函数工具构建器
 */
@Getter(AccessLevel.PACKAGE)
@Accessors(fluent = true)
class BaseChatAgentFunctionToolBuilder implements ChatAgent.FunctionToolBuilder {

    private static final AtomicInteger identityGen = new AtomicInteger(100);
    private final BaseChatAgent agent;
    private String name;
    private String description;

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
    public BaseChatAgentFunctionToolBuilder description(String description) {
        requireNonBlankString(description, "description is blank!");
        this.description = description;
        return this;
    }

    private String buildingName() {
        return Objects.requireNonNullElseGet(name, () ->
                "agent_chat_function_%s".formatted(identityGen.getAndIncrement()));
    }

    private String buildingDescription() {
        return Objects.requireNonNullElseGet(description, () ->
                agent.functionTools().stream()
                        .map(FunctionTool::meta)
                        .map(FunctionTool.Meta::description)
                        .collect(Collectors.joining("\n\n")));
    }

    @Override
    public FunctionTool build() {
        requireNonNull(agent);
        return new FunctionTool() {

            private final FunctionTool delegate = ChatFunctionTool.newBuilder()
                    .name(buildingName())
                    .description(buildingDescription())
                    .parameterType(BaseChatAgentFunction.Parameter.class)
                    .function(new BaseChatAgentFunction(agent))
                    .build();

            @Override
            public Meta meta() {
                return new Meta(
                        delegate.meta().name(),
                        buildingDescription(),
                        delegate.meta().parameterSchema()
                );
            }

            @Override
            public CompletionStage<String> call(Caller caller, String argument) {
                return delegate.call(caller, argument);
            }

        };

    }

}
