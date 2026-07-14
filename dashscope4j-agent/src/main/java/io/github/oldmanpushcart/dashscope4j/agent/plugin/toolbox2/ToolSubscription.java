package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox2.source.ToolSource;

public interface ToolSubscription extends AutoCloseable {

    ToolSource source();

    void subscribe();

    boolean isSubscribed();

    boolean isClosed();

    @Override
    void close();

}
