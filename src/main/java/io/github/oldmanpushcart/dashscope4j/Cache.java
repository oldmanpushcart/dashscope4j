package io.github.oldmanpushcart.dashscope4j;

import java.io.Closeable;
import java.time.Instant;
import java.util.Optional;

/**
 * 缓存接口
 */
public interface Cache extends Closeable {

    /**
     * 获取缓存
     *
     * @param namespace 命名空间
     * @param key       KEY
     * @return 缓存条目，如果缓存不存在则返回为{@code null}
     */
    Optional<Entry> get(String namespace, String key);

    /**
     * 添加缓存条目
     * <p>如果缓存已存在则被替换，并返回被替换的缓存条目</p>
     *
     * @param namespace 命名空间
     * @param key       KEY
     * @param payload   缓存内容
     * @return 被替换的缓存条目，如果缓存不存在则返回为{@code null}
     */
    Optional<Entry> put(String namespace, String key, byte[] payload);

    /**
     * 添加缓存条目
     * <p>如果缓存已存在则被替换，并返回被替换的缓存条目</p>
     *
     * @param namespace 命名空间
     * @param key       KEY
     * @param payload   缓存内容
     * @param expireAt  过期时间
     * @return 被替换的缓存条目，如果缓存不存在则返回为{@code null}
     */
    Optional<Entry> put(String namespace, String key, byte[] payload, Instant expireAt);

    /**
     * 删除缓存条目
     *
     * @param namespace 命名空间
     * @param key       KEY
     * @return 被删除的缓存条目，如果缓存不存在则返回为{@code null}
     */
    Optional<Entry> remove(String namespace, String key);

    /**
     * 缓存条目
     */
    interface Entry {

        /**
         * @return 命名空间
         */
        String namespace();

        /**
         * @return KEY
         */
        String key();

        /**
         * @return 缓存内容
         */
        byte[] payload();

        /**
         * @return 是否过期
         */
        boolean isExpired();

        /**
         * @return 过期时间
         */
        Instant expireAt();

    }

}
