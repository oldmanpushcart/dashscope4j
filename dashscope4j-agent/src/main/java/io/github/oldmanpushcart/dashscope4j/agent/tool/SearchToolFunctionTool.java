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

    private CompletionStage<List<Tool>> query(Query query) {
        return registry.routing(query.intent());
    }

    private record Query(

            @JsonProperty("intent")
            String intent

    ) {

    }

    public Tool toTool() {
        return FunctionTool.newBuilder()
                .name("search_tools")
                .description("根据意图搜索可以使用的工具")
                .parameterType(Query.class)
                .function(this::query)
                .build();
    }

}
