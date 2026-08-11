package io.github.oldmanpushcart.dashscope4j.agent.plugin;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * 插件接口
 */
public interface Plugin {

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

    /**
     * 在Agent启动过程中运行
     *
     * @param agent Agent实例
     * @return 插件扩展
     */
    Extension install(Agent agent);

    /**
     * 在Agent关闭过程中运行
     */
    void uninstall();

    /**
     * 插件扩展接口
     */
    interface Extension {

        /**
         * 获取所属插件
         *
         * @return 插件实例
         */
        Plugin plugin();

        /**
         * 获取拦截器
         *
         * @param phases 拦截阶段
         * @return 拦截器列表
         */
        List<ChatInterceptor> interceptors(Phases phases);

    }

}
