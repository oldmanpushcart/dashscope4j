package io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.toolkit;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.AbstractToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils.illegalStateStage;

public class ToolkitToolSource extends AbstractToolSource {

    private final String _toString;
    private final List<Tool> tools = new CopyOnWriteArrayList<>();
    private volatile State state = State.IDLE;

    private ToolkitToolSource(Builder builder) {
        super(builder.namespace);
        this._toString = "dashscope4j-agent:/toolbox/source/toolkit/%s".formatted(namespace());
        this.tools.addAll(builder.tools);
    }

    @Override
    public String toString() {
        return _toString;
    }

    public ToolkitToolSource append(Iterable<? extends Tool> it) {

        if (isClosed()) {
            throw new IllegalStateException("Already closed!");
        }

        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized!");
        }

        synchronized (this) {
            if (null != it) {
                it.forEach(tool -> {
                    final var namespaceTool = new NamespaceTool(namespace(), tool);
                    this.tools.add(namespaceTool);
                });
                fireChanged();
            }
        }

        return this;
    }

    public ToolkitToolSource remove(List<String> names) {

        if (isClosed()) {
            throw new IllegalStateException("Already closed!");
        }

        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized!");
        }

        synchronized (this) {
            if (null != names) {
                this.tools.removeIf(tool -> names.contains(tool.meta().name()));
                fireChanged();
            }
        }

        return this;
    }

    @Override
    public List<Tool> tools() {

        if (isClosed()) {
            throw new IllegalStateException("Already closed!");
        }

        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized!");
        }

        synchronized (this) {
            return Collections.unmodifiableList(tools);
        }

    }

    @Override
    public synchronized CompletionStage<ToolkitToolSource> initialize() {

        if (isClosed()) {
            return illegalStateStage("Already closed!");
        }

        if (isInitialized()) {
            return illegalStateStage("Already initialized!");
        }

        state = State.INITIALIZED;
        return CompletableFuture.completedStage(this);
    }

    @Override
    public boolean isClosed() {
        return State.CLOSED == state;
    }

    private boolean isInitialized() {
        return State.INITIALIZED == state;
    }

    @Override
    public synchronized void close() {
        if (isClosed()) {
            return;
        }
        state = State.CLOSED;
        tools.clear();
        super.close();
    }

    public static ToolkitToolSource create(String name) {
        return newBuilder()
                .namespace(name)
                .build();
    }

    public static ToolkitToolSource create() {
        return newBuilder()
                .build();
    }

    private enum State {
        IDLE,
        INITIALIZED,
        CLOSED
    }

    private static class NamespaceTool implements Tool {

        private final Tool tool;
        private final Tool.Meta meta;

        private NamespaceTool(String namespace, Tool tool) {
            this.tool = tool;
            this.meta = new Meta() {

                private final String name = "%s$%s".formatted(namespace, tool.meta().name());

                @Override
                public String name() {
                    return name;
                }

                @Override
                public String description() {
                    return tool.meta().description();
                }
            };
        }

        @Override
        public Meta meta() {
            return meta;
        }

        @Override
        public Classify classify() {
            return tool.classify();
        }

        @Override
        public CompletionStage<String> call(Caller caller, String argumentJson) {
            return tool.call(caller, argumentJson);
        }

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<ToolkitToolSource, Builder> {

        private String namespace;
        private final List<Tool> tools = new ArrayList<>();

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder append(Tool... tools) {
            if (null != tools) {
                this.tools.addAll(List.of(tools));
            }
            return this;
        }

        public Builder append(Toolkit... toolkits) {
            if (null != toolkits) {
                for (Toolkit toolkit : toolkits) {
                    toolkit.forEach(this.tools::add);
                }
            }
            return this;
        }

        @Override
        public ToolkitToolSource build() {
            return new ToolkitToolSource(this);
        }

    }

}
