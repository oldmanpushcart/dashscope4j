package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolExecutionException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

/**
 * 工具调用函数
 */
class InvokeToolFunction
        implements BiFunction<Tool.Caller, InvokeToolFunction.Invocation, CompletionStage<String>> {

    private final Toolbox toolbox;

    public InvokeToolFunction(Toolbox toolbox) {
        this.toolbox = toolbox;
    }

    @Override
    public CompletionStage<String> apply(Tool.Caller caller, Invocation invocation) {
        return CompletableFuture.completedStage(null)
                .thenApply(u -> requyireTool(invocation.name()))
                .thenCompose(t -> t.call(caller, invocation.arguments()));
    }

    private Tool requyireTool(String name) {
        return toolbox.lookupByName(name)
                .orElseThrow(() -> ToolExecutionException.notFound(name));
    }

    /**
     * 工具调用
     *
     * @param name      工具名称
     * @param arguments 调用参数
     */
    public record Invocation(

            @JsonPropertyDescription("工具名称")
            @JsonProperty(value = "name", required = true)
            String name,

            @JsonPropertyDescription("工具入参JSON")
            @JsonProperty(value = "arguments", required = true)
            String arguments

    ) {

    }

    /**
     * 转换为 FunctionTool
     * <p>
     * 将当前工具调用函数包装为一个标准的 FunctionTool，
     * 使其可以被 LLM 发现和调用。
     * </p>
     *
     * @return 封装后的工具对象
     */
    public Tool asTool() {
        return FunctionTool.newBuilder()
                .name("invoke_tool")
                .description("调用指定工具。")
                .parameterType(Invocation.class)
                .function(this)
                .build();
    }

}
