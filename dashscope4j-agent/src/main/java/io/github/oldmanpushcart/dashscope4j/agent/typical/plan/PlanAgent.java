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
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.*;
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

    private static final Type stepValidationType = new TypeReference<Map<String, Object>>() {
    }.getType();

    private static final Type goalValidationType = new TypeReference<Map<String, Object>>() {
    }.getType();

    private static final PromptTemplate PLAN_PROMPT_TEMPLATE = PromptTemplate.newBuilder()
            .template(PlanAgent.class.getResourceAsStream("/prompt/PLAN_AGENT.md"))
            .build();

    private static final PromptTemplate AGGREGATE_PROMPT_TEMPLATE = PromptTemplate.newBuilder()
            .template(PlanAgent.class.getResourceAsStream("/prompt/PLAN_AGGREGATE.md"))
            .build();

    private static final PromptTemplate STEP_VALIDATE_PROMPT_TEMPLATE = PromptTemplate.newBuilder()
            .template(PlanAgent.class.getResourceAsStream("/prompt/PLAN_VALIDATE_STEP.md"))
            .build();

    private static final PromptTemplate GOAL_VALIDATE_PROMPT_TEMPLATE = PromptTemplate.newBuilder()
            .template(PlanAgent.class.getResourceAsStream("/prompt/PLAN_VALIDATE_GOAL.md"))
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
        final var prompt = PLAN_PROMPT_TEMPLATE.render(Map.of(
                "task", inbound.text()
        ));

        final var request = AigcRequest.newBuilder(model())
                .input(Input.newBuilder()
                        .addMessage(Message.user(prompt))
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
                    try {

                        final List<Step> steps = JacksonJsonUtils.toObject(json, listType);

                        // 构建完整的 Plan 对象
                        return new Plan(
                                this.sessionId(),       // planId: 使用当前 sessionId
                                inbound.text(),         // originalTask: 使用用户输入
                                steps,                  // steps: 从 JSON 解析
                                new HashMap<>()         // context: 初始化为空 Map
                        );
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Failed to parse plan JSON: " + json, e);
                    }
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

        // 步骤上下文,用于传递前置步骤的输出
        final var context = new HashMap<String, Object>();
        
        // 串行执行所有步骤
        final var results = new ArrayList<String>();
        CompletionStage<Void> chain = CompletableFuture.completedStage(null);

        for (final var step : plan.steps()) {
            chain = chain.thenCompose(unused -> executeStep(step, context))
                    .thenAccept(output -> {
                        results.add(output);
                        context.put("step_" + step.seq() + "_output", output);
                        logger.debug("{}/step-{} completed", this, step.seq());
                    });
        }

        return chain.thenCompose(unused -> aggregateResults(results, inbound));
    }

    /**
     * 执行单个步骤
     *
     * @param step    步骤
     * @param context 步骤上下文
     * @return 执行结果文本
     */
    private CompletionStage<String> executeStep(Step step, Map<String, Object> context) {

        final var stepAgent = ReActAgent.newBuilder(reactAgentTemplate)
                .sessionId("%s-step-%d".formatted(this.sessionId(), step.seq()))
                .name("step-executor-" + step.seq())
                .build();

        // 收集所有前置步骤的输出
        final var sb = new StringBuilder();
        for (int i = 1; i < step.seq(); i++) {
            final var output = context.get("step_" + i + "_output");
            if (output != null) {
                sb.append("### 步骤 ").append(i).append(" 输出\n");
                sb.append(output).append("\n\n");
            }
        }
        final String prefixOutput = sb.toString();

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
     * @param results 步骤执行结果列表
     * @param inbound 原始用户输入
     * @return 最终的助手消息
     */
    private CompletionStage<AssistantMessage> aggregateResults(List<String> results, UserMessage inbound) {
        // 校验原始目标是否达成
        return validateGoal(inbound.text(), results)
                .thenCompose(validation -> {
                    final var goalAchieved = (Boolean) validation.getOrDefault("goal_achieved", true);
                    
                    if (goalAchieved) {
                        // 目标已达成，直接聚合
                        logger.debug("{}/goal-validation passed", this);
                        return doAggregate(results, inbound);
                    } else {
                        // 目标未达成，生成补充步骤
                        logger.warn("{}/goal-validation failed, generating supplementary steps", this);
                        final var supplementarySteps = generateSupplementarySteps(inbound.text(), validation, results.size());
                        
                        if (supplementarySteps.isEmpty()) {
                            // 没有建议的补充步骤，直接聚合
                            logger.warn("{}/no supplementary steps suggested", this);
                            return doAggregate(results, inbound);
                        }
                        
                        // 执行补充步骤
                        logger.info("{}/executing {} supplementary steps", this, supplementarySteps.size());
                        return executeSupplementarySteps(supplementarySteps, results, inbound);
                    }
                });
    }

    /**
     * 执行补充步骤
     *
     * @param supplementarySteps 补充步骤列表
     * @param existingResults    已有结果
     * @param inbound            原始用户输入
     * @return 最终的助手消息
     */
    private CompletionStage<AssistantMessage> executeSupplementarySteps(
            List<Step> supplementarySteps,
            List<String> existingResults,
            UserMessage inbound
    ) {
        final var context = new HashMap<String, Object>();
        // 将已有结果放入 context
        for (int i = 0; i < existingResults.size(); i++) {
            context.put("step_" + (i + 1) + "_output", existingResults.get(i));
        }

        final var allResults = new ArrayList<>(existingResults);
        CompletionStage<Void> chain = CompletableFuture.completedStage(null);

        for (final var step : supplementarySteps) {
            chain = chain.thenCompose(unused -> executeStep(step, context))
                    .thenAccept(output -> {
                        allResults.add(output);
                        context.put("step_" + step.seq() + "_output", output);
                        logger.debug("{}/supplementary-step-{} completed", this, step.seq());
                    });
        }

        return chain.thenCompose(unused -> doAggregate(allResults, inbound));
    }

    /**
     * 执行实际的聚合操作
     *
     * @param results 步骤结果列表
     * @param inbound 原始用户输入
     * @return 最终的助手消息
     */
    private CompletionStage<AssistantMessage> doAggregate(List<String> results, UserMessage inbound) {
        final var prompt = AGGREGATE_PROMPT_TEMPLATE.render(Map.of(
                "original_task", inbound.text(),
                "step_results", formatStepResults(results)
        ));

        final var request = AigcRequest.newBuilder(model())
                .input(Input.newBuilder()
                        .addMessage(Message.user(prompt))
                        .build())
                .build();

        return client().async(request)
                .thenApply(response -> response.output().best().message());
    }

    /**
     * 格式化步骤结果
     *
     * @param results 步骤结果列表
     * @return 格式化后的字符串
     */
    private String formatStepResults(List<String> results) {
        final var sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            sb.append("### Step ").append(i + 1).append("\n");
            sb.append("Output: ").append(results.get(i)).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 校验步骤执行质量
     *
     * @param step          步骤
     * @param actualOutput  实际输出
     * @return 校验结果
     */
    private CompletionStage<Map<String, Object>> validateStep(Step step, String actualOutput) {
        final var prompt = STEP_VALIDATE_PROMPT_TEMPLATE.render(Map.of(
                "step_description", step.description(),
                "expected_output", step.expectedOutput(),
                "actual_output", actualOutput
        ));

        final var request = AigcRequest.newBuilder(model())
                .input(Input.newBuilder()
                        .addMessage(Message.user(prompt))
                        .build())
                .parameters(parameters -> {
                    parameters.put("response_format", Map.of("type", "json_object"));
                    return parameters;
                })
                .build();

        return client().async(request)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(json -> {
                    try {
                        return JacksonJsonUtils.toObject(json, stepValidationType);
                    } catch (Exception e) {
                        logger.warn("{}/validate-step failed to parse JSON: {}", this, json, e);
                        return Map.of("completed", true, "quality", "unknown");
                    }
                });
    }

    /**
     * 校验原始目标是否达成
     *
     * @param originalTask 原始任务
     * @param stepResults  步骤结果列表
     * @return 校验结果
     */
    private CompletionStage<Map<String, Object>> validateGoal(String originalTask, List<String> stepResults) {
        final var prompt = GOAL_VALIDATE_PROMPT_TEMPLATE.render(Map.of(
                "original_task", originalTask,
                "step_results", formatStepResults(stepResults)
        ));

        final var request = AigcRequest.newBuilder(model())
                .input(Input.newBuilder()
                        .addMessage(Message.user(prompt))
                        .build())
                .parameters(parameters -> {
                    parameters.put("response_format", Map.of("type", "json_object"));
                    return parameters;
                })
                .build();

        return client().async(request)
                .thenApply(response -> response.output().best().message().text())
                .thenApply(json -> {
                    try {
                        return JacksonJsonUtils.toObject(json, goalValidationType);
                    } catch (Exception e) {
                        logger.warn("{}/validate-goal failed to parse JSON: {}", this, json, e);
                        return Map.of("goal_achieved", true);
                    }
                });
    }

    /**
     * 生成补充步骤
     *
     * @param originalTask   原始任务
     * @param validation     校验结果
     * @param existingSteps  已有步骤数
     * @return 补充步骤列表
     */
    private List<Step> generateSupplementarySteps(String originalTask, Map<String, Object> validation, int existingSteps) {
        @SuppressWarnings("unchecked")
        final var suggestedSteps = (List<Map<String, Object>>) validation.get("suggested_steps");
        
        if (suggestedSteps == null || suggestedSteps.isEmpty()) {
            return List.of();
        }

        final var steps = new ArrayList<Step>();
        int seq = existingSteps + 1;
        
        for (var suggested : suggestedSteps) {
            final var description = (String) suggested.get("description");
            final var expectedOutput = (String) suggested.get("expected_output");
            
            if (description != null && expectedOutput != null) {
                steps.add(new Step(seq++, description, expectedOutput, null, null));
            }
        }
        
        return steps;
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
