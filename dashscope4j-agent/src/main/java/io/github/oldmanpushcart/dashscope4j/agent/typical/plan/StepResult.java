package io.github.oldmanpushcart.dashscope4j.agent.typical.plan;

/**
 * 步骤执行结果
 *
 * @param seq     步骤序号
 * @param status  执行状态
 * @param output  执行结果输出
 * @param error   异常信息(失败时)
 */
record StepResult(
        int seq,
        StepStatus status,
        String output,
        Throwable error
) {
    
    /**
     * 创建成功的执行结果
     *
     * @param seq    步骤序号
     * @param output 执行结果
     * @return 执行结果对象
     */
    public static StepResult success(int seq, String output) {
        return new StepResult(seq, StepStatus.COMPLETED, output, null);
    }
    
    /**
     * 创建失败的执行结果
     *
     * @param seq   步骤序号
     * @param error 异常信息
     * @return 执行结果对象
     */
    public static StepResult failed(int seq, Throwable error) {
        return new StepResult(seq, StepStatus.FAILED, null, error);
    }
    
    /**
     * 检查是否成功
     *
     * @return 是否成功
     */
    public boolean isSuccess() {
        return status == StepStatus.COMPLETED;
    }
}
