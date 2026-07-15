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

        if (State.RUNNING != state) {
            throw new IllegalStateException("Not initialized!");
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

        if (State.RUNNING != state) {
            throw new IllegalStateException("Not initialized!");
        }

        if (null != names) {
            this.tools.removeIf(tool -> names.contains(tool.meta().name()));
            fireChanged();
        }
        return this;
    }

    @Override
    public List<Tool> tools() {

        if (State.RUNNING != state) {
            throw new IllegalStateException("Not initialized!");
        }

        return Collections.unmodifiableList(tools);
    }

    @Override
    public synchronized ToolkitSource initialize() {

        if (State.CLOSED == state) {
            throw new IllegalStateException("Already closed!");
        }

        state = State.RUNNING;
        return this;
    }

    @Override
    public boolean isClosed() {
        return State.CLOSED == state;
    }

    @Override
    public synchronized void close() {
        if (State.CLOSED == state) {
            return;
        }
        state = State.CLOSED;
        super.close();
        tools.clear();
    }

    public static ToolkitSource create(String name) {
        return new ToolkitSource(name);
    }

    private enum State {
        IDLE,
        RUNNING,
        CLOSED
    }

}
