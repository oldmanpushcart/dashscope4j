package io.github.oldmanpushcart.dashscope4j.agent.toolbox;

import io.github.oldmanpushcart.dashscope4j.agent.toolbox.indexer.ToolIndexer;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.ToolSource;
import io.github.oldmanpushcart.dashscope4j.agent.toolbox.source.toolkit.ToolkitToolSource;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.util.Buildable;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils.illegalStateStage;

/**
 * 基于 {@link HashMap} 的工具箱实现
 */
public class HashMapToolbox implements Toolbox {

    private final ToolIndexer indexer;
    private final Syncer syncer;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CompletableFuture<?> closeF = new CompletableFuture<>();
    private final Map<String, Entity> entities = new ConcurrentHashMap<>();
    private final Set<ToolSubscription> subscriptions = ConcurrentHashMap.newKeySet();

    private HashMapToolbox(Builder builder) {
        this.indexer = builder.indexer;
        this.syncer = new Syncer(builder.syncInterval).begin();
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/toolbox";
    }

    @Override
    public CompletionStage<? extends ToolSubscription> subscribe(ToolSource source) {

        /*
         * 加载加载器并建立后续的订阅关系
         */
        return reload(source)

                // 加载成功，建立订阅关系
                .thenApply(u -> {

                    // 创建订阅关系
                    final var subscription = new ToolSubscriptionImpl(source);

                    // 建立订阅关系
                    subscriptions.add(subscription.subscribe());
                    return subscription;

                })

                /*
                 * 如果加载失败，则主动进行卸载
                 * 并明确异常信息
                 */
                .exceptionallyCompose(ex -> {
                    unload(source);
                    return illegalStateStage(ex, "Subscribe failed: loading from source occur error!");
                })
                ;

    }

    @Override
    public CompletionStage<? extends ToolSubscription> subscribe(Tool tool) {
        return subscribe(List.of(tool));
    }

    @Override
    public CompletionStage<? extends ToolSubscription> subscribe(Iterable<? extends Tool> it) {
        final var toolkitTs = ToolkitToolSource.newBuilder()
                .building(builder -> it.forEach(builder::append))
                .build();
        return subscribe(toolkitTs)
                .thenApply(sub ->
                        new ToolSubscription() {

                            @Override
                            public ToolSource source() {
                                return sub.source();
                            }

                            @Override
                            public boolean isClosed() {
                                return sub.isClosed();
                            }

                            @Override
                            public void close() {
                                sub.close();
                                toolkitTs.close();
                            }

                        });
    }


    /**
     * 重新加载工具源（异步）
     *
     * @param source 工具源
     * @return 加载结果回调
     */
    private CompletionStage<Void> reload(ToolSource source) {

        // 卸载加载器
        unload(source);

        // 重新推入工具箱
        final var tools = source.tools();
        tools.forEach(tool -> entities.put(tool.meta().name(), Entity.of(source, tool)));

        // 计算工具索引
        return CompletableFutureUtils
                .allOf(tools.stream().map(indexer::upsert).toList());
    }

    /**
     * 卸载工具源
     * <p>
     * 这里仅涉及对工具实体数据和索引数据的清理，
     * 不涉及订阅关系的改变。
     * </p>
     *
     * @param source 工具源
     */
    private void unload(ToolSource source) {

        // 先找到由loader所引入的所有工具名称
        final var removeNames = entities.values()
                .stream()
                .filter(entity -> entity.source == source)
                .map(Entity::name)
                .collect(Collectors.toSet());

        // 再根据工具名称去清理所有的工具和工具索引
        removeNames.forEach(name -> {
            indexer.remove(name);
            entities.remove(name);
        });

    }

    @Override
    public boolean isClosed() {
        return closeF.isDone();
    }

    @Override
    public void close() {
        if (closeF.complete(null)) {
            syncer.interrupt();
            new ArrayList<>(subscriptions)
                    .forEach(ToolSubscription::close);
        }
    }

    @Override
    public CompletionStage<List<Tool>> lookupByIntent(String intent) {
        return indexer.query(intent)
                .thenApply(names -> {
                    final List<Tool> result = new ArrayList<>();
                    names.forEach(name -> {
                        final var lookupOpt = lookupByName(name);
                        if (lookupOpt.isPresent()) {
                            result.add(lookupOpt.get());
                        } else {
                            logger.warn("{} found bad index, remove it. tool={}", this, name);
                            indexer.remove(name);
                        }
                    });
                    return result;
                })
                .exceptionallyCompose(ex -> illegalStateStage(ex, "Lookup tools by intent failed!"));
    }

    @Override
    public Optional<Tool> lookupByName(String name) {

        // entities类型是ConcurrentHashMap，要求确保key不为null
        if (null == name) {
            return Optional.empty();
        }

        return Optional.ofNullable(entities.get(name))
                .map(Entity::tool);
    }


    /**
     * 工具实体
     *
     * @param name   工具名称
     * @param tool   工具
     * @param source 工具源
     */
    private record Entity(String name, Tool tool, ToolSource source) {

        public static Entity of(ToolSource loader, Tool tool) {
            return new Entity(tool.meta().name(), tool, loader);
        }

    }

    /**
     * 工具订阅关系实现
     */
    private class ToolSubscriptionImpl implements ToolSubscription {

        private final ToolSource source;
        private final ToolSource.Listener listener;

        private final CompletableFuture<?> closeF = new CompletableFuture<>();

        private ToolSubscriptionImpl(ToolSource source) {
            this.source = source;
            this.listener = new ListenerImpl(this);
        }

        @Override
        public ToolSource source() {
            return source;
        }

        public ToolSubscriptionImpl subscribe() {
            source.addListener(listener);
            return this;
        }

        @Override
        public boolean isClosed() {
            return closeF.isDone();
        }

        @Override
        public void close() {

            /*
             * 关闭订阅关系流程
             * 1. 先解除订阅
             * 2. 再删除工具和索引数据
             * 3. 最后将订阅关系从订阅关系列表中删除
             */
            if (closeF.complete(null)) {
                source.removeListener(listener);
                unload(source);
                subscriptions.remove(this);
            }

        }

        /**
         * 订阅关系监听器实现
         */
        private class ListenerImpl implements ToolSource.Listener {

            private final ToolSubscription subscription;

            private ListenerImpl(ToolSubscription subscription) {
                this.subscription = subscription;
            }

            @Override
            public void onChanged() {
                syncer.sync(subscription);
            }

            @Override
            public void onClosed() {

                /*
                 * 这里只负责找到订阅关系，并关闭
                 * 具体清理工具、索引和订阅关系，由订阅关系关闭触发。
                 */
                subscription.close();

            }

        }

    }


    /**
     * 订阅关系同步器
     */
    private class Syncer extends Thread {

        private final Duration syncInterval;
        private final BlockingQueue<Object> waiting = new LinkedBlockingQueue<>();
        private final List<ToolSubscription> subscriptions = new ArrayList<>();

        private Syncer(Duration syncInterval) {
            this.syncInterval = syncInterval;
        }

        /**
         * 同步订阅关系
         *
         * @param subscription 订阅关系
         */
        public void sync(ToolSubscription subscription) {
            synchronized (this) {
                subscriptions.add(subscription);
            }
            waiting.add(this);
        }

        @Override
        public void run() {

            logger.debug("{}/syncer start.", this);
            while (!isInterrupted()) {
                try {
                    waiting.poll(syncInterval.toMillis(), TimeUnit.MILLISECONDS);
                    new ArrayList<>(subscriptions)
                            .forEach(subscription -> {

                                // 如果已关闭，则从等待更新集合中移除
                                if (subscription.isClosed()) {
                                    synchronized (this) {
                                        subscriptions.remove(subscription);
                                    }
                                    return;
                                }

                                // 进行同步
                                reload(subscription.source())
                                        .whenComplete((u, ex) -> {
                                            if (null == ex) {
                                                synchronized (this) {
                                                    subscriptions.remove(subscription);
                                                }
                                            } else {
                                                logger.warn("{}/syncer syncing subscription occur error!", this, ex);
                                            }
                                        })
                                        .toCompletableFuture()
                                        .join();

                            });
                } catch (InterruptedException iEx) {
                    logger.debug("{}/syncer interrupted.", this);
                    interrupt();
                    break;
                } catch (Throwable t) {
                    logger.warn("{}/syncer occur error!", this, t);
                }
            }//while
            logger.debug("{}/syncer stopped.", this);
        }

        /**
         * 启动
         *
         * @return this
         */
        public Syncer begin() {
            start();
            return this;
        }

    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * 构建器
     */
    public static class Builder implements Buildable<HashMapToolbox, Builder> {

        private ToolIndexer indexer;
        private Duration syncInterval = Duration.ofSeconds(5);

        /**
         * 设置工具索引
         *
         * @param indexer 工具索引
         * @return this
         */
        public Builder indexer(ToolIndexer indexer) {
            this.indexer = indexer;
            return this;
        }

        /**
         * 设置同步间隔
         * <p>
         * 工具源发生变更时会立即同步到工具箱，
         * 但如果同步失败后，就得依靠这个同步间隔来定期检查还有哪些未完成的同步，持续的进行同步操作，直至订阅取消。
         * </p>
         *
         * @param syncInterval 同步间隔
         * @return this
         */
        public Builder syncInterval(Duration syncInterval) {
            this.syncInterval = syncInterval;
            return this;
        }

        @Override
        public HashMapToolbox build() {
            return new HashMapToolbox(this);
        }

    }

}
