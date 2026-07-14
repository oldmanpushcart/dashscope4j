package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.toolkit;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.AbstractToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

public class ToolkitSource extends AbstractToolSource {

    private final List<Tool> tools = new CopyOnWriteArrayList<>();
    private final CompletableFuture<?> initF = new CompletableFuture<>();
    private final CompletableFuture<?> closeF = new CompletableFuture<>();

    public ToolkitSource(String name) {
        super(name);
    }

    public ToolkitSource append(List<Tool> tools) {
        if (isClosed()) {
            throw new IllegalStateException("Already closed!");
        }
        if (null != tools) {
            this.tools.addAll(tools);
            fireChanged();
        }
        return this;
    }

    public ToolkitSource append(Toolkit toolkit) {
        return append(toolkit.tools());
    }

    public ToolkitSource remove(List<String> names) {
        if (isClosed()) {
            throw new IllegalStateException("Already closed!");
        }
        if (null != names) {
            this.tools.removeIf(tool -> names.contains(tool.meta().name()));
            fireChanged();
        }
        return this;
    }

    @Override
    public CompletionStage<List<Tool>> tools() {
        return initF
                .thenApply(u -> Collections.unmodifiableList(tools));
    }

    @Override
    public CompletionStage<ToolkitSource> initialize() {
        initF.complete(null);
        return initF.thenApply(u -> this);
    }

    @Override
    public boolean isClosed() {
        return closeF.isDone();
    }

    @Override
    public void close() {
        if (closeF.complete(null)) {
            super.close();
            tools.clear();
        }
    }

    public static ToolkitSource create(String name) {
        return new ToolkitSource(name);
    }

}
