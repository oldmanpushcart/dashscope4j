package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * MCP 工具函数封装
 * <p>
 * 将 MCP 服务器提供的工具封装为标准的 FunctionTool，
 * 使其可以被 Agent 调用和执行。
 * </p>
 */
class McpToolFunctionTool implements McpFunctionTool {

    /**
     * Map 类型引用，用于 JSON 反序列化
     */
    private static final TypeReference<HashMap<String, Object>> mapType = new TypeReference<>() {
    };

    /**
     * MCP 异步客户端
     */
    private final McpAsyncClient mcpClient;

    /**
     * MCP 工具定义
     */
    private final McpSchema.Tool mcpTool;

    /**
     * 工具元数据
     */
    private final Meta meta;

    /**
     * 构造 MCP 工具函数
     *
     * @param namespace 命名空间，用于生成工具名称前缀
     * @param mcpClient MCP 异步客户端
     * @param mcpTool   MCP 工具定义
     */
    public McpToolFunctionTool(String namespace, McpAsyncClient mcpClient, McpSchema.Tool mcpTool) {
        this.mcpClient = mcpClient;
        this.mcpTool = mcpTool;
        // 生成工具元数据，名称格式：namespace$tool$toolName
        this.meta = new Meta(
                "mcp$%s$tool$%s".formatted(namespace, mcpTool.name()),
                mcpTool.description(),
                JacksonJsonUtils.toNode(mcpTool.inputSchema())
        );
    }

    @Override
    public Type type() {
        return Type.TOOL;
    }

    @Override
    public Meta meta() {
        return meta;
    }

    /**
     * 调用 MCP 工具
     * <p>
     * 将 JSON 参数转换为 Map，调用 MCP 服务器的工具接口，
     * 并将结果转换回 JSON 字符串返回。
     * </p>
     *
     * @param caller       调用者上下文
     * @param argumentJson 参数的 JSON 字符串
     * @return 工具执行结果的 JSON 字符串
     * @throws IllegalStateException 如果工具调用失败
     */
    @Override
    public CompletionStage<String> call(Caller caller, String argumentJson) {

        final var serverInfo = mcpClient.getServerInfo();
        // 构建工具调用前缀，用于错误信息展示
        final var prefix = "%s@%s/%s".formatted(serverInfo.name(), serverInfo.version(), mcpTool.name());

        // 将 JSON 参数转换为 Map
        final var argumentMap = JacksonJsonUtils.<Map<String, Object>>toObject(argumentJson, mapType.getType());
        final var name = mcpTool.name();


        final var request = McpSchema.CallToolRequest.builder(name)
                .arguments(argumentMap)
                .build();

        return mcpClient.callTool(request)
                .toFuture()
                .thenApply(result -> {

                    if (null == result) {
                        throw new IllegalStateException("Calling too: /%s failed: result is null!".formatted(
                                prefix
                        ));
                    }

                    // 检查是否为错误结果
                    if (isErrorResult(result)) {
                        throw new IllegalStateException("Calling tool: /%s failed: %s".formatted(
                                prefix,
                                parseResultText(result)
                        ));
                    }
                    return result;
                })
                // 提取内容并转换为 JSON
                .thenApply(callToolResult -> Objects.requireNonNull(callToolResult).content())
                .thenApply(JacksonJsonUtils::toJson);
    }

    /**
     * 判断结果是否为错误
     *
     * @param result 结果
     * @return TRUE | FALSE
     */
    private static boolean isErrorResult(McpSchema.CallToolResult result) {
        return null != result.isError()
                && result.isError();
    }

    /**
     * 解析结果中的文本
     *
     * @param result 结果
     * @return 文本
     */
    private static String parseResultText(McpSchema.CallToolResult result) {
        return result.content().stream()
                .filter(McpSchema.TextContent.class::isInstance)
                .map(McpSchema.TextContent.class::cast)
                .map(McpSchema.TextContent::text)
                .collect(Collectors.joining("\n"));
    }

}
