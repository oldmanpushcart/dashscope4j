package io.github.oldmanpushcart.dashscope4j.agent.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer.ToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.loader.ToolLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class HashMapToolbox implements Toolbox {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ToolIndexer indexer;
    private final Map<String, Entry> registry = new ConcurrentHashMap<>();
    private final CompletableFuture<?> closeF = new CompletableFuture<>();

    // 变更同步器
    private final Syncer syncer;

    private HashMapToolbox(Builder builder) {
        Objects.requireNonNull(builder.indexer, "indexer cannot be null!");
        Objects.requireNonNull(builder.syncInterval, "syncInterval cannot be null!");
        this.indexer = builder.indexer;
        this.syncer = new Syncer(builder.syncInterval);
        this.syncer.start();
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/toolbox";
    }

    @Override
    public CompletionStage<ToolSubscription> subscribe(ToolLoader loader) {
        final var subscription = new ToolSubscriptionImpl(loader);
        final var handler = new ToolSubscriptionHandlerImpl(loader, subscription);
        return loader.subscribe(subscription, handler)
                .thenApply(u -> subscription);
    }

    @Override
    public CompletionStage<List<ToolUse>> lookupByIntent(String intent) {
        return indexer.query(intent)
                .thenApply(names -> {
                    final var result = new ArrayList<ToolUse>();
                    for (final var name : names) {
                        final var lookupOpt = lookupByName(name);
                        if (lookupOpt.isPresent()) {
                            result.add(lookupOpt.get());
                        } else {
                            indexer.remove(name);
                        }
                    }
                    return result;
                });
    }

    @Override
    public Optional<ToolUse> lookupByName(String name) {
        return Optional.ofNullable(registry.get(name))
                .map(Entry::use);
    }

    @Override
    public List<ToolUse> lookupAll() {
        return registry.values()
                .stream()
                .map(Entry::use)
                .toList();
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

        // 停止同步器
        syncer.interrupt();

        // 关闭所有的订阅关系
        new ArrayList<>(registry.values())
                .stream()
                .map(entry -> entry.subscription)
                .forEach(ToolSubscription::close);

        logger.debug("{} closed.", this);
    }

    private CompletionStage<Void> upsert(ToolUse use, ToolSubscription subscription) {
        final var name = use.tool().meta().name();
        registry.put(name, new Entry(name, use, subscription));
        return indexer.upsert(use.tool());
    }

    private void remove(String name, ToolSubscription subscription) {
        final var entry = registry.get(name);

        // 没有在注册表中找到，说明这个是脏数据，直接清理索结束
        if (entry == null) {
            indexer.remove(name);
            return;
        }

        // 只有订阅者自己才能删除
        if (entry.subscription != subscription) {
            throw new IllegalArgumentException("Remove tool: %s failed, not the same subscription!".formatted(name));
        }

        indexer.remove(name);
        registry.remove(name);

    }

    private void unsubscribe(ToolSubscription subscription) {
        //noinspection resource
        registry.entrySet()
                .removeIf(entry -> entry.getValue().subscription() == subscription);
    }

    private class ToolSubscriptionImpl implements ToolSubscription {

        private final ToolLoader loader;
        private final CompletableFuture<?> closeF = new CompletableFuture<>();

        private ToolSubscriptionImpl(ToolLoader loader) {
            this.loader = loader;
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
            unsubscribe(this);
            loader.unsubscribe(this);
        }

    }

    private class ToolSubscriptionHandlerImpl implements ToolSubscriptionHandler {

        private final ToolLoader loader;
        private final ToolSubscription subscription;

        private ToolSubscriptionHandlerImpl(ToolLoader loader, ToolSubscription subscription) {
            this.loader = loader;
            this.subscription = subscription;
        }

        @Override
        public CompletionStage<Void> onSubscribe() {
            CompletionStage<Void> stage = CompletableFuture.completedStage(null);
            for (ToolUse use : loader.loaded()) {
                stage = stage.thenCompose(u -> upsert(use, subscription));
            }
            return stage;
        }

        @Override
        public void onChange(List<ToolUse> upserts, List<String> removes) {
            syncer.notifyChange(upserts, removes, subscription);
        }

    }

    /**
     * 变更同步器
     * <p>
     * 继承 Thread，按固定周期串行处理 onChange 事件。
     * 消费失败的事件会保留在队列中，下次周期继续尝试。
     * </p>
     */
    private class Syncer extends Thread {

        private final ReentrantLock lock = new ReentrantLock();
        private final Condition waiting = lock.newCondition();
        private final List<ChangeEvent> events = new ArrayList<>();

        private final Duration syncInterval;

        private record ChangeEvent(List<ToolUse> upserts, List<String> removes, ToolSubscription subscription) {
        }

        Syncer(Duration syncInterval) {
            this.syncInterval = syncInterval;
            setDaemon(true);
            setName("dashscope4j-agent:/toolbox/syncer");
        }

        /**
         * 通知变更
         *
         * @param upserts      新增/更新的工具列表
         * @param removes      删除的工具名称列表
         * @param subscription 对应的订阅
         */
        void notifyChange(List<ToolUse> upserts, List<String> removes, ToolSubscription subscription) {
            lock.lock();
            try {
                events.add(new ChangeEvent(upserts, removes, subscription));
                waiting.signalAll();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void run() {

            while (!isInterrupted()) {

                try {

                    // 等待一个同步周期
                    lock.lock();
                    try {
                        if (!waiting.await(syncInterval.toMillis(), TimeUnit.MILLISECONDS)) {
                            if (events.isEmpty()) {
                                continue;
                            }
                        }
                    } finally {
                        lock.unlock();
                    }

                    // 开始执行同步（使用迭代器，失败不删除）
                    final var eventIt = events.iterator();
                    while (eventIt.hasNext()) {
                        final var event = eventIt.next();

                        try {
                            processEvent(event);
                            eventIt.remove();
                        } catch (Throwable ex) {
                            logger.warn("{} sync change event failed, will retry after {}ms! upserts={}, removes={};",
                                    HashMapToolbox.this,
                                    syncInterval.toMillis(),
                                    event.upserts.size(),
                                    event.removes.size(),
                                    ex
                            );
                        }
                    }

                } catch (InterruptedException ieEx) {
                    interrupt();
                    break;
                }

            }// while

        }

        /**
         * 处理变更事件
         *
         * @param event 变更事件
         */
        private void processEvent(ChangeEvent event) {

            // 先处理删除
            for (final var name : event.removes) {
                remove(name, event.subscription);
            }

            // 再处理新增/更新
            for (final var use : event.upserts) {
                upsert(use, event.subscription).toCompletableFuture().join();
            }

        }
    }

    private record Entry(String name, ToolUse use, ToolSubscription subscription) {

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        private ToolIndexer indexer;
        private Duration syncInterval = Duration.ofSeconds(5);

        public Builder indexer(ToolIndexer indexer) {
            this.indexer = indexer;
            return this;
        }

        public Builder syncInterval(Duration syncInterval) {
            this.syncInterval = syncInterval;
            return this;
        }

        public HashMapToolbox build() {
            return new HashMapToolbox(this);
        }
    }

}
