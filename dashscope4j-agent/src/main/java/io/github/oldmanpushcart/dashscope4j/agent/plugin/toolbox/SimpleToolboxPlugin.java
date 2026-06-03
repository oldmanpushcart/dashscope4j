package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.indexer.HashMapToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.indexer.ToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.mcp.McpLoader;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.skill.SkillLoader;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.toolkit.ToolkitLoader;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.modelcontextprotocol.spec.McpClientTransport;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 一个简单的工具箱插件
 */
public class SimpleToolboxPlugin implements Plugin {

    private final Duration syncInterval;
    private final Path dataspace;
    private final boolean enableSearchTools;
    private final Function<Agent, ToolIndexer> indexerFactory;
    private final List<Supplier<CompletionStage<? extends ToolLoader>>> loaderSuppliers;

    private volatile Toolbox toolbox;

    private SimpleToolboxPlugin(Builder builder) {
        this.syncInterval = builder.syncInterval;
        this.dataspace = builder.dataspace;
        this.enableSearchTools = builder.enableSearchTools;
        this.indexerFactory = builder.indexerFactory;
        this.loaderSuppliers = builder.loaderSuppliers;
    }

    // 创建工具索引
    private ToolIndexer createToolIndexer(Agent agent) {
        final var factory = Optional.ofNullable(indexerFactory)
                .orElseGet(() -> a -> HashMapToolIndexer.newBuilder()
                        .client(agent.client())
                        .cacheFile(dataspace.resolve(Path.of(".toolbox-index-cache.jsonl")))
                        .build());
        return Optional.ofNullable(factory.apply(agent))
                .orElseThrow(() -> new IllegalStateException("indexer must not be null!"));
    }

    @Override
    public CompletionStage<Extension> install(Agent agent) {

        // 创建工具箱
        this.toolbox = HashMapToolbox.newBuilder()
                .indexer(createToolIndexer(agent))
                .syncInterval(syncInterval)
                .shared(false)
                .build();

        // 订阅所有工具加载器
        CompletionStage<Void> stage = CompletableFuture.completedStage(null);
        for (final var loaderSupplier : loaderSuppliers) {
            stage = stage
                    .thenCompose(unused -> loaderSupplier.get())
                    .thenAccept(toolbox::subscribe);
        }

        // 订阅完成，返回插件扩展
        return stage.thenApply(u -> {
            final var settingInterceptor = new SettingInterceptor(toolbox, enableSearchTools);
            return new Extension() {
                @Override
                public Plugin plugin() {
                    return SimpleToolboxPlugin.this;
                }

                @Override
                public List<ChatInterceptor> interceptors(Phases phases) {
                    return switch (phases) {
                        case PREPARATION -> List.of(settingInterceptor);
                        case INTERACTION -> List.of();
                    };
                }
            };
        });
    }


    @Override
    public CompletionStage<Void> uninstall() {
        return CompletableFuture.completedStage(null)
                .thenAccept(u -> {
                    if (null != toolbox && !toolbox.isShared() && !toolbox.isClosed()) {
                        toolbox.close();
                    }
                });
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<SimpleToolboxPlugin, Builder> {

        private Duration syncInterval = Duration.ofSeconds(5);
        private Path dataspace = Path.of("./");
        private boolean enableSearchTools = true;
        private Function<Agent, ToolIndexer> indexerFactory;
        private final List<Supplier<CompletionStage<? extends ToolLoader>>> loaderSuppliers = new ArrayList<>();

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

        /**
         * 添加数据空间
         *
         * @param dataspace 数据空间
         * @return this
         */
        public Builder dataspace(Path dataspace) {
            this.dataspace = dataspace;
            return this;
        }

        /**
         * 设置是否启用搜索工具
         *
         * @param enableSearchTools 是否启用搜索工具
         * @return this
         */
        public Builder enableSearchTools(boolean enableSearchTools) {
            this.enableSearchTools = enableSearchTools;
            return this;
        }

        /**
         * 工具索引工厂
         *
         * @param factory 工具索引工厂
         * @return this
         */
        public Builder indexerFactory(Function<Agent, ToolIndexer> factory) {
            this.indexerFactory = factory;
            return this;
        }

        /**
         * 添加一个MCP工具
         *
         * @param mode      模式
         * @param name      名称
         * @param transport 传输
         * @return this
         */
        public Builder mcp(ToolUse.Mode mode, String name, McpClientTransport transport) {
            loaderSuppliers.add(() -> McpLoader.newBuilder()
                    .mode(mode)
                    .name(name)
                    .transport(transport)
                    .buildAsync());
            return this;
        }

        /**
         * 添加一个技能工具
         *
         * @param mode 模式
         * @param home SKILL目录
         * @return this
         */
        public Builder skill(ToolUse.Mode mode, Path home) {
            loaderSuppliers.add(() -> CompletableFuture.completedStage(null)
                    .thenApply(u -> SkillLoader.newBuilder()
                            .mode(mode)
                            .directories(List.of(home))
                            .build()));
            return this;
        }

        /**
         * 添加一个工具包工具
         *
         * @param mode    模式
         * @param toolkit 工具包
         * @return this
         */
        public Builder toolkit(ToolUse.Mode mode, Toolkit toolkit) {
            loaderSuppliers.add(() -> CompletableFuture.completedStage(null)
                    .thenApply(u -> {
                        final var loader = new ToolkitLoader();
                        loader.append(mode, toolkit);
                        return loader;
                    }));
            return this;
        }

        /**
         * 添加一个工具工具
         *
         * @param mode 模式
         * @param tool 工具
         * @return this
         */
        public Builder tool(ToolUse.Mode mode, Tool tool) {
            loaderSuppliers.add(() -> CompletableFuture.completedStage(null)
                    .thenApply(u -> {
                        final var loader = new ToolkitLoader();
                        loader.append(mode, tool);
                        return loader;
                    }));
            return this;
        }

        @Override
        public SimpleToolboxPlugin build() {
            return new SimpleToolboxPlugin(this);
        }
    }

}
