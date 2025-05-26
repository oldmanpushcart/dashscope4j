package io.github.oldmanpushcart.dashscope4j.agent.typical.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.util.JsonUtils;
import io.github.oldmanpushcart.dashscope4j.common.util.Buildable;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.concurrent.CompletionStage;

import static io.github.oldmanpushcart.dashscope4j.agent.typical.mcp.McpHelper.isErrorResult;
import static io.github.oldmanpushcart.dashscope4j.agent.typical.mcp.McpHelper.parseResultText;
import static java.util.Objects.requireNonNull;

/**
 * 异步MCP函数工具
 */
@Slf4j
@Accessors(fluent = true)
public class AsyncMcpFunctionTool implements FunctionTool {

    private static final TypeReference<HashMap<String, Object>> mapTypeRef = new TypeReference<>() {

    };

    private final McpAsyncClient mcpClient;
    private final McpSchema.Tool mcpTool;
    private final String _toString;

    @Getter
    private final Meta meta;

    private AsyncMcpFunctionTool(Builder builder) {
        requireNonNull(builder.mcpClient, "McpClient must not be null");
        requireNonNull(builder.mcpTool, "McpSchema.Tool must not be null");
        this.mcpClient = builder.mcpClient;
        this.mcpTool = builder.mcpTool;
        this.meta = newFunctionMeta(builder);
        this._toString = "%s@%s/%s".formatted(
                mcpClient.getServerInfo().name(),
                mcpClient.getServerInfo().version(),
                mcpTool.name()
        );
    }

    private static Meta newFunctionMeta(Builder builder) {
        final var mcpTool = builder.mcpTool;
        return new Meta(
                mcpTool.name(),
                mcpTool.description(),
                JsonUtils.toNode(mcpTool.inputSchema())
        );
    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public CompletionStage<String> call(Caller caller, String argumentsJson) {

        final var argumentsMap = JsonUtils.toObject(argumentsJson, mapTypeRef);
        final var name = mcpTool.name();
        final var request = new McpSchema.CallToolRequest(name, argumentsMap);

        if (log.isDebugEnabled()) {
            log.debug("dashscope-agent://mcp/tool/{} <<< {}", this, JsonUtils.compact(argumentsJson));
        }

        try {
            return mcpClient.callTool(request)
                    .toFuture()
                    .thenApply(result -> {
                        if (isErrorResult(result)) {
                            throw new IllegalStateException("Mcp calling tool: %s failed: %s".formatted(
                                    this,
                                    parseResultText(result)
                            ));
                        }
                        return result;
                    })
                    .thenApply(McpSchema.CallToolResult::content)
                    .thenApply(JsonUtils::toJson)
                    .whenComplete((resultJson, ex) -> {
                        if (log.isDebugEnabled()) {
                            log.debug("dashscope-agent://mcp/tool/{} >>> {}", this, JsonUtils.compact(resultJson), ex);
                        }
                    });
        } catch (Throwable ex) {
            throw new RuntimeException(
                    "Mcp calling tool: %s occur error! arguments=%s".formatted(
                            this,
                            argumentsJson
                    ),
                    ex
            );
        }

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<AsyncMcpFunctionTool, Builder> {

        private McpAsyncClient mcpClient;
        private McpSchema.Tool mcpTool;

        /**
         * 设置异步MCP客户端
         *
         * @param mcpClient 异步MCP客户端
         * @return this
         */
        public Builder mcpClient(McpAsyncClient mcpClient) {
            requireNonNull(mcpClient, "McpClient must not be null");
            this.mcpClient = mcpClient;
            return this;
        }

        /**
         * 设置MCP工具
         *
         * @param mcpTool MCP工具
         * @return this
         */
        public Builder mcpTool(McpSchema.Tool mcpTool) {
            requireNonNull(mcpTool, "McpSchema.Tool must not be null");
            this.mcpTool = mcpTool;
            return this;
        }

        @Override
        public AsyncMcpFunctionTool build() {
            return new AsyncMcpFunctionTool(this);
        }

    }


}
