package io.github.oldmanpushcart.dashscope4j.base.cache;

/**
 * 持久化缓存工厂
 */
public interface PersistentCacheFactory {

    /**
     * 创建持久化缓存
     *
     * @param namespace 命名空间
     * @return 持久化缓存
     */
    PersistentCache make(String namespace);

}
