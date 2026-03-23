package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.HashMap;
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
class McpPromptFunctionTool implements McpFunctionTool {

    private static final TypeReference<HashMap<String, Object>> mapTypeRef = new TypeReference<>() {
    };

    private final McpAsyncClient client;
    private final McpSchema.Prompt mcpPrompt;
    private final Meta meta;

    public McpPromptFunctionTool(String namespace, McpAsyncClient client, McpSchema.Prompt mcpPrompt) {
        this.client = client;
        this.mcpPrompt = mcpPrompt;
        this.meta = newMeta(namespace, mcpPrompt);
    }

    @Override
    public Type type() {
        return Type.PROMPT;
    }

    /**
     * 构建函数元数据
     * <p>
     * 从 Prompt 的定义中提取参数 schema
     * </p>
     *
     * @param namespace 函数命名空间
     * @param mcpPrompt MCP Prompt 定义
     * @return 函数元数据
     */
    private static Meta newMeta(String namespace, McpSchema.Prompt mcpPrompt) {
        // 从 Prompt 的参数列表构建 JSON Schema
        final var parameterSchema = JacksonJsonUtils.toNode(McpSchemaHelper.buildPromptArgumentsSchema(mcpPrompt.arguments()));
        return new Meta(
                "%s$prompt$%s".formatted(namespace, mcpPrompt.name()),
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
        final var argumentMap = JacksonJsonUtils.<Map<String, Object>>toObject(argumentJson, mapTypeRef.getType());

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
                        .map(McpSchema.PromptMessage::content)
                        .filter(content -> content instanceof McpSchema.TextContent)
                        .map(content -> ((McpSchema.TextContent) content).text())
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


}
