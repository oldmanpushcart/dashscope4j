package io.github.oldmanpushcart.dashscope4j.agent.toolbox3.loader;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox3.ToolSubscription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox3.ToolSubscriptionHandler;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox3.ToolUse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractToolLoader implements ToolLoader {

    private final Map<ToolSubscription, ToolSubscriptionHandler> handlerMap = new ConcurrentHashMap<>();
    private final CompletableFuture<?> closeF = new CompletableFuture<>();

    protected void notifyChanged(List<ToolUse> upserts, List<String> removes) {
        handlerMap.forEach((subscription, handler) -> {
            if (!subscription.isClosed()) {
                handler.onChange(upserts, removes);
            }
        });
    }

    @Override
    public CompletionStage<Void> subscribe(ToolSubscription subscription, ToolSubscriptionHandler handler) {
        return handler.onSubscribe()
                .thenAccept(u -> {
                    synchronized (subscription) {
                        if (!subscription.isClosed()) {
                            handlerMap.put(subscription, handler);
                        }
                    }
                });
    }

    @Override
    public void unsubscribe(ToolSubscription subscription) {
        handlerMap.remove(subscription);
    }

    @Override
    public boolean isClosed() {
        return closeF.isDone();
    }

    @Override
    public void close() {

        if (!closeF.complete(null)) {
            return;
        }

        // 遍历取消所有订阅
        new HashMap<>(handlerMap).keySet().forEach(subscription -> {
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (subscription) {
                if (!subscription.isClosed()) {
                    subscription.close();
                }
            }
        });

        // 最后还需要清理一下，避免有遗漏
        handlerMap.clear();

    }

}
