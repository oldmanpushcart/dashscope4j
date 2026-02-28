package io.github.oldmanpushcart.dashscope4j.client.util.tracer;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 调用链追踪器接口，用于追踪方法调用链路信息
 */
public interface Tracer {

    static Tracer getDefault() {
        return StdTracer.INSTANCE;
    }

    /**
     * 默认的追踪器实例
     */
    Tracer instance = getDefault();

    /**
     * 进入指定名称的追踪作用域
     *
     * @param name 作用域名称
     * @return 追踪作用域对象
     */
    Scope enter(String name);

    /**
     * 获取当前追踪跨度
     *
     * @return 当前追踪跨度，如果不存在则返回空
     */
    Optional<Span> current();

    /**
     * 追踪跨度接口，表示一次操作的追踪信息
     */
    interface Span {

        /**
         * 获取根跨度
         *
         * @return 根跨度
         */
        Span root();

        /**
         * 获取父跨度
         *
         * @return 父跨度，如果是根跨度则返回null
         */
        Span parent();

        /**
         * 获取追踪ID
         *
         * @return 追踪ID
         */
        String traceId();

        /**
         * 获取跨度ID
         *
         * @return 跨度ID
         */
        int spanId();

        /**
         * 获取跨度名称
         *
         * @return 跨度名称
         */
        String name();

        /**
         * 获取属性映射
         *
         * @return 属性映射
         */
        Map<String, String> properties();

        /**
         * 获取时间戳
         *
         * @return 时间戳
         */
        Instant timestamp();

        /**
         * 判断是否为根跨度
         *
         * @return 如果是根跨度则返回true，否则返回false
         */
        boolean isRoot();

        /**
         * 判断是否已终止
         *
         * @return 如果已终止则返回true，否则返回false
         */
        boolean isTerminated();

        /**
         * 判断是否已结束
         *
         * @return 如果已结束则返回true，否则返回false
         */
        boolean isEnd();

        /**
         * 获取跨度开始时间
         *
         * @return 跨度开始时间
         */
        Instant beginAt();

        /**
         * 获取跨度结束时间
         *
         * @return 跨度结束时间
         */
        Instant endAt();

        /**
         * 获取跨度持续时间
         *
         * @return 跨度持续时间
         */
        Duration duration();

        /**
         * 获取自身跨度引用
         *
         * @return 自身跨度引用
         */
        Span self();

        /**
         * 设置属性
         *
         * @param key   属性键
         * @param value 属性值
         * @return 当前跨度对象
         */
        Span property(String key, String value);

        /**
         * 设置状态
         *
         * @param status 状态
         * @return 当前跨度对象
         */
        Span status(Status status);

        /**
         * 标记成功状态
         *
         * @return 当前跨度对象
         */
        Span success();

        /**
         * 标记失败状态
         *
         * @return 当前跨度对象
         */
        Span failure();

        /**
         * 标记异常失败状态
         *
         * @param t 异常
         * @return 当前跨度对象
         */
        Span failure(Throwable t);

        /**
         * 获取状态
         *
         * @return 状态
         */
        Status status();

        /**
         * 创建子跨度
         *
         * @param name 子跨度名称
         * @return 新的子跨度
         */
        Span newChild(String name);

        /**
         * 跨度状态枚举
         */
        enum Status {
            /**
             * 待处理状态
             */
            PENDING,
            /**
             * 成功状态
             */
            SUCCESS,
            /**
             * 失败状态
             */
            FAILURE
        }

        /**
         * 创建根跨度
         *
         * @param name 跨度名称
         * @return 根跨度实例
         */
        static Span ofRoot(String name) {
            final var traceId = UUID.randomUUID().toString().replace("-", "");
            final var indexer = new AtomicInteger();
            final var spanId = indexer.getAndIncrement();
            return new StdSpan(null, null, traceId, spanId, name, indexer);
        }

    }

    /**
     * 追踪作用域接口，实现了AutoCloseable，用于自动管理追踪上下文
     */
    interface Scope extends AutoCloseable {

        /**
         * 获取当前作用域关联的跨度
         *
         * @return 关联的跨度
         */
        Span span();

        /**
         * 恢复到之前的作用域状态
         *
         * @return 恢复后的跨度
         */
        Span restore();


        /**
         * 关闭作用域，清理追踪上下文
         */
        @Override
        void close();

    }

    /**
     * 注册追踪监听器
     *
     * @param listener 监听器
     */
    void registerListener(Listener listener);

    /**
     * 注销追踪监听器
     *
     * @param listener 监听器
     */
    void unregisterListener(Listener listener);

    /**
     * 追踪监听器接口，用于监听跨度事件
     */
    interface Listener {

        /**
         * 监听跨度事件
         *
         * @param span 跨度
         */
        void onSpan(Span span);

    }

}
