package io.github.oldmanpushcart.dashscope4j.agent.typical.plan;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 执行步骤
 * <p>
 * 表示计划中的一个可执行步骤，每个步骤必须有明确的工具依赖和输入输出。
 * </p>
 *
 * @param seq            步骤序号(从1开始)
 * @param description    步骤描述
 * @param expectedOutput 期望输出格式说明
 */
record Step(

        @JsonProperty("seq")
        int seq,

        @JsonProperty("description")
        String description,

        @JsonProperty("expectedOutput")
        String expectedOutput

) {
    
}
