package io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.ToolSubscription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.ToolSubscriptionHandler;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.ToolUse;

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
