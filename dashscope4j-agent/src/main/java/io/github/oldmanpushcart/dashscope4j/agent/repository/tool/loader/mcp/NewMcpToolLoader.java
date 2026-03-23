package io.github.oldmanpushcart.dashscope4j.agent.repository.tool.loader.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.repository.Repository;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class NewMcpToolLoader implements Repository.Loader<String, Tool> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String name;
    private final McpClientTransport transport;
    private final String _toString;

    private final CompletableFuture<Void> closeF = new CompletableFuture<>();
    private final ReentrantLock syncerLock = new ReentrantLock();
    private final Condition syncerCondition = syncerLock.newCondition();
    private volatile boolean synced = false;

    private final List<McpFunctionTool> activeTools = new CopyOnWriteArrayList<>();
    private final List<McpFunctionTool> stagedTools = new CopyOnWriteArrayList<>();

    private volatile McpAsyncClient mcpClient;
    private volatile Repository.Updater<String, Tool> updater;

    public NewMcpToolLoader(Builder builder) {
        this.name = "mcp$" + builder.name;
        this.transport = builder.transport;
        this._toString = "dashscope4j-agent:/tool/loader/mcp/%s".formatted(this.name);
    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public CompletionStage<Void> init(Repository.Updater<String, Tool> updater) {
        this.updater = updater;
        this.mcpClient = McpClient.async(transport)
                .toolsChangeConsumer(mcpTools -> {
                    stagingMcpTools(mcpTools);
                    wakeupSyncer();
                    return Mono.empty();
                })
                .promptsChangeConsumer(mcpPrompts -> {
                    stagingMcpPrompts(mcpPrompts);
                    wakeupSyncer();
                    return Mono.empty();
                })
                .resourcesChangeConsumer(mcpResources -> {
                    stagingMcpResources(mcpResources);
                    wakeupSyncer();
                    return Mono.empty();
                })
                .build();
        return this.mcpClient.initialize()
                .toFuture()
                .thenCompose(result -> {
                    final var capabilities = mcpClient.getServerCapabilities();
                    if (null == capabilities) {
                        return CompletableFuture.completedStage(null);
                    }
                    return CompletableFuture.completedStage(null)
                            .thenCompose(unused -> {
                                if (capabilities.tools() == null) {
                                    return CompletableFuture.completedStage(null);
                                }
                                return mcpClient.listTools()
                                        .toFuture()
                                        .thenAccept(r -> {
                                            if (null != r) {
                                                stagingMcpTools(r.tools());
                                            }
                                        });
                            })
                            .thenCompose(unused -> {
                                if (capabilities.prompts() == null) {
                                    return CompletableFuture.completedStage(null);
                                }
                                return mcpClient.listPrompts()
                                        .toFuture()
                                        .thenAccept(r -> {
                                            if (null != r) {
                                                stagingMcpPrompts(r.prompts());
                                            }
                                        });
                            })
                            .thenCompose(unused -> {
                                if (capabilities.resources() == null) {
                                    return CompletableFuture.completedStage(null);
                                }
                                return mcpClient.listResources()
                                        .toFuture()
                                        .thenAccept(r -> {
                                            if (null != r) {
                                                stagingMcpResources(r.resources());
                                            }
                                        });
                            })
                            .thenAccept(unused -> wakeupSyncer());
                });
    }

    private void stagingMcpTools(List<McpSchema.Tool> mcpTools) {
        final var tools = mcpTools.stream()
                .map(mcpTool -> new McpToolFunctionTool(mcpClient, mcpTool))
                .toList();
        stagedTools.removeIf(tool -> tool.type() == McpFunctionTool.Type.TOOL);
        stagedTools.addAll(tools);
    }

    private void stagingMcpPrompts(List<McpSchema.Prompt> mcpPrompts) {
        final var prompts = mcpPrompts.stream()
                .map(mcpPrompt -> new McpPromptFunctionTool(mcpClient, mcpPrompt))
                .toList();
        stagedTools.removeIf(tool -> tool.type() == McpFunctionTool.Type.PROMPT);
        stagedTools.addAll(prompts);
    }

    private void stagingMcpResources(List<McpSchema.Resource> mcpResources) {
        final var resources = mcpResources.stream()
                .map(mcpResource -> new McpResourceFunctionTool(mcpClient, mcpResource))
                .toList();
        stagedTools.removeIf(tool -> tool.type() == McpFunctionTool.Type.RESOURCE);
        stagedTools.addAll(resources);
    }

    private void wakeupSyncer() {
        if (syncerLock.tryLock()) {
            try {
                synced = false;
                syncerCondition.signal();
            } finally {
                syncerLock.unlock();
            }
        }
    }

    private void syncing() {
        while (!closeF.isDone() && !Thread.currentThread().isInterrupted()) {

            /*
             * 休眠，等待唤醒或者自己唤醒
             */
            syncerLock.lock();
            try {

                //noinspection ResultOfMethodCallIgnored
                syncerCondition.await(1, TimeUnit.SECONDS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                syncerLock.unlock();
            }

            // 醒来先检查环境是否还存在，如果已关闭则自杀
            if (closeF.isDone()) {
                break;
            }

            // 检查同步标志，如果已同步则跳过本次周期
            if (synced) {
                continue;
            }

            // 开始同步作业


            // 同步完成，标记已同步
            syncerLock.lock();
            try {
                synced = true;
            } finally {
                syncerLock.unlock();
            }

        }
    }

    @Override
    public void close() throws Exception {
        if (null != mcpClient) {
            try {
                mcpClient.close();
            } catch (Throwable ex) {
                // ignored.
            }
        }
    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<NewMcpToolLoader, Builder> {

        private String name;
        private McpClientTransport transport;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder transport(McpClientTransport transport) {
            this.transport = transport;
            return this;
        }

        @Override
        public NewMcpToolLoader build() {
            return new NewMcpToolLoader(this);
        }
    }

}
