package io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.indexer.ToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.toolbox.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
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
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils.illegalState;
import static io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils.unwrapEx;

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
    public CompletionStage<List<Tool>> lookupByIntent(String intent) {
        return indexer.query(intent)
                .thenApply(names -> {
                    final List<Tool> result = new ArrayList<>();
                    for (final var name : names) {
                        final var lookupOpt = lookupByName(name);
                        if (lookupOpt.isPresent()) {
                            result.add(lookupOpt.get());
                        } else {
                            logger.warn("{} found bad index, remove it. tool={}", this, name);
                            indexer.remove(name);
                        }
                    }
                    return result;
                })
                .exceptionallyCompose(ex -> illegalState(ex, "Lookup tools by intent failed!"))
                ;
    }

    @Override
    public Optional<Tool> lookupByName(String name) {
        return Optional.ofNullable(registry.get(name))
                .map(Entry::use)
                .map(ToolUse::tool);
    }

    @Override
    public List<Tool> lookupAll() {
        return registry.values()
                .stream()
                .map(Entry::use)
                .filter(use->use.mode() == ToolUse.Mode.FIXED)
                .map(ToolUse::tool)
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
        registry.values()
                .stream()
                .map(entry -> entry.subscription)
                .collect(Collectors.toSet())
                .forEach(IOUtils::closeQuietly);

        logger.debug("{} closed.", this);
    }

    /**
     * 添加工具
     * <p>
     * 添加的工具必须要有订阅关系，明确工具来源。
     * </p>
     *
     * @param use          工具使用信息
     * @param subscription 订阅关系
     * @return 操作回调
     */
    private CompletionStage<Void> upsert(ToolUse use, ToolSubscription subscription) {
        final var name = use.tool().meta().name();
        registry.put(name, new Entry(name, use, subscription));
        return indexer.upsert(use.tool())
                .exceptionallyCompose(ex -> illegalState(ex, "Upsert tool: %s occur error!".formatted(name)));
    }

    /**
     * 删除工具
     * <p>
     * 删除的工具必须要有订阅关系，只有订阅者自己才能删除
     * </p>
     *
     * @param name         工具名称
     * @param subscription 订阅关系
     */
    private void remove(String name, ToolSubscription subscription) {
        final var entry = registry.get(name);

        // 没有在注册表中找到，说明这个是脏数据，直接清理索结束
        if (entry == null) {
            indexer.remove(name);
            return;
        }

        // 只有订阅者自己才能删除
        if (entry.subscription != subscription) {
            throw new IllegalArgumentException("Remove tool: %s occur error, not the same subscription!".formatted(name));
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

        private final Duration syncInterval;

        private final ReentrantLock lock = new ReentrantLock();
        private final Condition waiting = lock.newCondition();
        private final List<ChangeEvent> events = new ArrayList<>();

        /**
         * 变更事件
         *
         * @param upserts      更新工具集合
         * @param removes      删除工具名称集合
         * @param subscription 订阅关系
         */
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

                    // 创建跳过名单，开始新一轮同步
                    final var skipSet = new HashSet<ToolSubscription>();

                    // 开始执行同步（使用迭代器，失败不删除）
                    final var eventIt = events.iterator();
                    while (eventIt.hasNext()) {
                        final var event = eventIt.next();

                        // 检查是否在跳过名单中
                        if (skipSet.contains(event.subscription)) {
                            logger.debug("{} skip event for subscription: {}", HashMapToolbox.this, event.subscription);
                            continue;
                        }

                        try {
                            processEvent(event);
                            eventIt.remove();
                        } catch (Throwable ex) {
                            // 加入跳过名单，本轮不再处理该 subscription 的后续事件
                            skipSet.add(event.subscription);
                            logger.warn("{} process change failed! will be retry after {}ms. subscription={}, upserts={}, removes={};",
                                    HashMapToolbox.this,
                                    syncInterval.toMillis(),
                                    event.subscription,
                                    event.upserts.size(),
                                    event.removes.size(),
                                    unwrapEx(ex)
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

    /**
     * 注册项
     *
     * @param name         工具名称
     * @param use          工具用途
     * @param subscription 订阅关系
     */
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
