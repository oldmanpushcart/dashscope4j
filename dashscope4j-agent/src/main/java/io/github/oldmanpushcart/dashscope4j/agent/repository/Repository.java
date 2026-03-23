package io.github.oldmanpushcart.dashscope4j.agent.repository;

import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 仓库
 *
 * @param <K> 索引类型
 * @param <E> 数据类型
 */
public interface Repository<K, E> extends AutoCloseable {

    /**
     * 初始化仓库
     *
     * @return 初始化完成的异步回调
     */
    CompletionStage<Repository<K, E>> initialize();

    /**
     * @return 仓库名称
     */
    String name();

    /**
     * 根据意图查询匹配的项
     *
     * @param instant 用户意图
     * @return 匹配结果回调
     */
    CompletionStage<Map<K, E>> lookup(UserMessage instant);

    /**
     * 根据主键获取数据
     *
     * @param key 主键
     * @return 数据回调
     */
    CompletionStage<E> lookupByKey(K key);

    /**
     * @return 是否已关闭
     */
    boolean isClosed();

    /**
     * 关闭仓库
     */
    @Override
    void close();

    /**
     * 数据更新器
     *
     * @param <K> 主键类型
     * @param <E> 数据类型
     */
    interface Updater<K, E> {

        /**
         * 插入或更新数据
         *
         * @param key  主键
         * @param item 数据
         * @return 操作回调
         */
        CompletionStage<Void> upsert(K key, E item);

        /**
         * 删除数据
         *
         * @param key 主键
         * @return 操作回调
         */
        CompletionStage<Void> remove(K key);

    }

    /**
     * 数据索引
     *
     * @param <K> 主键类型
     * @param <E> 数据类型
     */
    interface Indexer<K, E> extends Updater<K, E>, AutoCloseable {

        /**
         * 初始化
         *
         * @return 初始化完成的异步回调
         */
        CompletionStage<Void> init();

        /**
         * 根据意图查询匹配的项
         *
         * @param instant 用户意图
         * @return 匹配结果回调
         */
        CompletionStage<Set<K>> lookup(UserMessage instant);

    }

    /**
     * 数据存储
     *
     * @param <K> 主键类型
     * @param <E> 数据类型
     */
    interface Storer<K, E> extends Updater<K, E>, AutoCloseable {

        /**
         * 初始化
         *
         * @return 初始化完成的异步回调
         */
        CompletionStage<Void> init();

        /**
         * 根据主键获取数据
         *
         * @param key 主键
         * @return 数据回调
         */
        CompletionStage<E> get(K key);

    }

    /**
     * 数据加载器
     *
     * @param <K> 主键类型
     * @param <E> 数据类型
     */
    interface Loader<K, E> extends AutoCloseable {

        /**
         * 初始化
         *
         * @param updater 仓库更新器
         * @return 初始化完成的异步回调
         */
        CompletionStage<Void> init(Updater<K, E> updater);

        static <K, E> Loader<K, E> group(List<Loader<K, E>> loaders) {
            return new Loader<>() {

                @Override
                public CompletionStage<Void> init(Updater<K, E> updater) {
                    CompletionStage<Void> stage = CompletableFuture.completedStage(null);
                    for (final var loader : loaders) {
                        stage = stage.thenCompose(unused -> loader.init(updater));
                    }
                    return stage;
                }

                @Override
                public void close() {
                    loaders.forEach(loader -> {
                        try {
                            loader.close();
                        } catch (Exception e) {
                            // ignored...
                        }
                    });
                }

            };
        }

    }

}
