package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 任务评估响应（JSON 格式）
 *
 * @param isSuccess 是否成功
 * @param reason    评估原因
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskEvaluationResponse(
        @JsonProperty("isSuccess") boolean isSuccess,
        @JsonProperty("reason") String reason
) {
}
