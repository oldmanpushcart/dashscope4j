package io.github.oldmanpushcart.dashscope4j.agent.toolbox3;

public interface ToolSubscription extends AutoCloseable {

    boolean isClosed();

    @Override
    void close();

}
