package io.github.oldmanpushcart.dashscope4j.base.cache;

import java.time.Duration;

/**
 * 缓存
 *
 * @param <K> KEY 类型
 * @param <V> VAL 类型
 * @since 1.4.2
 */
public interface Cache<K, V> extends Iterable<Cache.Entry<K, V>> {

    /**
     * @return 命名空间
     */
    String namespace();

    /**
     * 获取值
     *
     * @param key KEY
     * @return VAL
     */
    V get(K key);

    /**
     * 存储值
     *
     * @param key   KEY
     * @param value VAL
     * @return 是否成功
     */
    boolean put(K key, V value);

    /**
     * 存储值
     *
     * @param key      KEY
     * @param value    VAL
     * @param duration 有效时长
     * @return 是否成功
     */
    boolean put(K key, V value, Duration duration);

    /**
     * 移除值
     *
     * @param key KEY
     * @return VALUE
     */
    V remove(K key);

    /**
     * 清空缓存
     *
     * @return 清空数量
     */
    int clear();

    /**
     * 缓存数据项
     *
     * @param <K> KEY 类型
     * @param <V> VAL 类型
     */
    interface Entry<K, V> {

        /**
         * @return KEY
         */
        K key();

        /**
         * @return VAL
         */
        V value();

        /**
         * @return 是否过期
         */
        boolean isExpired();

    }

}
