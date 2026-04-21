package io.github.oldmanpushcart.dashscope4j.agent.typical.plan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * 任务计划
 * <p>
 * 表示一个完整的任务执行计划，包含多个有序的步骤。
 * </p>
 *
 * @param planId        计划ID
 * @param originalTask  原始任务描述
 * @param steps         步骤列表
 * @param context       全局上下文(用于Step间数据传递)
 */
record Plan(
        @JsonProperty("planId") String planId,
        @JsonProperty("originalTask") String originalTask,
        @JsonProperty("steps") List<Step> steps,
        @JsonProperty("context") Map<String, Object> context
) {
}
