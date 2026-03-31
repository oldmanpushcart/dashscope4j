package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class NewMcpToolLoader implements Repository.Loader<String, Tool> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String name;
    private final McpClientTransport transport;
    private final Duration syncInterval;
    private boolean blocking;
    private final String _toString;

    private final CompletableFuture<Void> closeF = new CompletableFuture<>();
    private McpAsyncClient mcpClient;

    private NewMcpToolLoader(Builder builder) {
        this.name = builder.name;
        this.transport = builder.transport;
        this.syncInterval = builder.syncInterval;
        this.blocking = builder.blocking;
        this._toString = "dashscope4j-agent:/repo/tool/loader/mcp/%s".formatted(name);
    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public CompletionStage<Void> init(Repository.Updater<String, Tool> updater) {
        return null;
    }

    private CompletionStage<Map<String, FunctionTool>> flush() {
        final var flushTools = new ConcurrentHashMap<String, FunctionTool>();
        final var capabilities = mcpClient.getServerCapabilities();

        if(null != capabilities.tools()) {

        }

    }

    @Override
    public void close() throws Exception {

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<NewMcpToolLoader, Builder> {

        private String name;
        private McpClientTransport transport;
        private Duration syncInterval;
        private boolean blocking;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder transport(McpClientTransport transport) {
            this.transport = transport;
            return this;
        }

        public Builder syncInterval(Duration syncInterval) {
            this.syncInterval = syncInterval;
            return this;
        }

        public Builder blocking(boolean blocking) {
            this.blocking = blocking;
            return this;
        }

        @Override
        public NewMcpToolLoader build() {
            return null;
        }

    }

}
