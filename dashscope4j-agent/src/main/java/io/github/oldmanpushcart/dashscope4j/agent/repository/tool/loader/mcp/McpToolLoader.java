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

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils.requireNonBlankString;
import static java.util.Objects.requireNonNull;

public class McpToolLoader implements Repository.Loader<String, Tool> {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String namespace;
    private final McpClientTransport transport;
    private final Duration syncInterval;
    private final String _toString;

    private final CompletableFuture<Void> closeF = new CompletableFuture<>();
    private final ReentrantLock syncerLock = new ReentrantLock();
    private final Condition syncerCondition = syncerLock.newCondition();

    // 生效工具集合
    private final Map<String, McpFunctionTool> activeTools = new ConcurrentHashMap<>();

    // 生效版本
    private final AtomicInteger activeVersion = new AtomicInteger();

    // 工具草稿集合
    private final Map<String, McpFunctionTool> stagedTools = new ConcurrentHashMap<>();

    // 草稿版本
    private final AtomicInteger stagedVersion = new AtomicInteger();

    // 同步线程
    private final Thread syncer;

    private volatile McpAsyncClient mcpClient;
    private volatile Repository.Updater<String, Tool> updater;

    public McpToolLoader(Builder builder) {

        requireNonBlankString(builder.name, "name must not be blank");
        requireNonNull(builder.transport, "transport must not be null");
        requireNonNull(builder.syncInterval, "syncInterval must not be null");

        this.namespace = "mcp$" + builder.name;
        this.transport = builder.transport;
        this.syncInterval = builder.syncInterval;
        this._toString = "dashscope4j-agent:/tool/loader/mcp/%s".formatted(builder.name);
        this.syncer = new Thread(this::syncing, "%s/syncer".formatted(this));

    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public CompletionStage<Void> init(Repository.Updater<String, Tool> updater) {

        this.updater = updater;

        // 启动同步器
        this.syncer.setDaemon(true);
        this.syncer.start();

        /*
         * 初始化 McpClient
         *
         * 1. 需要动态变更
         * 2. 初始化时全量同步所有工具
         */
        this.mcpClient = McpClient.async(transport)
                .toolsChangeConsumer(mcpTools -> {
                    stagingMcpTools(mcpTools);
                    notifySyncer();
                    return Mono.empty();
                })
                .promptsChangeConsumer(mcpPrompts -> {
                    stagingMcpPrompts(mcpPrompts);
                    notifySyncer();
                    return Mono.empty();
                })
                .resourcesChangeConsumer(mcpResources -> {
                    stagingMcpResources(mcpResources);
                    notifySyncer();
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
                            .thenAccept(unused -> notifySyncer());
                });
    }

    /**
     * 将 McpClient Tool 存入草稿集合中
     *
     * @param mcpTools McpClient Tools
     */
    private void stagingMcpTools(List<McpSchema.Tool> mcpTools) {
        final var tools = mcpTools.stream()
                .map(mcpTool -> new McpToolFunctionTool(namespace, mcpClient, mcpTool))
                .collect(Collectors.toMap(
                        tool -> tool.meta().name(),
                        tool -> tool
                ));
        final var removeIt = stagedTools.entrySet().iterator();
        while (removeIt.hasNext()) {
            final var entry = removeIt.next();
            final var tool = entry.getValue();
            if (tool.type() == McpFunctionTool.Type.TOOL) {
                removeIt.remove();
            }
        }
        stagedTools.putAll(tools);
    }

    /**
     * 将 McpClient Prompt 存入草稿集合中
     *
     * @param mcpPrompts McpClient Prompts
     */
    private void stagingMcpPrompts(List<McpSchema.Prompt> mcpPrompts) {
        final var prompts = mcpPrompts.stream()
                .map(mcpPrompt -> new McpPromptFunctionTool(namespace, mcpClient, mcpPrompt))
                .collect(Collectors.toMap(
                        tool -> tool.meta().name(),
                        tool -> tool
                ));
        final var removeIt = stagedTools.entrySet().iterator();
        while (removeIt.hasNext()) {
            final var entry = removeIt.next();
            final var tool = entry.getValue();
            if (tool.type() == McpFunctionTool.Type.PROMPT) {
                removeIt.remove();
            }
        }
        stagedTools.putAll(prompts);
    }

    /**
     * 将 McpClient Resource 存入草稿集合中
     *
     * @param mcpResources McpClient Resources
     */
    private void stagingMcpResources(List<McpSchema.Resource> mcpResources) {
        final var resources = mcpResources.stream()
                .map(mcpResource -> new McpResourceFunctionTool(namespace, mcpClient, mcpResource))
                .collect(Collectors.toMap(
                        tool -> tool.meta().name(),
                        tool -> tool
                ));
        final var removeIt = stagedTools.entrySet().iterator();
        while (removeIt.hasNext()) {
            final var entry = removeIt.next();
            final var tool = entry.getValue();
            if (tool.type() == McpFunctionTool.Type.RESOURCE) {
                removeIt.remove();
            }
        }
        stagedTools.putAll(resources);
    }

    /**
     * 唤醒同步器
     */
    private void notifySyncer() {
        if (syncerLock.tryLock()) {
            try {
                stagedVersion.incrementAndGet();
                syncerCondition.signal();
            } finally {
                syncerLock.unlock();
            }
        }
    }


    /**
     * 同步作业
     */
    private void syncing() {

        logger.debug("{}/syncer running...", this);
        while (!closeF.isDone() && !Thread.currentThread().isInterrupted()) {

            /*
             * 休眠，等待唤醒或者自己唤醒
             */
            syncerLock.lock();
            try {

                //noinspection ResultOfMethodCallIgnored
                syncerCondition.await(syncInterval.toMillis(), TimeUnit.MILLISECONDS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                syncerLock.unlock();
            }

            logger.debug("{}/syncer wakeup.", this);

            // 醒来先检查环境是否还存在，如果已关闭则自杀
            if (closeF.isDone()) {
                break;
            }

            try {

                do {

                    /*
                     * 通过比对发布版本号和草稿版本号是否一致来判定草稿是否有修改。
                     * 若修改则进入同步作业，
                     * 否则进入休眠等待下一轮同步周期。
                     */
                    final var currentVersion = stagedVersion.get();
                    if (activeVersion.get() == currentVersion) {
                        logger.debug("{}/syncer nothing synced.", this);
                        break;
                    }

                    // 开始同步作业
                    {

                        /*
                         * STEP1：clone一个稳定副本。
                         * 接下来的比对都非常依赖一个稳定的集合作为基线
                         */
                        final var cloneStagedTools = new HashMap<>(stagedTools);

                        // STEP2：清理被删除的工具
                        final var cleanupNameSet = activeTools.keySet().stream()
                                .filter(name -> !cloneStagedTools.containsKey(name))
                                .collect(Collectors.toSet());
                        cleanupNameSet.forEach(name -> {
                            updater.remove(name).toCompletableFuture().join();
                            activeTools.remove(name);
                            logger.debug("{} remove tool: {}", this, name);
                        });

                        // STEP3：更新有变动的工具（包括新增）
                        cloneStagedTools.forEach((name, tool) -> {

                            // 检测是否有变动，如有变动立即同步
                            final var activeTool = activeTools.get(name);
                            if (activeTool == null || tool.meta().equals(activeTool.meta())) {
                                updater.upsert(name, tool);
                                activeTools.put(name, tool);
                                logger.debug("{} upsert tool: {}", this, name);
                            }

                        });

                    }

                    // 同步完成，标记已同步
                    activeVersion.set(currentVersion);
                    logger.debug("{}/syncer sync completed.", this);

                } while (true);

            } catch (Throwable ex) {
                logger.warn("{}/syncer sync failed!", this, ex);
            }

        }
        logger.debug("{}/syncer stopped.", this);
    }

    @Override
    public void close() {
        syncer.interrupt();
        if (null != mcpClient) {
            try {
                mcpClient.close();
            } catch (Throwable ex) {
                // ignored.
            }
        }
    }

    /**
     * @return 构建器
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 构建器
     */
    public static class Builder implements Buildable<McpToolLoader, Builder> {

        private String name;
        private McpClientTransport transport;
        private Duration syncInterval = Duration.ofSeconds(10);

        /**
         * 设置名称
         *
         * @return this
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置 Mcp Transport
         *
         * @param transport Mcp Transport
         * @return this
         */
        public Builder transport(McpClientTransport transport) {
            this.transport = transport;
            return this;
        }

        /**
         * 设置同步间隔
         *
         * @param syncInterval 同步间隔
         * @return this
         */
        public Builder syncInterval(Duration syncInterval) {
            this.syncInterval = syncInterval;
            return this;
        }

        @Override
        public McpToolLoader build() {
            return new McpToolLoader(this);
        }

    }

}
