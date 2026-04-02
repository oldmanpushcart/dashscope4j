package io.github.oldmanpushcart.dashscope4j.agent.repository.memory.typical;

import io.github.oldmanpushcart.dashscope4j.agent.repository.memory.Memory;

/**
 * 语义记忆
 * <p>
 * 从情景记忆中提炼出的通用知识和事实。
 * </p>
 * <p>
 * 例如，“用户偏好简洁的代码风格”、“支付前必须进行幂等性校验”。它脱离了具体事件，是抽象的认知。
 * </p>
 */
public interface SemanticMemory extends Memory {
}
