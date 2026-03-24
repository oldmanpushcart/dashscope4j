package io.github.oldmanpushcart.dashscope4j.agent.storage;

import java.util.concurrent.CompletionStage;

/**
 * 数据存储
 *
 * @param <K> 主键类型
 * @param <E> 数据类型
 */
public interface Storage<K, E> extends AutoCloseable {

    /**
     * 初始化存储
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
