package io.github.oldmanpushcart.dashscope4j.agent.typical.plan;

/**
 * 步骤执行状态
 */
enum StepStatus {
    /**
     * 待执行
     */
    PENDING,
    
    /**
     * 执行中
     */
    EXECUTING,
    
    /**
     * 已完成
     */
    COMPLETED,
    
    /**
     * 执行失败
     */
    FAILED,
    
    /**
     * 已跳过
     */
    SKIPPED
}
