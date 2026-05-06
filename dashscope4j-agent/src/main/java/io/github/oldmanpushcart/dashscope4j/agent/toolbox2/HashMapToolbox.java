package io.github.oldmanpushcart.dashscope4j.agent.toolbox2;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.indexer.ToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.Bundle;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.Subscription;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox2.loader.ToolLoader;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
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
import java.util.function.UnaryOperator;

import static java.util.concurrent.CompletableFuture.completedStage;

public class HashMapToolbox implements Toolbox {


    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ToolIndexer indexer;
    private final List<ToolLoader> loaders;
    private final Syncer syncer;

    private final List<Bundle> bundles = new ArrayList<>();
    private final List<Subscription> subscriptions = new ArrayList<>();


    private HashMapToolbox(Builder builder) {

        Objects.requireNonNull(builder.loaders, "loaders cannot be null!");
        Objects.requireNonNull(builder.indexer, "indexer cannot be null!");
        Objects.requireNonNull(builder.syncInterval, "syncInterval cannot be null!");

        this.loaders = CommonUtils.unmodifiableCopy(builder.loaders);
        this.indexer = builder.indexer;
        this.syncer = new Syncer(builder.syncInterval);
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/toolbox";
    }

    private CompletionStage<HashMapToolbox> init() {

        // 订阅loader的变更
        loaders.forEach(loader -> subscriptions.add(loader.subscribe(syncer::notifySync)));

        // 主动加载loader
        return reload(loaders)
                .exceptionallyCompose(ex -> {
                    final var cause = new IllegalStateException("Loding loaders failed in init!", ex);
                    return CompletableFuture.failedStage(cause);
                })
                .thenApply(u -> {

                    int fixedCnt = 0;
                    int dynamicCnt = 0;
                    for (final var bundle : bundles) {
                        for (final var use : bundle.uses()) {
                            switch (use.mode()) {
                                case FIXED -> fixedCnt++;
                                case DYNAMIC -> dynamicCnt++;
                            }
                        }
                    }

                    logger.debug("{} init completed. loaders={};fixed={};dynamic={};",
                            this,
                            loaders.size(),
                            fixedCnt,
                            dynamicCnt
                    );

                    syncer.start();
                    return this;
                });
    }

    private CompletionStage<Void> reload(List<ToolLoader> loaders) {
        CompletionStage<Void> stage = completedStage(null);
        for (final var loader : loaders) {
            stage = stage.thenCompose(u -> {

                /*
                 * 加载时候的注意顺序，必须严格保证这个顺序，避免出现索引搜索到但找不到工具的情况。
                 * 1. 清理：卸载loader原本所加载的所有工具包以其索引；先删除索引表，再删除工具包表；
                 * 1. 更新：先更新工具包表，然后再更新索引表。
                 */
                return loader.load().thenCompose(bundle -> completedStage(null)

                        // 清理所有已加载的数据
                        .thenCompose(uu -> {

                            // 找出所有待删除的工具包
                            //noinspection resource
                            final var removes = bundles.stream()
                                    .filter(b -> b.loader() == loader)
                                    .toList();

                            // 先清理索引表，再清理工具包表
                            return CompletableFutureUtils.sequentialMap(removes, indexer::remove)
                                    .thenApply(uuu -> {
                                        bundles.removeAll(removes);
                                        return bundle;
                                    });

                        })

                        // 更新所有已加载的数据
                        .thenCompose(uu -> {
                            bundles.add(bundle);
                            return indexer.upsert(bundle);
                        }));
            });
        }
        return stage;
    }

    @Override
    public CompletionStage<List<ToolUse>> lookupByIntent(String intent) {
        return indexer.query(intent)
                .thenApply(names -> {

                    /*
                     * 从索引查询出来的names有可能是脏数据，
                     * 需要从工具包中过滤掉
                     */
                    final var result = new ArrayList<ToolUse>();
                    for (final var name : names) {
                        final var lookupOpt = lookupByName(name);
                        if (lookupOpt.isPresent()) {
                            result.add(lookupOpt.get());
                        } else {
                            logger.warn("{} found bad index, will be removed. tool={}", this, name);
                            indexer.remove(name);
                        }
                    }

                    return result;
                });
    }

    @Override
    public Optional<ToolUse> lookupByName(String name) {
        Objects.requireNonNull(name, "name can't be null!");
        return lookupAll()
                .stream()
                .filter(use -> Objects.equals(name, use.tool().meta().name()))
                .findFirst();
    }

    @Override
    public List<ToolUse> lookupAll() {
        return bundles.stream()
                .map(Bundle::uses)
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public void close() {
        subscriptions.forEach(Subscription::unsubscribe);
        loaders.forEach(IOUtils::closeQuietly);
        IOUtils.closeQuietly(indexer);
        logger.debug("{} closed.", this);
    }

    private class Syncer extends Thread {

        private final Object superThis = HashMapToolbox.this;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition waiting = lock.newCondition();
        private final Set<ToolLoader> queue = new ConcurrentHashMap<ToolLoader, Object>().keySet();

        private final Duration syncInterval;


        private Syncer(Duration syncInterval) {
            this.syncInterval = syncInterval;
            setDaemon(true);
            setName("dashscope4j-agent:/toolbox/syncer");
        }

        /**
         * 通知同步
         *
         * @param loader 待同步的Loader
         */
        public void notifySync(ToolLoader loader) {
            lock.lock();
            try {
                queue.add(loader);
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
                            if (queue.isEmpty()) {
                                continue;
                            }
                        }
                    } finally {
                        lock.unlock();
                    }

                    // 开始执行同步
                    final var syncIt = queue.iterator();
                    while (syncIt.hasNext()) {

                        final var loader = syncIt.next();

                        try {
                            reload(List.of(loader))
                                    .toCompletableFuture()
                                    .join();
                            syncIt.remove();
                        } catch (Throwable ex) {
                            final var cause = CompletableFutureUtils.unwrapEx(ex);
                            logger.warn("{} sync loader failed, will be retry after {}ms! loader={};",
                                    superThis,
                                    loader,
                                    syncInterval.toMillis(),
                                    cause
                            );
                        }

                    }

                } catch (InterruptedException ieEx) {
                    interrupt();
                    break;
                }

            }// while
            logger.debug("{} sync stopped.", superThis);

        }

    }


    public static class Builder implements Buildable<HashMapToolbox, Builder> {

        private ToolIndexer indexer;
        private List<ToolLoader> loaders;
        private Duration syncInterval = Duration.ofMinutes(5);

        public Builder indexer(ToolIndexer indexer) {
            this.indexer = indexer;
            return this;
        }

        public Builder loaders(List<ToolLoader> loaders) {
            this.loaders = loaders;
            return this;
        }

        public Builder loaders(UnaryOperator<List<ToolLoader>> operator) {
            this.loaders = operator.apply(CommonUtils.mutableCopy(this.loaders));
            return this;
        }

        public Builder syncInterval(Duration syncInterval) {
            this.syncInterval = syncInterval;
            return this;
        }

        @Override
        public HashMapToolbox build() {

            try {
                return buildAsync()
                        .toCompletableFuture()
                        .join();
            } catch (Throwable ex) {
                final var cause = CompletableFutureUtils.unwrapEx(ex);
                throw new IllegalStateException("Build HashMapToolbox occur error!", cause);
            }

        }

        public CompletionStage<HashMapToolbox> buildAsync() {
            //noinspection resource
            return new HashMapToolbox(this)
                    .init()
                    .exceptionallyCompose(ex -> {
                        final var cause = CompletableFutureUtils.unwrapEx(ex);
                        return CompletableFuture.failedStage(new IllegalStateException("Build HashMapToolbox occur error!", cause));
                    });
        }

    }

}
