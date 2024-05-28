package io.github.oldmanpushcart.dashscope4j.base.cache;

/**
 * 缓存工厂
 *
 * @since 1.4.2
 */
public interface CacheFactory {

    /**
     * 创建缓存
     *
     * @param namespace 命名空间
     * @param <K>       KEY 类型
     * @param <V>       VAL 类型
     * @return 缓存
     */
    <K, V> Cache<K, V> make(String namespace);

}
