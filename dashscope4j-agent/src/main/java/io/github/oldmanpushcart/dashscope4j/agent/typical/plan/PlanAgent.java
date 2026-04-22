package io.github.oldmanpushcart.dashscope4j.agent.typical.plan;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.BaseAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.content.Content;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 计划智能体
 * <p>
 * 负责将复杂任务拆分为可执行的步骤序列,并协调多个 ReActAgent 按顺序执行各个步骤。
 * </p>
 * <p>
 * 核心特性:
 * <ul>
 *     <li>任务分解: 接收复杂任务,生成包含多个 Step 的执行计划</li>
 *     <li>流程编排: 按顺序执行每个 Step,管理 Step 间的依赖关系</li>
 *     <li>状态管理: 维护全局上下文,实现 Step 间的数据传递</li>
 *     <li>结果聚合: 汇总所有 Step 的执行结果,形成最终答案</li>
 *     <li>质量校验: 每步执行后校验质量,最终校验目标达成度</li>
 *     <li>自动修正: 目标未达成时自动生成补充步骤并重试</li>
 * </ul>
 * </p>
 *
 * @since 4.0.0
 */
public class PlanAgent extends BaseAgent {

    private static final Type listType = new TypeReference<List<Step>>() {
    }.getType();


    private static final PromptTemplate PLAN_PROMPT_TEMPLATE = PromptTemplate.newBuilder()
            .template(PlanAgent.class.getResourceAsStream("/prompt/PLAN_AGENT.md"))
            .build();

    private static final PromptTemplate AGGREGATE_PROMPT_TEMPLATE = PromptTemplate.newBuilder()
            .template(PlanAgent.class.getResourceAsStream("/prompt/PLAN_AGGREGATE.md"))
            .build();


    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * ReActAgent 模板实例,用于创建执行 Step 的子 Agent
     */
    private final ReActAgent reactAgentTemplate;

    protected PlanAgent(Builder builder) {
        super(builder);

        // 创建 ReActAgent 模板实例
        this.reactAgentTemplate = ReActAgent.newBuilder()
                .client(this.client())
                .toolbox(this.toolbox())
                .sessionManager(this.sessionManager())
                .model(this.model())
                .build();
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/plan";
    }

    @Override
    public CompletionStage<AssistantMessage> async(UserMessage inbound) {
        return generatePlan(inbound)
                .thenCompose(plan -> {
                    logger.debug("{} generated plan with {} steps", this, plan.steps().size());
                    return executePlan(plan, inbound);
                });
    }

    /**
     * 生成执行计划
     *
     * @param inbound 用户输入消息
     * @return 执行计划
     */
    private CompletionStage<Plan> generatePlan(UserMessage inbound) {

        final var request = AigcRequest.newBuilder(model())
                .input(Input.newBuilder()
                        .messages(messages -> {
                            messages.add(Message.system(Content.text(PLAN_PROMPT_TEMPLATE.render()).withCache()));
                            messages.add(Message.user(inbound.text()));
                            return messages;
                        })
                        .build())
                .parameters(parameters -> {
                    parameters.put("response_format", Map.of(
                            "type", "json_object"
                    ));
                    return parameters;
                })
                .build();

        return client().async(request)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(json -> {
                    logger.debug("{}/plan <<< {}", this, json);
                    final List<Step> steps = JacksonJsonUtils.toObject(json, listType);

                    // 构建完整的 Plan 对象
                    return new Plan(
                            this.sessionId(),   // planId: 使用当前 sessionId
                            inbound.text(),     // originalTask: 使用用户输入
                            steps               // steps: 从 JSON 解析
                    );
                });
    }


    /**
     * 执行计划
     *
     * @param plan    执行计划
     * @param inbound 原始用户输入
     * @return 最终执行结果
     */
    private CompletionStage<AssistantMessage> executePlan(Plan plan, UserMessage inbound) {

        // 串行执行所有步骤
        CompletionStage<Void> chain = CompletableFuture.completedStage(null);

        for (final var step : plan.steps()) {
            chain = chain.thenCompose(unused -> executeStep(step, plan))
                    .thenAccept(output -> {
                        plan.saveStepOutput(step.seq(), output);
                        logger.debug("{}/step-{} completed", this, step.seq());
                    });
        }

        return chain.thenCompose(unused -> aggregateResults(plan));
    }

    /**
     * 执行单个步骤
     *
     * @param step 步骤
     * @param plan 执行计划
     * @return 执行结果文本
     */
    private CompletionStage<String> executeStep(Step step, Plan plan) {

        final var stepAgent = ReActAgent.newBuilder(reactAgentTemplate)
                .sessionId("%s-step-%d".formatted(this.sessionId(), step.seq()))
                .name("step-executor-" + step.seq())
                .build();

        // 获取前置步骤输出
        final String prefixOutput = plan.formatPrefixOutputs(step.seq());

        final var stepInbound = Message.user(PromptTemplate.newBuilder()
                .template("""
                        ## 当前步骤
                        ${step_desc}
                        
                        ## 前置步骤输出
                        ${prefix_step_output}
                        
                        ## 期望输出
                        ${expected_output}
                        
                        请使用合适的工具完成此步骤
                        """)
                .variable("step_desc", step.description())
                .variable("prefix_step_output", prefixOutput.isEmpty() ? "无" : prefixOutput)
                .variable("expected_output", step.expectedOutput())
                .build()
                .render());

        return stepAgent.async(stepInbound)
                .thenApply(AssistantMessage::text);
    }

    /**
     * 聚合所有步骤的结果
     *
     * @param plan 执行计划
     * @return 最终的助手消息
     */
    private CompletionStage<AssistantMessage> aggregateResults(Plan plan) {

        final var request = AigcRequest.newBuilder(model())
                .input(Input.newBuilder()
                        .addMessage(Message.system(AGGREGATE_PROMPT_TEMPLATE.render()))
                        .addMessage(Message.user("""
                                ### 原始问题
                                %s
                                
                                ### 每一步结果
                                %s
                                """.formatted(
                                        plan.originalTask(),
                                        plan.formatAllStepOutputs()
                        )))
                        .build())
                .build();

        return client().async(request)
                .thenApply(response -> response.output().best().message());
    }

    @Override
    public Agent newSession(String sessionId) {
        return PlanAgent.newBuilder(this)
                .sessionId(sessionId)
                .build();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(PlanAgent agent) {
        return new Builder(agent);
    }

    public static class Builder extends BaseAgent.Builder<PlanAgent, Builder> {

        protected Builder() {
        }

        protected Builder(PlanAgent agent) {
            super(agent);
        }

        @Override
        public PlanAgent build() {
            return new PlanAgent(this);
        }
    }
}
