package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Plan 生成响应（JSON 格式）
 *
 * @param thought 思考过程
 * @param tasks   任务列表
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlanResponse(
        String thought,
        List<TaskItem> tasks
) {
    
    /**
     * 任务项
     *
     * @param taskId      任务ID
     * @param description 任务描述
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TaskItem(
            String taskId,
            String description
    ) {
    }
}
