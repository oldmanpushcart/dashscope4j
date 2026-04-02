package io.github.oldmanpushcart.dashscope4j.agent.repository.memory.typical;

import io.github.oldmanpushcart.dashscope4j.agent.repository.memory.Memory;

/**
 * 工作记忆
 * <p>
 * 短期记忆，用于维持当前任务的上下文。
 * </p>
 * <p>
 * 例如，最近的几轮对话、当前工具调用的结果缓存。
 * </p>
 */
public interface WorkingMemory extends Memory {
}
