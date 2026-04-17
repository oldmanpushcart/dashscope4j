package io.github.oldmanpushcart.dashscope4j.agent.typical;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.TextContent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.util.CheckUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.CommonUtils;

import java.util.ArrayList;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * Agent 作为函数的适配器
 * <p>
 * 将 Agent 包装为 Function，使得一个 Agent 可以作为工具被另一个 Agent 调用，
 * 实现 Agent 组合和层级调用。
 * </p>
 *
 * <h3>使用场景</h3>
 * <ul>
 *     <li>构建层次化的 Agent 系统</li>
 *     <li>不同 Agent 专注不同领域，通过工具调用协作</li>
 *     <li>复用专业 Agent 的能力</li>
 * </ul>
 *
 * <h3>示例</h3>
 * <pre>{@code
 * // 创建数据分析 Agent
 * var dataAgent = new ReActAgent.Builder()
 *         .name("data_analyst")
 *         .description("专业的数据分析师")
 *         .sessionId("data-session-001")
 *         .build();
 *
 * // 包装为函数并转换为工具
 * var dataTool = new AgentAsFunction(dataAgent).asTool();
 *
 * // 注册到工具箱
 * toolbox.register("data_analyst", dataTool);
 * }</pre>
 *
 * @since 4.0.0
 */
class AgentAsFunction implements Function<AgentAsFunction.AgentSpec, CompletionStage<String>> {

    /**
     * 被包装的 Agent
     */
    private final Agent agent;

    /**
     * 构造 Agent 函数适配器
     *
     * @param agent 被包装的 Agent
     */
    AgentAsFunction(Agent agent) {
        this.agent = agent;
    }

    /**
     * 执行 Agent 任务
     * <p>
     * 根据任务描述调用 Agent 进行处理，返回封装的结果。
     * </p>
     *
     * @param spec 任务参数，包含任务描述和可选的上下文信息
     * @return Agent 处理结果的 CompletionStage
     */
    @Override
    public CompletionStage<String> apply(AgentSpec spec) {
        return agent.async(spec.toInstant())
                .thenApply(AssistantMessage::text);
    }

    /**
     * 转换为 FunctionTool
     * <p>
     * 将当前函数包装为一个标准的 FunctionTool，
     * 使其可以被 LLM 发现和调用。
     * </p>
     *
     * @return 封装后的工具对象
     */
    public FunctionTool asTool() {
        final var name = "agent$" + agent.name();
        final var description = buildToolDescription(agent);
        return FunctionTool.newBuilder()
                .name(name)
                .description(description)
                .parameterType(AgentSpec.class)
                .function(this)
                .build();
    }

    /**
     * 构建工具描述
     *
     * @param agent Agent 实例
     * @return 工具描述
     */
    private static String buildToolDescription(Agent agent) {
        return """
                AI Agent 工具：%s
                
                %s
                
                【使用说明】
                - 此工具是一个智能助手，可以处理复杂的多步骤任务
                - 请提供清晰、详细的任务描述
                - 可以附加上下文信息帮助理解
                - 适合需要推理、规划或多轮对话的场景
                
                【适用场景】
                - 复杂问题分析
                - 多步骤任务执行
                - 需要专业知识的任务
                """.formatted(agent.name(), agent.description());
    }

    /**
     * Agent 作为工具时的任务参数
     *
     * @param task    要执行的任务描述
     * @param context 可选的上下文信息
     */
    public record AgentSpec(

            @JsonProperty("task")
            @JsonPropertyDescription("要执行的任务描述，应尽可能详细和清晰")
            String task,

            @JsonProperty("context")
            @JsonPropertyDescription("可选的上下文信息，帮助 Agent 更好地理解任务")
            String context

    ) {

        public UserMessage toInstant() {
            CheckUtils.requireNonBlankString(task(), "task must not be blank!");

            final var contents = new ArrayList<Content>();

            // 上下文（如有）
            if (CommonUtils.isNotBlankString(context())) {
                final var content = TextContent.newBuilder()
                        .text("""
                            ### 上下文
                            %s
                            
                            """.formatted(context()))
                        .build();
                contents.add(content);
            }

            // 任务
            contents.add(Content.text("""
                ### 任务
                %s
                """.formatted(task())));

            return Message.user(contents);
        }

    }

}
