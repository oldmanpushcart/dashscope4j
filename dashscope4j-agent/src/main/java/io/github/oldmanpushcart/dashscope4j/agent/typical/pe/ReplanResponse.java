package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Replan 重规划响应（JSON 格式）
 *
 * @param thought  思考过程
 * @param newTasks 新任务列表
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ReplanResponse(
        String thought,
        List<ReplanTaskItem> newTasks
) {
    
    /**
     * 重规划任务项
     *
     * @param description 任务描述
     * @param reason      调整原因
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ReplanTaskItem(
            String description,
            String reason
    ) {
    }
}
