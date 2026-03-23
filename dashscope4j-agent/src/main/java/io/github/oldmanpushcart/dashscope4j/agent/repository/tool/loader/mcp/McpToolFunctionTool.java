package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

class McpToolFunctionTool implements McpFunctionTool {

    private static final TypeReference<HashMap<String, Object>> mapType = new TypeReference<>() {
    };

    private final McpAsyncClient mcpClient;
    private final McpSchema.Tool mcpTool;
    private final Meta meta;

    public McpToolFunctionTool(String namespace, McpAsyncClient mcpClient, McpSchema.Tool mcpTool) {
        this.mcpClient = mcpClient;
        this.mcpTool = mcpTool;
        this.meta = new Meta(
                "%s$tool$%s".formatted(namespace, mcpTool.name()),
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

    @Override
    public CompletionStage<String> call(Caller caller, String argumentJson) {

        final var serverInfo = mcpClient.getServerInfo();
        final var prefix = "%s@%s/%s".formatted(serverInfo.name(), serverInfo.version(), mcpTool.name());

        final var argumentMap = JacksonJsonUtils.<Map<String, Object>>toObject(argumentJson, mapType.getType());
        final var name = mcpTool.name();
        final var request = new McpSchema.CallToolRequest(name, argumentMap);

        return mcpClient.callTool(request)
                .toFuture()
                .thenApply(result -> {
                    if (isErrorResult(result)) {
                        throw new IllegalStateException("Calling tool: /%s failed: %s".formatted(
                                prefix,
                                parseResultText(result)
                        ));
                    }
                    return result;
                })
                .thenApply(McpSchema.CallToolResult::content)
                .thenApply(JacksonJsonUtils::toJson);
    }

    /**
     * 判断结果是否为错误
     *
     * @param result 结果
     * @return TRUE | FALSE
     */
    private static boolean isErrorResult(McpSchema.CallToolResult result) {
        return null != result
                && null != result.isError()
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
