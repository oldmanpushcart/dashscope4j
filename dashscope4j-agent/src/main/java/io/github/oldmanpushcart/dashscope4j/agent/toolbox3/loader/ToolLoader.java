package io.github.oldmanpushcart.dashscope4j.agent.toolbox3.loader;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox3.ToolSubscription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox3.ToolSubscriptionHandler;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox3.ToolUse;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface ToolLoader extends AutoCloseable {

    CompletionStage<Void> subscribe(ToolSubscription subscription, ToolSubscriptionHandler handler);

    void unsubscribe(ToolSubscription subscription);

    List<ToolUse> loaded();

    boolean isClosed();

    @Override
    void close();

}
