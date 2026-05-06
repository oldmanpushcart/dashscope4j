package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * 工具搜索函数
 * <p>
 * 封装了从工具箱中根据用户意图搜索可用工具的功能。
 * 当 Agent 没有合适的工具完成任务时，可以调用此工具动态查找相关工具。
 * </p>
 * <p>
 * 该函数会被包装为一个 FunctionTool，名为 "search_tools"，
 * 供 LLM 在需要时发现和调用。
 * </p>
 */
class SearchToolsFunction implements Function<SearchToolsFunction.Search, CompletionStage<Map<String, Tool>>> {

    /**
     * 工具箱实例
     */
    private final Toolbox toolbox;

    /**
     * 构造工具搜索函数
     *
     * @param toolbox 工具箱实例
     */
    SearchToolsFunction(Toolbox toolbox) {
        this.toolbox = toolbox;
    }

    /**
     * 执行工具搜索
     * <p>
     * 根据用户提供的意图描述，从工具箱中查找匹配的工具。
     * </p>
     *
     * @param search 搜索参数，包含用户意图描述
     * @return 匹配的工具映射表（工具名 -> 工具实例）
     */
    @Override
    public CompletionStage<Map<String, Tool>> apply(Search search) {
        return toolbox.lookupByIntent(search.intent())
                .thenApply(uses -> uses.stream()
                        .map(ToolUse::tool)
                        .collect(toMap(
                                tool -> tool.meta().name(),
                                Function.identity()
                        )));
    }

    /**
     * 工具搜索参数
     *
     * @param intent 用户意图描述，用于匹配相关工具
     */
    public record Search(

            @JsonPropertyDescription("意图")
            @JsonProperty("intent")
            String intent

    ) {

    }

    /**
     * 转换为 FunctionTool
     * <p>
     * 将当前搜索函数包装为一个标准的 FunctionTool，
     * 使其可以被 LLM 发现和调用。
     * </p>
     *
     * @return 封装后的工具对象
     */
    public Tool asTool() {
        return FunctionTool.newBuilder()
                .name("search_tools")
                .description("根据意图搜索工具。当你没有工具可以完成任务时调用。")
                .parameterType(Search.class)
                .function(this)
                .build();
    }

}
