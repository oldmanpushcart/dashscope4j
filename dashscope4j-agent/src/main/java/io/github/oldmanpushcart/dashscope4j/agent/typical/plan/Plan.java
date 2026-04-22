package io.github.oldmanpushcart.dashscope4j.agent.typical.plan;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务计划
 * <p>
 * 表示一个完整的任务执行计划，包含多个有序的步骤。
 * </p>
 */
final class Plan {

    private final String planId;
    private final String originalTask;
    private final List<Step> steps;
    private final Map<Integer, String> stepOutputs = new ConcurrentHashMap<>();

    public Plan(String planId, String originalTask, List<Step> steps) {
        this.planId = planId;
        this.originalTask = originalTask;
        this.steps = steps;
    }

    public String planId() {
        return planId;
    }

    public String originalTask() {
        return originalTask;
    }

    public List<Step> steps() {
        return steps;
    }

    /**
     * 保存步骤执行结果
     *
     * @param seq    步骤序号
     * @param output 执行结果
     */
    public void saveStepOutput(int seq, String output) {
        stepOutputs.put(seq, output);
    }

    /**
     * 获取步骤执行结果
     *
     * @param seq 步骤序号
     * @return 执行结果,不存在返回null
     */
    public String getStepOutput(int seq) {
        return stepOutputs.get(seq);
    }

    /**
     * 格式化前置步骤输出(用于传递给当前步骤)
     *
     * @param currentSeq 当前步骤序号
     * @return 格式化后的前置步骤输出字符串
     */
    public String formatPrefixOutputs(int currentSeq) {
        final var sb = new StringBuilder();
        for (int seq = 1; seq < currentSeq; seq++) {
            final var output = getStepOutput(seq);
            sb.append("""
                    ### Step %d
                    %s
                    
                    """.formatted(seq, output));
        }
        return sb.toString();
    }

    /**
     * 格式化所有步骤结果(用于最终聚合)
     *
     * @return 格式化后的所有步骤结果字符串
     */
    public String formatAllStepOutputs() {
        final var sb = new StringBuilder();
        for (int seq = 1; seq <= steps.size(); seq++) {
            final var output = getStepOutput(seq);
            sb.append("""
                    ### Step-%d
                    %s
                    
                    """.formatted(seq, output));
        }
        return sb.toString();
    }
}
