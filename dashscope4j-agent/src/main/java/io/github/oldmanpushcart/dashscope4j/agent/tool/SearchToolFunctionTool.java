package io.github.oldmanpushcart.dashscope4j.agent.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class SearchToolFunctionTool {

    private final ToolRegistry registry;

    public SearchToolFunctionTool(ToolRegistry registry) {
        this.registry = registry;
    }

    private CompletionStage<List<Tool>> query(Tool.Caller caller, Query query) {
        return registry.routing(query.intent())
                .whenComplete((tools, ex) -> {
                    if (null != tools) {
                        final var context = caller.request().context();
                        context.put("DYNAMIC_TOOLS", tools);
                    }
                });
    }

    private record Query(

            @JsonProperty("intent")
            String intent

    ) {

    }

    public Tool toTool() {
        return FunctionTool.newBuilder()
                .name("search_tools")
                .description("当你手头上没有合适的工具时，可以根据意图找到可以解决你问题的工具。")
                .parameterType(Query.class)
                .function(this::query)
                .build();
    }

}
