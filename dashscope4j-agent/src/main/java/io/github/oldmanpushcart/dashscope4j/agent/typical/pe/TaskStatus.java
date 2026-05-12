package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

/**
 * 任务状态枚举
 */
public enum TaskStatus {
    
    /**
     * 待执行
     */
    PENDING,
    
    /**
     * 执行中
     */
    RUNNING,
    
    /**
     * 执行成功
     */
    SUCCESS,
    
    /**
     * 执行失败
     */
    FAILED,
    
    /**
     * 已跳过（Replan 时被移除或合并）
     */
    SKIPPED
}
