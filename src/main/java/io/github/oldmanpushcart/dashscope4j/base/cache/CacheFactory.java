package io.github.oldmanpushcart.dashscope4j.base.cache;

/**
 * 缓存工厂
 */
public interface CacheFactory {

    /**
     * 创建缓存
     *
     * @param namespace 命名空间
     * @return 缓存
     */
    Cache make(String namespace);

}
