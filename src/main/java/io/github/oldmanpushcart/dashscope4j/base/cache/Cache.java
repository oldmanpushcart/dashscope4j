package io.github.oldmanpushcart.dashscope4j.base.cache;

import java.time.Duration;

/**
 * 缓存
 */
public interface Cache extends Iterable<Cache.Entry> {

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
    String get(String key);

    /**
     * 存储值
     *
     * @param key   KEY
     * @param value VAL
     * @return 是否成功
     */
    boolean put(String key, String value);

    /**
     * 存储值
     *
     * @param key      KEY
     * @param value    VAL
     * @param duration 有效时长
     * @return 是否成功
     */
    boolean put(String key, String value, Duration duration);

    /**
     * 移除值
     *
     * @param key KEY
     * @return VALUE
     */
    String remove(String key);

    /**
     * 清空缓存
     *
     * @return 清空数量
     */
    int clear();

    /**
     * 缓存数据项
     */
    interface Entry {

        /**
         * @return KEY
         */
        String key();

        /**
         * @return VAL
         */
        String value();

        /**
         * @return 是否过期
         */
        boolean isExpired();

    }

}
