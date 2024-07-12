package io.github.oldmanpushcart.dashscope4j.base.interceptor;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 拦截器上下文
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

    /**
     * @return 附件集合
     */
    Map<String, Object> attachmentMap();

}
