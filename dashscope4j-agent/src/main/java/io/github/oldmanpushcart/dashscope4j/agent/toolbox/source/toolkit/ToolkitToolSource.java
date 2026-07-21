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

    private final List<Tool> tools = new CopyOnWriteArrayList<>();
    private volatile State state = State.IDLE;

    private ToolkitToolSource(Builder builder) {
        super(builder.name);
        this.tools.addAll(builder.tools);
    }

    public ToolkitToolSource append(List<Tool> tools) {

        if (isClosed()) {
            throw new IllegalStateException("Already closed!");
        }

        if (!isInitialized()) {
            throw new IllegalStateException("Not initialized!");
        }

        synchronized (this) {
            if (null != tools) {
                this.tools.addAll(tools);
                fireChanged();
            }
        }

        return this;
    }

    public ToolkitToolSource append(Toolkit toolkit) {
        return append(toolkit.tools());
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
                .name(name)
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

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder implements Buildable<ToolkitToolSource, Builder> {

        private String name;
        private final List<Tool> tools = new ArrayList<>();

        public Builder name(String name) {
            this.name = name;
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
                    this.tools.addAll(toolkit.tools());
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
