package io.github.oldmanpushcart.dashscope4j.agent.hook;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.List;

/**
 * 准备阶段钩子
 */
public interface PreparationHook extends Hook {

    /**
     * 获取准备阶段拦截器列表
     *
     * @param agent 智能体
     * @return 准备阶段拦截器列表
     */
    List<? extends ChatInterceptor> onPreparation(Agent agent);

}
