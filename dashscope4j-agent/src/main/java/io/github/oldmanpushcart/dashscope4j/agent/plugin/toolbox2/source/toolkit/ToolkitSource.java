package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.toolkit;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.AbstractToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ToolkitSource extends AbstractToolSource {

    private final List<Tool> tools = new CopyOnWriteArrayList<>();
    private volatile State state = State.IDLE;

    public ToolkitSource(String name) {
        super(name);
    }

    public ToolkitSource append(List<Tool> tools) {

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

    public ToolkitSource append(Toolkit toolkit) {
        return append(toolkit.tools());
    }

    public ToolkitSource remove(List<String> names) {

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
    public synchronized ToolkitSource initialize() {

        if (isClosed()) {
            throw new IllegalStateException("Already closed!");
        }

        if (isInitialized()) {
            throw new IllegalStateException("Already initialized!");
        }

        state = State.INITIALIZED;
        return this;
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

    public static ToolkitSource create(String name) {
        return new ToolkitSource(name);
    }

    private enum State {
        IDLE,
        INITIALIZED,
        CLOSED
    }

}
