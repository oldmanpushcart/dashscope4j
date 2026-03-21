package io.github.oldmanpushcart.dashscope4j.agent.typical.react;

import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.util.HashMap;
import java.util.regex.Pattern;

/**
 * ReAct模式的响应记录
 * <p>
 * ReAct(Reasoning and Acting) 是一种将推理和行动交织在一起的模式，用于提升 LLM 解决复杂任务的能力。
 * 执行循环：Thought → Action → Action Input → Observation → ... → Final Answer
 * </p>
 *
 * @param thought     思考过程（可选）
 * @param action      要执行的动作（可选）
 * @param actionInput 动作的输入参数（可选）
 * @param observation 观察结果（可选）
 * @param finalAnswer 最终答案（可选）
 */
public record ReAct(
        String thought,
        String action,
        String actionInput,
        String observation,
        String finalAnswer
) {

    public boolean hasThought() {
        return CommonUtils.isNotBlankString(thought);
    }

    /**
     * @return 是否有 Action
     */
    public boolean hasAction() {
        return CommonUtils.isNotBlankString(action);
    }

    /**
     * @return 是否有 Final Answer
     */
    public boolean hasFinalAnswer() {
        return CommonUtils.isNotBlankString(finalAnswer);
    }

    // ==================== 常量定义 ====================

    /**
     * Thought 字段标识符
     */
    public static final String THOUGHT = "Thought";

    /**
     * Action 字段标识符
     */
    public static final String ACTION = "Action";

    /**
     * Action Input 字段标识符
     */
    public static final String ACTION_INPUT = "Action Input";

    /**
     * Observation 字段标识符
     */
    public static final String OBSERVATION = "Observation";

    /**
     * Final Answer 字段标识符
     */
    public static final String FINAL_ANSWER = "Final Answer";

    // ==================== 正则表达式 ====================

    /**
     * ReAct 解析正则表达式
     * <p>
     * 使用命名捕获组一次性匹配所有字段，格式为：FieldName: content
     * 匹配逻辑：
     * - (?i) 忽略大小写
     * - (?:...) 非捕获组
     * - (?<name>...) 命名捕获组
     * - [\\s\\S]*? 非贪婪匹配任意字符（包括换行）
     * - (?=...) 正向先行断言，匹配字段边界
     * </p>
     */
    private static final Pattern REACT_PATTERN = Pattern.compile(
            "(?i)" + // 忽略大小写
                    "(?:" +
                    "(?<thought>Thought\\s*:[\\s\\S]*?)" +
                    "(?=\\n\\s*(?:Thought|Action|Action Input|Observation|Final Answer)\\s*:|$)" +
                    "|" +
                    "(?<action>Action\\s*:[\\s\\S]*?)" +
                    "(?=\\n\\s*(?:Thought|Action|Action Input|Observation|Final Answer)\\s*:|$)" +
                    "|" +
                    "(?<actionInput>Action Input\\s*:[\\s\\S]*?)" +
                    "(?=\\n\\s*(?:Thought|Action|Action Input|Observation|Final Answer)\\s*:|$)" +
                    "|" +
                    "(?<observation>Observation\\s*:[\\s\\S]*?)" +
                    "(?=\\n\\s*(?:Thought|Action|Action Input|Observation|Final Answer)\\s*:|$)" +
                    "|" +
                    "(?<finalAnswer>Final Answer\\s*:[\\s\\S]*?)" +
                    "(?=\\n\\s*(?:Thought|Action|Action Input|Observation|Final Answer)\\s*:|$)" +
                    ")+"
    );

    /**
     * 从文本中提取 ReAct 响应
     * <p>
     * 通过一次正则匹配完成所有字段提取，避免对每个字段单独执行正则匹配，提升性能和一致性。
     * 当同一字段在输入中出现多次时，以最后一次出现的值为准，确保数据覆盖逻辑的一致性。
     * </p>
     *
     * @param text 包含 ReAct 格式文本
     * @return ReAct 实例，如果无法解析则返回 null
     */
    public static ReAct valueOf(String text) {

        if (text == null || text.isEmpty()) {
            return null;
        }

        // 使用 Map 存储字段，后值覆盖前值
        final var fields = new HashMap<String, String>();

        final var matcher = REACT_PATTERN.matcher(text);
        while (matcher.find()) {

            // 提取 thought 字段
            final var thought = matcher.group("thought");
            if (thought != null && !thought.trim().isEmpty()) {
                fields.put(THOUGHT, extractContent(thought));
            }

            // 提取 action 字段
            final var action = matcher.group("action");
            if (action != null && !action.trim().isEmpty()) {
                fields.put(ACTION, extractContent(action));
            }

            // 提取 actionInput 字段
            final var actionInput = matcher.group("actionInput");
            if (actionInput != null && !actionInput.trim().isEmpty()) {
                fields.put(ACTION_INPUT, extractContent(actionInput));
            }

            // 提取 observation 字段
            final var observation = matcher.group("observation");
            if (observation != null && !observation.trim().isEmpty()) {
                fields.put(OBSERVATION, extractContent(observation));
            }

            // 提取 finalAnswer 字段
            final var finalAnswer = matcher.group("finalAnswer");
            if (finalAnswer != null && !finalAnswer.trim().isEmpty()) {
                fields.put(FINAL_ANSWER, extractContent(finalAnswer));
            }
        }

        // 如果没有匹配到任何字段，返回 null
        if (fields.isEmpty()) {
            return null;
        }

        // 构建并返回唯一的 ReAct 实例
        return new ReAct(
                fields.get(THOUGHT),
                fields.get(ACTION),
                fields.get(ACTION_INPUT),
                fields.get(OBSERVATION),
                fields.get(FINAL_ANSWER)
        );
    }

    /**
     * 从匹配的字段文本中提取实际内容
     * <p>
     * 移除字段标识符和冒号，提取后续的实际内容
     * </p>
     *
     * @param matchedText 匹配到的字段文本（如 "Thought: xxx"）
     * @return 去除字段标识后的内容
     */
    private static String extractContent(String matchedText) {
        // 找到第一个冒号的位置
        final int colonIndex = matchedText.indexOf(':');
        if (colonIndex == -1) {
            return matchedText.trim();
        }

        // 提取冒号后的内容并去除首尾空白
        final String content = matchedText.substring(colonIndex + 1).trim();

        // 移除开头可能的换行符
        return content.replaceAll("^\\n+", "");
    }

}
