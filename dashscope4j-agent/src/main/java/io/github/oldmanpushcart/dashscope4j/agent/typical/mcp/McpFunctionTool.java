package io.github.oldmanpushcart.dashscope4j.agent.typical.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.util.JsonUtils;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.agent.typical.mcp.McpHelper.isErrorResult;
import static io.github.oldmanpushcart.dashscope4j.agent.typical.mcp.McpHelper.parseResultText;
import static io.github.oldmanpushcart.dashscope4j.client.util.JsonUtils.compact;

/**
 * 异步MCP函数工具
 */
@Slf4j
@Accessors(fluent = true)
class McpFunctionTool implements FunctionTool {

    private static final TypeReference<HashMap<String, Object>> mapTypeRef = new TypeReference<>() {

    };

    private final McpChatAgent agent;
    private final McpSchema.Tool mcpTool;
    private final Meta meta;

    public McpFunctionTool(McpChatAgent agent, McpSchema.Tool mcpTool) {
        this.agent = agent;
        this.mcpTool = mcpTool;
        this.meta = newFunctionMeta(mcpTool);
    }

    private static Meta newFunctionMeta(McpSchema.Tool mcpTool) {
        return new Meta(
                mcpTool.name(),
                mcpTool.description(),
                JsonUtils.toNode(mcpTool.inputSchema())
        );
    }

    @Override
    public CompletionStage<String> call(Caller caller, String argumentsJson) {

        final var argumentsMap = JsonUtils.toObject(argumentsJson, mapTypeRef);
        final var name = mcpTool.name();
        final var request = new McpSchema.CallToolRequest(name, argumentsMap);

        return agent.fetch().thenCompose(client -> {

            final var serverInfo = client.getServerInfo();
            final var prefix = "%s@%s/%s".formatted(serverInfo.name(), serverInfo.version(), mcpTool.name());

            if (log.isDebugEnabled()) {
                log.debug("dashscope-agent://mcp/tool/{} <<< {}", prefix, compact(argumentsJson));
            }

            return client.callTool(request)
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
                    .thenApply(JsonUtils::toJson)
                    .whenComplete((resultJson, ex) -> {
                        if (log.isDebugEnabled()) {
                            log.debug("dashscope-agent://mcp/tool/{} >>> {}", prefix, compact(resultJson), ex);
                        }
                    });

        });

    }

    @Override
    public Meta meta() {
        return meta;
    }

}
