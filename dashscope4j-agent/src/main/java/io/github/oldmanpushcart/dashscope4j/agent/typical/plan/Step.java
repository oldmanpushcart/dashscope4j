package io.github.oldmanpushcart.dashscope4j.agent.typical.plan;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * 执行步骤
 * <p>
 * 表示计划中的一个可执行步骤，每个步骤必须有明确的工具依赖和输入输出。
 * </p>
 *
 * @param seq             步骤序号(从1开始)
 * @param description     步骤描述
 * @param expectedOutput  期望输出格式说明
 * @param inputFrom       输入来源(前置步骤的seq, null表示使用原始任务)
 * @param configOverrides 配置覆盖项(可选,用于覆盖ReActAgent的默认配置)
 */
record Step(
        @JsonProperty("seq") int seq,
        @JsonProperty("description") String description,
        @JsonProperty("expectedOutput") String expectedOutput,
        @JsonProperty("inputFrom") Integer inputFrom,
        @JsonProperty("configOverrides") Map<String, Object> configOverrides
) {
    
    /**
     * 检查是否有配置覆盖
     *
     * @return 是否有配置覆盖
     */
    public boolean hasConfigOverrides() {
        return configOverrides != null && !configOverrides.isEmpty();
    }
}
