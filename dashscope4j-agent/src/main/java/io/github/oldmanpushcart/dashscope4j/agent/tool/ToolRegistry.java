package io.github.oldmanpushcart.dashscope4j.agent.tool;

import io.github.oldmanpushcart.dashscope4j.agent.tool.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.agent.tool.router.ToolRouter;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ToolRegistry {

    private static final String PREFIX = "tool_";
    private static final AtomicInteger indexer = new AtomicInteger(0);

    private final Map<String, Stub> stubMap = new ConcurrentHashMap<>();
    private final List<ToolRouter> routers;
    private final List<ToolLoader> loaders;

    public ToolRegistry(Builder builder) {
        this.routers = CommonUtils.unmodifiableCopy(builder.routers);
        this.loaders = CommonUtils.unmodifiableCopy(builder.loaders);
    }

    private CompletionStage<ToolRegistry> init() {
        CompletionStage<Void> stage = CompletableFuture.completedStage(null);
        for (var loader : loaders) {
            stage = stage.thenCompose(unused -> loader.init(tools -> {

                // cleanup
                final var entryIt = stubMap.entrySet().iterator();
                while (entryIt.hasNext()) {
                    final var entry = entryIt.next();
                    final var stub = entry.getValue();
                    if (stub.loader() == loader) {
                        entryIt.remove();
                    }
                }

                // reset
                tools.forEach(tool -> {
                    if (tool instanceof FunctionTool functionTool) {
                        final var identity = PREFIX + indexer.getAndIncrement();
                        final var description = functionTool.meta().description();
                        final var schema = functionTool.meta().parameterSchema();
                        final var stub = new Stub(identity, loader, new FunctionTool() {

                            @Override
                            public CompletionStage<String> call(Caller caller, String argumentJson) {
                                return functionTool.call(caller, argumentJson);
                            }

                            @Override
                            public Meta meta() {
                                return new Meta(identity, description, schema);
                            }

                        });
                        stubMap.put(identity, stub);
                    }
                });

            }));
        }
        return stage.thenApply(u -> this);
    }

    public CompletionStage<List<Tool>> routing(String intent) {

        // 如果路由器为空，则不路由
        if (routers.isEmpty()) {
            return CompletableFuture.completedStage(List.of());
        }

        final var repository = stubMap.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().tool()
                ));

        CompletionStage<Map<String, Tool>> stage = CompletableFuture.completedStage(repository);
        for (var router : routers) {
            stage = stage.thenCompose(repo -> router.routing(repo, intent));
        }

        return stage
                .thenApply(repo -> new ArrayList<>(repo.values()));
    }

    public Tool get(String name) {
        return stubMap.get(name).tool();
    }

    private record Stub(String identity, ToolLoader loader, Tool tool) {

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
