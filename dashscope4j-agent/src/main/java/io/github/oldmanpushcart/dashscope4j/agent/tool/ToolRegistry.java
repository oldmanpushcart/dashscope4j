package io.github.oldmanpushcart.dashscope4j.agent.tool;

import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.router.ToolRouter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ToolRegistry implements ToolLookup {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<ToolLoader, List<Tool>> sources = new ConcurrentHashMap<>();
    private final List<ToolRouter> routers;
    private final List<ToolLoader> loaders;

    public ToolRegistry(Builder builder) {
        this.routers = CommonUtils.unmodifiableCopy(builder.routers);
        this.loaders = CommonUtils.unmodifiableCopy(builder.loaders);
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/tool";
    }

    private CompletionStage<ToolRegistry> init() {
        CompletionStage<Void> stage = CompletableFuture.completedStage(null);
        for (var loader : loaders) {
            stage = stage
                    .thenCompose(unused -> loader.init(tools -> {

                        // 工具为空，说明加载器的工具全部被卸载，则从缓存中移除
                        if (CommonUtils.isEmpty(tools)) {
                            sources.remove(loader);
                            logger.debug("{} unregister loader: {}", this, loader.name());
                            return;
                        }

                        // 加载到工具，则加入缓存
                        final var wraps = tools.stream()
                                .filter(FunctionTool.class::isInstance)
                                .map(FunctionTool.class::cast)

                                /*
                                 * 为了让不同加载器加载的工具不因为重名而冲突，
                                 * 在加载的时候将工具名改为：加载器名$工具名。
                                 */
                                .<Tool>map(tool -> {
                                    final var name = "%s$%s".formatted(loader.name(), tool.meta().name());
                                    final var desc = tool.meta().description();
                                    final var schema = tool.meta().parameterSchema();
                                    return new FunctionTool() {

                                        @Override
                                        public Meta meta() {
                                            return new Meta(name, desc, schema);
                                        }

                                        @Override
                                        public CompletionStage<String> call(Caller caller, String argumentJson) {
                                            return tool.call(caller, argumentJson);
                                        }

                                    };
                                })
                                .toList();

                        // 将工具加入缓存
                        sources.put(loader, wraps);
                        logger.debug("{} register loader: {}, total: {} tools", this, loader.name(), wraps.size());

                    }))
                    .whenComplete((u, t) -> {
                        if (null != t) {
                            logger.warn("{} init loader: {} failed!", this, loader.name(), t);
                        } else {
                            logger.debug("{} init loader: {} success!", this, loader.name());
                        }
                    });
        }
        return stage.thenApply(u -> this);
    }

    @Override
    public CompletionStage<List<Tool>> lookup(String intent) {

        // 如果路由器为空，则不路由
        if (routers.isEmpty()) {
            return CompletableFuture.completedStage(List.of());
        }

        // 转换为 Map<工具名,工具> 集合
        final var repository = sources.values()
                .stream()
                .flatMap(Collection::stream)
                .filter(FunctionTool.class::isInstance)
                .map(FunctionTool.class::cast)
                .collect(Collectors.toMap(
                        tool -> tool.meta().name(),
                        tool -> (Tool) tool
                ));

        // 异步串行通过路由器集合，对意图与工具进行匹配。
        CompletionStage<Map<String, Tool>> stage = CompletableFuture.completedStage(repository);
        for (var router : routers) {
            stage = stage.thenCompose(repo -> router.routing(repo, intent));
        }

        // 转换为工具列表
        return stage
                .thenApply(repo -> new ArrayList<>(repo.values()));
    }

    @Override
    public Optional<Tool> get(String name) {
        return sources.values()
                .stream()
                .flatMap(Collection::stream)
                .filter(FunctionTool.class::isInstance)
                .map(FunctionTool.class::cast)
                .filter(tool -> tool.meta().name().equals(name))
                .map(tool -> (Tool) tool)
                .findFirst();
    }


    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<CompletionStage<ToolRegistry>, Builder> {

        private List<ToolLoader> loaders;
        private List<ToolRouter> routers;

        public Builder loaders(List<ToolLoader> loaders) {
            this.loaders = loaders;
            return this;
        }

        public Builder loaders(UnaryOperator<List<ToolLoader>> operator) {
            this.loaders = operator.apply(CommonUtils.mutableCopy(this.loaders));
            return this;
        }

        public Builder routers(List<ToolRouter> routers) {
            this.routers = routers;
            return this;
        }

        public Builder routers(UnaryOperator<List<ToolRouter>> operator) {
            this.routers = operator.apply(CommonUtils.mutableCopy(this.routers));
            return this;
        }

        @Override
        public CompletionStage<ToolRegistry> build() {
            return new ToolRegistry(this).init();
        }

    }

}
