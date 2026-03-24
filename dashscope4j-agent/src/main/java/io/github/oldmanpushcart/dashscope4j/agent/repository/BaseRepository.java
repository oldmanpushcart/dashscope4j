package io.github.oldmanpushcart.dashscope4j.agent.repository;

import io.github.oldmanpushcart.dashscope4j.agent.storage.Storage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class BaseRepository<K, E> implements Repository<K, E> {

    private final String name;
    private final Repository.Updater<K, E> updater;
    private final Repository.Indexer<K, E> indexer;
    private final Storage<K, E> storage;
    private final Repository.Loader<K, E> loader;
    private final String _toString;

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final CompletableFuture<Void> closeF = new CompletableFuture<>();

    protected BaseRepository(String name, Indexer<K, E> indexer, Storage<K, E> storage, Loader<K, E> loader) {
        this.name = name;
        this.indexer = indexer;
        this.storage = storage;
        this.loader = loader;
        this.updater = new Updater();
        this._toString = "dashscope4j-agent:/repository/%s".formatted(name);
    }

    @Override
    public CompletionStage<Repository<K, E>> initialize() {
        return CompletableFuture.<Void>completedStage(null)
                .thenCompose(u -> storage.init())
                .thenCompose(u -> indexer.init())
                .thenCompose(u -> loader.init(updater))
                .thenApply(u -> (Repository<K, E>) this)
                .whenComplete((u, ex) -> {
                    if (ex != null) {
                        logger.warn("{} initialize failed!", this, ex);

                        /*
                         * 初始化失败，则需要主动关闭，避免资源泄露。
                         */
                        close();

                    } else {
                        logger.debug("{} initialized.", this);
                    }
                });
    }

    @Override
    public String toString() {
        return _toString;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public CompletionStage<Map<K, E>> lookup(UserMessage instant) {
        return indexer.lookup(instant)
                .thenCompose(keys -> {

                    // 从索引中查询的是 KEY 集合，需要通过 KEYS 去存储中回表获取数据
                    CompletionStage<Map<K, E>> stage = CompletableFuture.completedStage(new ConcurrentHashMap<>());
                    for (K key : keys) {
                        stage = stage.thenCompose(map -> storage.get(key)
                                .thenApply(item -> {
                                    if (item != null) {
                                        map.put(key, item);
                                    } else {
                                        indexer.remove(key);
                                    }
                                    return map;
                                }));
                    }

                    return stage;
                });
    }

    @Override
    public CompletionStage<E> lookupByKey(K key) {
        return storage.get(key);
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

        try {
            indexer.close();
            logger.debug("{} close indexer normally.", this);
        } catch (Exception ex) {
            logger.warn("{} close indexer failed!", this, ex);
        }

        try {
            storage.close();
            logger.debug("{} close storage normally.", this);
        } catch (Exception e) {
            logger.warn("{} close storage failed!", this, e);
        }

        try {
            loader.close();
            logger.debug("{} close loader normally.", this);
        } catch (Exception e) {
            logger.warn("{} close loader failed!", this, e);
        }

        logger.debug("{} closed.", this);

    }

    /**
     * 仓库更新器
     */
    private class Updater implements Repository.Updater<K, E> {

        /**
         * 插入或更新数据
         * <p>
         * 先存储数据，再索引数据。
         * 这样任何一个失败都不会影响数据的可见性。
         * </p>
         *
         * @param key  主键
         * @param item 数据
         * @return 操作回调
         */
        @Override
        public CompletionStage<Void> upsert(K key, E item) {
            return storage.upsert(key, item)
                    .thenCompose(u -> indexer.upsert(key, item))
                    .whenComplete((u, ex) -> {
                        if (ex != null) {
                            logger.warn("{}/{} upsert failed!", BaseRepository.this, key, ex);
                        } else {
                            logger.debug("{}/{} upsert success.", BaseRepository.this, key);
                        }
                    });
        }

        /**
         * 删除数据
         * <p>
         * 先索引数据，再存储数据。
         * 这样任何一个失败都不会影响数据的可见性。
         * </p>
         *
         * @param key 主键
         * @return 操作回调
         */
        @Override
        public CompletionStage<Void> remove(K key) {
            return indexer.remove(key)
                    .thenCompose(u -> storage.remove(key))
                    .whenComplete((u, ex) -> {
                        if (ex != null) {
                            logger.warn("{}/{} remove failed!", BaseRepository.this, key, ex);
                        } else {
                            logger.debug("{}/{} remove success.", BaseRepository.this, key);
                        }
                    });
        }

    }

}
