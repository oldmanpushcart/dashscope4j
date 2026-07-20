package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.source.toolkit;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.source.AbstractToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolkit.Toolkit;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ToolkitToolSource extends AbstractToolSource {

    private final List<Tool> tools = new CopyOnWriteArrayList<>();
    private volatile State state = State.IDLE;

    public ToolkitToolSource(String name) {
        super(name);
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
    public synchronized ToolkitToolSource initialize() {

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

    public static ToolkitToolSource create(String name) {
        return new ToolkitToolSource(name);
    }

    public static ToolkitToolSource create() {
        return new ToolkitToolSource(null);
    }

    private enum State {
        IDLE,
        INITIALIZED,
        CLOSED
    }

}
