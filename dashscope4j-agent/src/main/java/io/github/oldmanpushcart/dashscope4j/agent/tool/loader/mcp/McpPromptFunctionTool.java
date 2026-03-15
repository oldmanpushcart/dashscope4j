package io.github.oldmanpushcart.dashscope4j.agent.tool.loader.mcp;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * MCP Prompt 函数工具
 * <p>
 * 将 MCP 的 Prompt 包装为 FunctionTool，调用时会：
 * 1. 接收用户输入的参数
 * 2. 调用 MCP 服务器的 getPrompt API 获取渲染后的 prompt
 * 3. 直接返回渲染后的文本内容
 * </p>
 */
class McpPromptFunctionTool implements FunctionTool {

    private static final Type mapType = new TypeReference<HashMap<String, Object>>() {
    }.getType();

    private final McpAsyncClient client;
    private final McpSchema.Prompt mcpPrompt;
    private final Meta meta;

    public McpPromptFunctionTool(McpAsyncClient client, McpSchema.Prompt mcpPrompt) {
        this.client = client;
        this.mcpPrompt = mcpPrompt;
        this.meta = newMeta(mcpPrompt);
    }

    /**
     * 构建函数元数据
     * <p>
     * 从 Prompt 的定义中提取参数 schema
     * </p>
     *
     * @param mcpPrompt MCP Prompt 定义
     * @return 函数元数据
     */
    private static Meta newMeta(McpSchema.Prompt mcpPrompt) {
        // 从 Prompt 的参数列表构建 JSON Schema
        final var parameterSchema = JacksonJsonUtils.toNode(new PromptArgumentsSchema(mcpPrompt.arguments()));
        return new Meta(
                mcpPrompt.name(),
                mcpPrompt.description() != null ? mcpPrompt.description() : "MCP Prompt: " + mcpPrompt.name(),
                parameterSchema
        );
    }

    @Override
    public Meta meta() {
        return meta;
    }

    @Override
    public CompletionStage<String> call(Caller caller, String argumentJson) {
        final var serverInfo = client.getServerInfo();
        final var prefix = "%s@%s/%s".formatted(serverInfo.name(), serverInfo.version(), mcpPrompt.name());

        // 解析用户输入的参数
        final var argumentMap = JacksonJsonUtils.<Map<String, Object>>toObject(argumentJson, mapType);

        // 调用 MCP 服务器的 getPrompt API
        final var request = new McpSchema.GetPromptRequest(mcpPrompt.name(), argumentMap);

        return client.getPrompt(request)
                .toFuture()
                .thenApply(result -> {
                    // 检查是否有错误
                    if (isErrorResult(result)) {
                        throw new IllegalStateException("Getting prompt: /%s failed: %s".formatted(
                                prefix,
                                parseResultText(result)
                        ));
                    }
                    return result;
                })
                // 提取并拼接 prompt 消息内容
                .thenApply(McpSchema.GetPromptResult::messages)
                .thenApply(messages -> messages.stream()
                        .map(promptMessage -> {
                            // PromptMessage 包含 content 字段
                            final var content = promptMessage.content();
                            // Content 可能是多种类型，这里处理 TextContent
                            if (content instanceof McpSchema.TextContent textContent) {
                                return textContent.text();
                            }
                            return null;
                        })
                        .filter(text -> text != null && !text.isEmpty())
                        .collect(Collectors.joining("\n")));
    }

    /**
     * 判断结果是否为错误
     *
     * @param result 结果
     * @return TRUE | FALSE
     */
    private static boolean isErrorResult(McpSchema.GetPromptResult result) {
        // GetPromptResult 可能没有 isError 方法，需要根据实际 API 调整
        // 这里假设如果 messages 为空则表示出错
        return null == result || null == result.messages() || result.messages().isEmpty();
    }

    /**
     * 解析结果中的文本
     *
     * @param result 结果
     * @return 文本
     */
    private static String parseResultText(McpSchema.GetPromptResult result) {
        if (null == result || null == result.messages()) {
            return "Unknown error";
        }
        return result.messages().stream()
                .map(McpSchema.PromptMessage::content)
                .filter(content -> content instanceof McpSchema.TextContent)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .collect(Collectors.joining("\n"));
    }

    /**
     * Prompt 参数的 JSON Schema 表示
     */
    private record PromptArgumentsSchema(

            @JsonProperty("type")
            String type,

            @JsonProperty("properties")
            Map<String, ArgumentProperty> properties,

            @JsonProperty("required")
            List<String> required

    ) {
        public PromptArgumentsSchema(List<McpSchema.PromptArgument> arguments) {
            this(
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
    }

    /**
     * 参数属性定义
     */
    private record ArgumentProperty(
            @JsonProperty("type")
            String type,

            @JsonProperty("description")
            String description
    ) {
    }
}
