package io.github.oldmanpushcart.dashscope4j.base.cache;

/**
 * 缓存工厂
 * <p>
 * 不建议直接使用，因为{@link Cache}无法很好的被实现持久化方案。
 * 请使用{@link PersistentCacheFactory}代替
 * </p>
 *
 * @since 1.4.2
 */
@Deprecated
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
