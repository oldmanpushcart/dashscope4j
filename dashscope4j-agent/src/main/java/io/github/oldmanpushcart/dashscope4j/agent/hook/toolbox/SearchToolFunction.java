package io.github.oldmanpushcart.dashscope4j.agent.hook.toolbox;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * 工具搜索函数
 */
class SearchToolFunction
        implements Function<SearchToolFunction.Search, CompletionStage<Object>> {

    private final Toolbox toolbox;

    public SearchToolFunction(Toolbox toolbox) {
        this.toolbox = toolbox;
    }

    @Override
    public CompletionStage<Object> apply(Search search) {
        return toolbox.lookupByIntent(search.intent())
                .thenApply(tools -> Map.of(
                        "tools", tools,
                        "suggest", """
                                匹配到的工具列表不能直接使用，必须通过`invoke_tool`工具进行调用。
                                """
                ));
    }

    /**
     * 工具搜索参数
     *
     * @param intent 用户意图描述，用于匹配相关工具
     */
    public record Search(

            @JsonPropertyDescription("意图")
            @JsonProperty(value = "intent", required = true)
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
                .name("search_tool")
                .description("根据意图搜索工具。当你没有工具可以完成任务时调用。")
                .parameterType(Search.class)
                .function(this)
                .build();
    }

}
