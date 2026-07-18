package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.source.ToolSource;

public interface ToolSubscription extends AutoCloseable {

    ToolSource source();

    void subscribe();

    boolean isSubscribed();

    boolean isClosed();

    @Override
    void close();

}
