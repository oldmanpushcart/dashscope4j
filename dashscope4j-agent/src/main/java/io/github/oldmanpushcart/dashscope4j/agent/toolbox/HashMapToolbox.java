package io.github.oldmanpushcart.dashscope4j.agent.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer.ToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

/**
 * 基于 HashMap 的工具箱实现
 * <p>
 * 提供工具的注册、查询和管理功能，核心特性包括：
 * <ul>
 *     <li><b>工具索引</b>：通过 ToolIndexer 实现基于意图的智能工具检索</li>
 *     <li><b>工具加载</b>：支持多个 ToolLoader 并行加载工具</li>
 *     <li><b>线程安全</b>：使用 ConcurrentHashMap 保证并发访问安全</li>
 *     <li><b>生命周期管理</b>：支持初始化和关闭操作</li>
 * </ul>
 * </p>
 *
 * @see Toolbox
 * @see ToolIndexer
 * @see ToolLoader
 */
public class HashMapToolbox implements Toolbox {

    /**
     * 日志记录器
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    
    /**
     * 工具索引器，用于根据意图智能检索工具
     */
    private final ToolIndexer indexer;
    
    /**
     * 工具加载器组，负责从不同来源加载工具
     */
    private final ToolLoader loader;

    /**
     * 工具存储表（线程安全）
     * key: 工具名称
     * value: 工具实例
     */
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    
    /**
     * 初始化标志
     */
    private final AtomicBoolean init = new AtomicBoolean(false);
    
    /**
     * 关闭标志
     */
    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * 构造 HashMapToolbox
     *
     * @param builder 构建器
     */
    private HashMapToolbox(Builder builder) {
        Objects.requireNonNull(builder.indexer, "indexer must not be null!");
        this.indexer = builder.indexer;
        // 将多个加载器包装为组加载器
        this.loader = new GroupToolLoader(CommonUtils.unmodifiableCopy(builder.loaders));
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/toolbox";
    }

    /**
     * 初始化工具箱
     * <p>
     * 触发所有 ToolLoader 并行安装工具，完成后可正常使用。
     * 该方法只能调用一次，重复调用会抛出异常。
     * </p>
     *
     * @return 初始化完成的 CompletableFuture
     * @throws IllegalStateException 如果已经关闭或已经初始化
     */
    CompletionStage<HashMapToolbox> init() {

        if (isClosed()) {
            throw new IllegalStateException("Already closed!");
        }

        if (!init.compareAndSet(false, true)) {
            throw new IllegalStateException("Already initialized!");
        }

        return loader.install(this)
                .thenApply(u -> this)
                .whenComplete((u, ex) -> {
                    if (null != ex) {
                        logger.warn("{} init failed!", this, ex);
                        // 初始化失败时自动关闭资源
                        close();
                    } else {
                        logger.debug("{} init success.", this);
                    }
                });
    }

    /**
     * 根据用户意图查找相关工具
     * <p>
     * 通过 ToolIndexer 智能检索与用户意图匹配的工具名称，
     * 然后从工具表中获取对应的工具实例。
     * </p>
     *
     * @param instant 用户意图消息
     * @return 匹配的工具映射表（工具名 -> 工具实例）
     */
    @Override
    public CompletionStage<Map<String, Tool>> lookup(UserMessage instant) {
        return indexer.query(instant.text())
                .thenApply(names -> {
                    final var result = new HashMap<String, Tool>();
                    // 过滤掉已删除的工具
                    names.forEach(name -> {
                        final var tool = tools.get(name);
                        if (null != tool) {
                            result.put(name, tool);
                        }
                    });
                    return result;
                });
    }

    /**
     * 根据工具名称精确查找工具
     *
     * @param name 工具名称
     * @return 工具实例，如果不存在则返回 null
     */
    @Override
    public CompletionStage<Tool> lookupByName(String name) {
        return CompletableFuture.completedStage(tools.get(name));
    }

    /**
     * 注册工具
     * <p>
     * 将工具同时注册到索引器和工具表中，使其可以被查找和使用。
     * </p>
     *
     * @param name 工具名称
     * @param tool 工具实例
     * @return 注册完成的 CompletableFuture
     */
    @Override
    public CompletionStage<Void> register(String name, Tool tool) {
        return indexer.upsert(name, tool)
                .thenAccept(u -> tools.put(name, tool));
    }

    /**
     * 移除工具
     * <p>
     * 从索引器和工具表中同时移除指定工具。
     * </p>
     *
     * @param name 工具名称
     * @return 移除完成的 CompletableFuture
     */
    @Override
    public CompletionStage<Void> remove(String name) {
        return indexer.remove(name)
                .thenAccept(u -> tools.remove(name));
    }

    /**
     * 检查工具箱是否已关闭
     *
     * @return true 如果已关闭，否则 false
     */
    @Override
    public boolean isClosed() {
        return closed.get();
    }

    /**
     * 关闭工具箱
     * <p>
     * 释放所有资源，包括关闭加载器和索引器。
     * 该方法可以安全地多次调用，只有第一次调用会执行真正的关闭操作。
     * </p>
     */
    @Override
    public void close() {

        if (!closed.compareAndSet(false, true)) {
            return;
        }

        // 关闭所有加载器和索引器
        IOUtils.closeQuietly(loader);
        IOUtils.closeQuietly(indexer);

        logger.debug("{} closed.", this);

    }


    /**
     * 工具加载器组
     * <p>
     * 将多个 ToolLoader 组合为一个逻辑加载器，支持并行安装和统一关闭。
     * </p>
     */
    class GroupToolLoader implements ToolLoader {

        /**
         * 加载器列表
         */
        private final List<ToolLoader> loaders;

        /**
         * 构造加载器组
         *
         * @param loaders 加载器列表
         */
        GroupToolLoader(List<ToolLoader> loaders) {
            this.loaders = loaders;
        }

        /**
         * 并行安装所有工具加载器
         *
         * @param toolbox 工具箱实例
         * @return 所有加载器安装完成的 CompletableFuture
         */
        @Override
        public CompletionStage<Void> install(Toolbox toolbox) {

            // 并行安装所有工具加载器
            final var stages = loaders.stream()
                    .map(loader -> loader.install(HashMapToolbox.this))
                    .toList();

            return CompletableFutureUtils.allOf(stages);
        }

        /**
         * 关闭所有加载器
         */
        @Override
        public void close() {
            // 关闭所有安装的加载器
            loaders.forEach(IOUtils::closeQuietly);
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
     * HashMapToolbox 构建器
     * <p>
     * 使用 Builder 模式配置和构建工具箱实例。
     * </p>
     */
    public static class Builder implements Buildable<HashMapToolbox, Builder> {

        /**
         * 工具索引器
         */
        private ToolIndexer indexer;
        
        /**
         * 工具加载器列表
         */
        private List<ToolLoader> loaders;

        /**
         * 设置工具索引器
         *
         * @param indexer 索引器实例
         * @return 当前构建器
         */
        public Builder indexer(ToolIndexer indexer) {
            this.indexer = indexer;
            return this;
        }

        /**
         * 设置工具加载器列表
         *
         * @param loaders 加载器列表
         * @return 当前构建器
         */
        public Builder loaders(List<ToolLoader> loaders) {
            this.loaders = loaders;
            return this;
        }

        /**
         * 通过操作符修改工具加载器列表
         *
         * @param operator 列表操作符
         * @return 当前构建器
         */
        public Builder loaders(UnaryOperator<List<ToolLoader>> operator) {
            this.loaders = operator.apply(CommonUtils.mutableCopy(this.loaders));
            return this;
        }

        /**
         * 同步构建工具箱（阻塞等待初始化完成）
         *
         * @return 初始化完成的工具箱实例
         */
        @Override
        public HashMapToolbox build() {
            return buildAsync()
                    .toCompletableFuture()
                    .join();
        }

        /**
         * 异步构建工具箱（非阻塞）
         *
         * @return 初始化完成的 CompletionStage
         */
        public CompletionStage<HashMapToolbox> buildAsync() {
            //noinspection resource
            return new HashMapToolbox(this)
                    .init();
        }

    }

}
