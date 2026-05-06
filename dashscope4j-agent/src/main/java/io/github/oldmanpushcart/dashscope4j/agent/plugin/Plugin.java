package io.github.oldmanpushcart.dashscope4j.agent.plugin;

import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.List;

/**
 * 插件接口
 */
public interface Plugin {

    /**
     * 获取拦截器
     *
     * @param phases 拦截阶段
     * @return 拦截器
     */
    List<ChatInterceptor> interceptors(Phases phases);

    /**
     * 拦截阶段
     */
    enum Phases {

        /**
         * 预处理阶段
         */
        PREPARATION,

        /**
         * 交互阶段
         */
        INTERACTION

    }

}
