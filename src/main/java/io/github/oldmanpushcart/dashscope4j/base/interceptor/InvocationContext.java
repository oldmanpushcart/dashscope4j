package io.github.oldmanpushcart.dashscope4j.base.interceptor;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;

import java.util.concurrent.Executor;

/**
 * 拦截器上下文
 *
 * @since 1.4.0
 */
public interface InvocationContext {

    /**
     * @return 客户端
     */
    DashScopeClient client();

    /**
     * @return 线程池
     */
    Executor executor();

}
