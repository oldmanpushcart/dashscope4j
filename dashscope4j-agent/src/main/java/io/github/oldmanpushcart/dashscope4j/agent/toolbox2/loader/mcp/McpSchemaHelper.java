package io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * MCP Schema 工具类
 */
class McpSchemaHelper {

    /**
     * 构建 Prompt 参数的 JSON Schema
     *
     * @param arguments Prompt 参数列表
     * @return JSON Schema 节点
     */
    static ArgumentsSchema buildPromptArgumentsSchema(List<McpSchema.PromptArgument> arguments) {
        return new ArgumentsSchema(
                "object",
                arguments != null
                        ? arguments.stream()
                        .collect(Collectors.toMap(
                                McpSchema.PromptArgument::name,
                                arg -> new ArgumentProperty("string", arg.description())
                        ))
                        : Map.of(),
                arguments != null
                        ? arguments.stream()
                        .filter(arg -> Boolean.TRUE.equals(arg.required()))
                        .map(McpSchema.PromptArgument::name)
                        .toList()
                        : List.of()
        );
    }

    /**
     * 构建 Resource 参数的 JSON Schema
     *
     * @param resource Resource 定义
     * @return JSON Schema 节点
     */
    static ArgumentsSchema buildResourceArgumentsSchema(McpSchema.Resource resource) {
        return new ArgumentsSchema(
                "object",
                Map.of(
                        "uri", new ArgumentProperty("string", "Resource URI to read (default: " + resource.uri() + ")")
                ),
                List.of() // uri 是可选的
        );
    }

    /**
     * 参数属性定义
     */
    record ArgumentProperty(
            @JsonProperty("type")
            String type,

            @JsonProperty("description")
            String description
    ) {
    }

    /**
     * 参数 JSON Schema 表示
     */
    record ArgumentsSchema(
            @JsonProperty("type")
            String type,

            @JsonProperty("properties")
            Map<String, ArgumentProperty> properties,

            @JsonProperty("required")
            List<String> required
    ) {
    }
}
