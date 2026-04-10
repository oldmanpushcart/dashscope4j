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

public class HashMapToolbox implements Toolbox {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ToolIndexer indexer;
    private final ToolLoader loader;

    private final Map<String, Tool> tools = new ConcurrentHashMap<>();
    private final AtomicBoolean init = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private HashMapToolbox(Builder builder) {
        Objects.requireNonNull(builder.indexer, "indexer must not be null!");
        this.indexer = builder.indexer;
        this.loader = new GroupToolLoader(CommonUtils.unmodifiableCopy(builder.loaders));
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/toolbox";
    }

    /**
     * 初始化
     *
     * @return 初始化回调
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
                        close();
                    } else {
                        logger.debug("{} init success.", this);
                    }
                });
    }

    @Override
    public CompletionStage<Map<String, Tool>> lookup(UserMessage instant) {
        return indexer.query(instant.text())
                .thenApply(names -> {
                    final var result = new HashMap<String, Tool>();
                    names.forEach(name -> {
                        final var tool = tools.get(name);
                        if (null != tool) {
                            result.put(name, tool);
                        }
                    });
                    return result;
                });
    }

    @Override
    public CompletionStage<Tool> lookupByName(String name) {
        return CompletableFuture.completedStage(tools.get(name));
    }

    @Override
    public CompletionStage<Void> register(String name, Tool tool) {
        return indexer.upsert(name, tool)
                .thenAccept(u -> tools.put(name, tool));
    }

    @Override
    public CompletionStage<Void> remove(String name) {
        return indexer.remove(name)
                .thenAccept(u -> tools.remove(name));
    }

    @Override
    public boolean isClosed() {
        return closed.get();
    }

    @Override
    public void close() {

        if (!closed.compareAndSet(false, true)) {
            return;
        }

        IOUtils.closeQuietly(loader);
        IOUtils.closeQuietly(indexer);

        logger.debug("{} closed.", this);

    }


    /**
     * 工具加载器组
     */
    class GroupToolLoader implements ToolLoader {

        private final List<ToolLoader> loaders;

        GroupToolLoader(List<ToolLoader> loaders) {
            this.loaders = loaders;
        }

        @Override
        public CompletionStage<Void> install(Toolbox toolbox) {

            // 并行安装所有工具加载器
            final var stages = loaders.stream()
                    .map(loader -> loader.install(HashMapToolbox.this))
                    .toList();

            return CompletableFutureUtils.allOf(stages);
        }

        @Override
        public void close() {
            // 关闭所有安装的加载器
            loaders.forEach(IOUtils::closeQuietly);
        }

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<HashMapToolbox, Builder> {

        private ToolIndexer indexer;
        private List<ToolLoader> loaders;

        public Builder indexer(ToolIndexer indexer) {
            this.indexer = indexer;
            return this;
        }

        public Builder loaders(List<ToolLoader> loaders) {
            this.loaders = loaders;
            return this;
        }

        public Builder loaders(UnaryOperator<List<ToolLoader>> operator) {
            this.loaders = operator.apply(CommonUtils.mutableCopy(this.loaders));
            return this;
        }

        @Override
        public HashMapToolbox build() {
            return buildAsync()
                    .toCompletableFuture()
                    .join();
        }

        public CompletionStage<HashMapToolbox> buildAsync() {
            //noinspection resource
            return new HashMapToolbox(this)
                    .init();
        }

    }

}
