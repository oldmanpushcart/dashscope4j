package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.mcp;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.Toolbox;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * MCP (Model Context Protocol) 工具加载器
 * <p>
 * 从 MCP 服务器动态加载工具、提示词和资源，并定期同步更新。
 * 支持的功能包括：
 * <ul>
 *     <li><b>工具加载</b>：从 MCP 服务器获取可用的工具列表</li>
 *     <li><b>提示词加载</b>：加载 MCP 服务器提供的提示词模板</li>
 *     <li><b>资源加载</b>：加载 MCP 服务器管理的资源</li>
 *     <li><b>自动同步</b>：后台线程定期同步工具变更（新增、修改、删除）</li>
 * </ul>
 * </p>
 * <p>
 * 所有从 MCP 加载的工具名称都会加上前缀 {@code "mcp$" + name}，以避免命名冲突。
 * </p>
 *
 * @see ToolLoader
 * @see McpAsyncClient
 */
public class McpToolLoader implements ToolLoader {

    /**
     * 日志记录器
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    /**
     * MCP 加载器名称，用于标识和工具名前缀
     */
    private final String name;
    
    /**
     * MCP 客户端传输层
     */
    private final McpClientTransport transport;
    
    /**
     * 同步间隔时间
     */
    private final Duration syncInterval;
    
    /**
     * 字符串表示形式
     */
    private final String _toString;

    /**
     * 同步线程，负责定期从 MCP 服务器同步工具
     */
    private final Thread syncer;
    
    /**
     * 已加载的工具缓存（线程安全）
     * key: 工具名称
     * value: 函数工具实例
     */
    private final Map<String, FunctionTool> tools = new ConcurrentHashMap<>();

    // --- 生命周期控制 ---
    
    /**
     * 关闭信号
     */
    private final CompletableFuture<Void> closeF = new CompletableFuture<>();
    
    /**
     * 安装信号
     */
    private final CompletableFuture<Void> installF = new CompletableFuture<>();
    
    /**
     * 工具箱实例
     */
    private volatile Toolbox toolbox;
    
    /**
     * MCP 异步客户端
     */
    private volatile McpAsyncClient mcpClient;

    /**
     * 构造 MCP 工具加载器
     *
     * @param builder 构建器
     */
    private McpToolLoader(Builder builder) {

        this.name = builder.name;
        this.transport = builder.transport;
        this.syncInterval = builder.syncInterval;

        this._toString = "dashscope4j-agent:/toolbox/loader/mcp/%s".formatted(name);
        // 创建守护线程，用于定期同步工具
        this.syncer = new Thread(this::sync, _toString);
        this.syncer.setDaemon(true);

    }

    @Override
    public String toString() {
        return _toString;
    }

    /**
     * 安装到工具箱
     * <p>
     * 初始化 MCP 客户端，加载初始工具集，并启动后台同步线程。
     * 该方法只能调用一次，重复调用会抛出异常。
     * </p>
     *
     * @param toolbox 目标工具箱
     * @return 安装完成的 CompletionStage
     * @throws IllegalStateException 如果已经关闭或已经安装
     */
    @Override
    public CompletionStage<Void> install(Toolbox toolbox) {

        if (closeF.isDone()) {
            throw new IllegalStateException("Already closed!");
        }

        if (!installF.complete(null)) {
            throw new IllegalStateException("Already installed!");
        }

        this.toolbox = toolbox;
        this.mcpClient = McpClient.async(transport).build();
        // 初始化 MCP 客户端 -> 同步工具 -> 启动同步线程
        return mcpClient.initialize().toFuture()
                .thenCompose(unused -> syncTools())
                .thenAccept(unused -> syncer.start());
    }

    /**
     * 刷新工具列表
     * <p>
     * 从 MCP 服务器并行获取工具、提示词和资源列表，
     * 并将它们转换为 FunctionTool 实例。
     * </p>
     *
     * @return 刷新后的工具映射表
     */
    private CompletionStage<Map<String, FunctionTool>> flushTools() {
        final var stages = new ArrayList<CompletionStage<Void>>();
        final var flushTools = new ConcurrentHashMap<String, FunctionTool>();
        final var capabilities = mcpClient.getServerCapabilities();

        // 如果服务器支持工具，则加载工具列表
        if (null != capabilities.tools()) {
            final var stage = mcpClient.listTools().toFuture()
                    .thenAccept(result -> {
                        if (null != result && null != result.tools()) {
                            result.tools().stream()
                                    .map(mcpTool -> new McpToolFunctionTool("mcp$" + name, mcpClient, mcpTool))
                                    .forEach(tool -> flushTools.put(tool.meta().name(), tool));
                        }
                    });
            stages.add(stage);
        }

        // 如果服务器支持提示词，则加载提示词列表
        if (null != capabilities.prompts()) {
            final var stage = mcpClient.listPrompts().toFuture()
                    .thenAccept(result -> {
                        if (null != result && null != result.prompts()) {
                            result.prompts().stream()
                                    .map(mcpPrompt -> new McpPromptFunctionTool("mcp$" + name, mcpClient, mcpPrompt))
                                    .forEach(tool -> flushTools.put(tool.meta().name(), tool));
                        }
                    });
            stages.add(stage);
        }

        // 如果服务器支持资源，则加载资源列表
        if (null != capabilities.resources()) {
            final var stage = mcpClient.listResources().toFuture()
                    .thenAccept(result -> {
                        if (null != result && null != result.resources()) {
                            result.resources().stream()
                                    .map(mcpResource -> new McpResourceFunctionTool("mcp$" + name, mcpClient, mcpResource))
                                    .forEach(tool -> flushTools.put(tool.meta().name(), tool));
                        }
                    });
            stages.add(stage);
        }

        return CompletableFutureUtils.allOf(stages)
                .thenApply(unused -> flushTools);
    }

    /**
     * 同步工具
     * <p>
     * 对比当前工具集和 MCP 服务器的最新工具集，执行增量更新：
     * <ul>
     *     <li>删除已从服务器移除的工具</li>
     *     <li>新增或更新已变更的工具</li>
     * </ul>
     * </p>
     *
     * @return 同步完成的 CompletionStage
     */
    private CompletionStage<Void> syncTools() {
        return flushTools().thenCompose(flushTools -> {

            // 找出已经被删除的工具
            final var removeNames = tools.keySet()
                    .stream()
                    .filter(name -> !flushTools.containsKey(name))
                    .collect(Collectors.toSet());

            // 找出变更的工具（新增或元数据变化）
            final var updateTools = flushTools.entrySet()
                    .stream()
                    .filter(entry -> {
                        final var name = entry.getKey();
                        final var fTool = entry.getValue();
                        final var aTool = tools.get(name);
                        return null == aTool || !fTool.meta().equals(aTool.meta());
                    })
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue
                    ));

            final var stages = new ArrayList<CompletionStage<Void>>();
            removeNames.forEach(name -> {
                final var stage = toolbox.remove(name)
                        .thenAccept(unused -> {
                            tools.remove(name);
                            logger.debug("{} remove tool: {}", this, name);
                        });
                stages.add(stage);
            });
            updateTools.forEach((name, tool) -> {
                final var stage = toolbox.register(name, tool)
                        .thenAccept(unused -> {
                            tools.put(name, tool);
                            logger.debug("{} upsert tool: {}", this, name);
                        });
                stages.add(stage);
            });

            return CompletableFutureUtils.allOf(stages);
        });
    }

    /**
     * 同步线程主循环
     * <p>
     * 定期从 MCP 服务器同步工具变更，直到线程被中断。
     * 如果同步过程中发生错误，会记录警告并在下一个周期重试。
     * </p>
     */
    private void sync() {
        logger.trace("{}/syncer started.", this);
        try {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 执行同步操作
                    syncTools().toCompletableFuture().join();
                    //noinspection BusyWait
                    Thread.sleep(syncInterval.toMillis());
                } catch (InterruptedException e) {
                    // 线程被中断，退出循环
                    Thread.currentThread().interrupt();
                    break;
                } catch (Throwable t) {
                    // 同步失败，记录警告并重试
                    logger.warn("{} sync error, will be retry after: {}ms", this, syncInterval.toMillis(), t);
                }
            }
        } finally {
            logger.trace("{}/syncer stopped.", this);
        }
    }

    /**
     * 关闭加载器
     * <p>
     * 中断同步线程，停止定期同步操作。
     * 该方法可以安全地多次调用，只有第一次调用会执行真正的关闭操作。
     * </p>
     */
    @Override
    public void close() {
        if (!closeF.complete(null)) {
            return;
        }
        // 中断同步线程
        if (!syncer.isInterrupted()) {
            syncer.interrupt();
        }
    }

    /**
     * 创建构建器
     *
     * @return 新的 Builder 实例
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * McpLoader 构建器
     * <p>
     * 使用 Builder 模式配置 MCP 工具加载器。
     * </p>
     */
    public static class Builder implements Buildable<McpToolLoader, Builder> {

        /**
         * MCP 加载器名称
         */
        private String name;
        
        /**
         * MCP 客户端传输层
         */
        private McpClientTransport transport;
        
        /**
         * 同步间隔时间，默认为 1 小时
         */
        private Duration syncInterval = Duration.ofHours(1);

        /**
         * 设置加载器名称
         *
         * @param name 名称
         * @return 当前构建器
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置 MCP 传输层
         *
         * @param transport 传输层实例
         * @return 当前构建器
         */
        public Builder transport(McpClientTransport transport) {
            this.transport = transport;
            return this;
        }

        /**
         * 设置同步间隔时间
         *
         * @param syncInterval 同步间隔
         * @return 当前构建器
         */
        public Builder syncInterval(Duration syncInterval) {
            this.syncInterval = syncInterval;
            return this;
        }

        /**
         * 构建 MCP 工具加载器
         *
         * @return 新创建的加载器实例
         */
        @Override
        public McpToolLoader build() {
            return new McpToolLoader(this);
        }

    }

}
