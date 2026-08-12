package io.github.oldmanpushcart.dashscope4j.agent.hook;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.List;

/**
 * 交互阶段钩子
 */
public interface InteractionHook extends Hook {

    /**
     * 获取交互阶段拦截器列表
     *
     * @param agent 智能体
     * @return 交互阶段拦截器列表
     */
    List<? extends ChatInterceptor> onInteraction(Agent agent);

}
