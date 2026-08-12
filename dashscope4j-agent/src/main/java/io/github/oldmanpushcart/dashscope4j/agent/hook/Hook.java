package io.github.oldmanpushcart.dashscope4j.agent.hook;

import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

/**
 * 智能体钩子
 * <p>
 * Hook 是智能体交互过程中的扩展机制，作为所有钩子的顶层抽象。
 * 钩子本身不定义任何方法，而是通过子接口按阶段划分职责，
 * 在智能体处理请求的不同阶段注入 {@link ChatInterceptor} 来拦截和增强行为。
 * </p>
 *
 * <p>
 * 阶段划分
 * <ul>
 *     <li>{@link PreparationHook} — 准备阶段钩子，在请求发起前执行，用于注入参数设置、上下文装配等拦截器</li>
 *     <li>{@link InteractionHook} — 交互阶段钩子，在请求交互过程中执行，用于注入会话记录、流式处理等拦截器</li>
 * </ul>
 * </p>
 *
 * @see PreparationHook 准备阶段钩子
 * @see InteractionHook 交互阶段钩子
 */
public interface Hook {
}
