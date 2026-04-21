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
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
 * </ul>
 * </p>
 *
 * @since 4.0.0
 */
public class PlanAgent extends BaseAgent {

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

    /**
     * Step 执行超时时间
     */
    private final Duration stepTimeout;

    protected PlanAgent(Builder builder) {
        super(builder);

        // 创建 ReActAgent 模板实例
        this.reactAgentTemplate = ReActAgent.newBuilder()
                .client(this.client())
                .toolbox(this.toolbox())
                .sessionManager(this.sessionManager())
                .model(this.model())
                .build();

        this.stepTimeout = builder.stepTimeout;
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/plan";
    }

    @Override
    public CompletionStage<AssistantMessage> async(UserMessage inbound) {
        logger.info("{} start executing task: {}", this, inbound.text());

        return generatePlan(inbound)
                .thenCompose(plan -> {
                    logger.info("{} plan generated with {} steps", this, plan.steps().size());
                    return executePlanAsync(plan, inbound);
                })
                .thenApply(result -> {
                    logger.info("{} task completed", this);
                    return result;
                })
                .exceptionally(ex -> {
                    logger.error("{} task failed", this, ex);
                    return Message.assistant("task execution failed: " + ex.getMessage());
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
                .parameters(parameters-> {
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
                        // 直接解析为 List<Step>
                        final Type listType = new TypeReference<List<Step>>() {}.getType();
                        @SuppressWarnings("unchecked")
                        final var steps = (List<Step>) JacksonJsonUtils.toObject(json, listType);
                        
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
    private CompletionStage<AssistantMessage> executePlanAsync(Plan plan, UserMessage inbound) {
        final var context = new HashMap<String, Object>();
        context.put("original_task", inbound.text());

        // 串行执行所有步骤
        final var results = new ArrayList<StepResult>();
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);

        for (Step step : plan.steps()) {
            chain = chain.thenCompose(unused -> executeStep(step, context))
                    .thenAccept(result -> {
                        results.add(result);
                        if (result.isSuccess()) {
                            context.put("step_" + step.seq() + "_output", result.output());
                            logger.info("{}.step{} completed", this, step.seq());
                        } else {
                            logger.error("{}.step{} failed: {}",
                                    this, step.seq(),
                                    result.error() != null ? result.error().getMessage() : "unknown error");
                        }
                    });
        }

        return chain.thenCompose(unused -> aggregateResults(plan, results, inbound));
    }

    /**
     * 执行单个步骤
     *
     * @param step    步骤
     * @param context 全局上下文
     * @return 执行结果
     */
    private CompletionStage<StepResult> executeStep(Step step, Map<String, Object> context) {
        final var stepAgent = createStepAgent(step);
        final var userMessage = buildStepUserMessage(step, context);

        logger.debug("{}.step{} starting: {}", this, step.seq(), step.description());

        final var future = stepAgent.async(userMessage).toCompletableFuture();

        // 添加超时控制
        try {
            final var result = future.get(stepTimeout.toMillis(), TimeUnit.MILLISECONDS);
            return CompletableFuture.completedStage(StepResult.success(step.seq(), result.text()));
        } catch (TimeoutException e) {
            future.cancel(true);
            return CompletableFuture.completedStage(
                    StepResult.failed(step.seq(), new TimeoutException("Step execution timeout: " + stepTimeout)));
        } catch (Exception e) {
            return CompletableFuture.completedStage(StepResult.failed(step.seq(), e));
        }
    }

    /**
     * 创建步骤专用的 ReActAgent
     *
     * @param step 步骤
     * @return ReActAgent 实例
     */
    private ReActAgent createStepAgent(Step step) {
        final var stepSessionId = String.format("%s.step%d", this.sessionId(), step.seq());

        return ReActAgent.newBuilder(reactAgentTemplate)
                .sessionId(stepSessionId)
                .name("step-executor-" + step.seq())
                .build();
    }

    /**
     * 构建步骤的用户消息
     *
     * @param step    步骤
     * @param context 全局上下文
     * @return 用户消息
     */
    private UserMessage buildStepUserMessage(Step step, Map<String, Object> context) {
        final var sb = new StringBuilder();

        sb.append("## 当前任务\n\n");
        sb.append(step.description()).append("\n\n");

        // 添加入口来源信息
        if (step.inputFrom() != null) {
            final var inputKey = "step_" + step.inputFrom() + "_output";
            final var inputValue = context.get(inputKey);
            if (inputValue != null) {
                sb.append("## 前置步骤输出\n\n");
                sb.append("来自步骤 ").append(step.inputFrom()).append(" 的结果:\n");
                sb.append(inputValue).append("\n\n");
            }
        } else {
            // 使用原始任务作为输入
            sb.append("## 原始任务\n\n");
            sb.append(context.get("original_task")).append("\n\n");
        }

        sb.append("## 期望输出\n\n");
        sb.append(step.expectedOutput()).append("\n\n");

        sb.append("请使用合适的工具完成此步骤。");

        return Message.user(sb.toString());
    }

    /**
     * 聚合所有步骤的结果
     *
     * @param plan        执行计划
     * @param stepResults 步骤执行结果列表
     * @param inbound     原始用户输入
     * @return 最终的助手消息
     */
    private CompletionStage<AssistantMessage> aggregateResults(
            Plan plan,
            List<StepResult> stepResults,
            UserMessage inbound
    ) {
        // 检查是否有失败的步骤
        final var failedSteps = stepResults.stream()
                .filter(r -> !r.isSuccess())
                .toList();

        if (!failedSteps.isEmpty()) {
            final var errorMsg = failedSteps.stream()
                    .map(r -> String.format("Step %d failed: %s", r.seq(),
                            r.error() != null ? r.error().getMessage() : "unknown"))
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Unknown error");
            return CompletableFuture.completedStage(
                    Message.assistant("Task execution encountered errors: " + errorMsg));
        }

        // All steps succeeded, generate final summary
        final var prompt = AGGREGATE_PROMPT_TEMPLATE.render(Map.of(
                "original_task", inbound.text(),
                "step_results", formatStepResults(stepResults)
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
     * @param stepResults 步骤结果列表
     * @return 格式化后的字符串
     */
    private String formatStepResults(List<StepResult> stepResults) {
        final var sb = new StringBuilder();
        for (var result : stepResults) {
            sb.append("### Step ").append(result.seq()).append("\n");
            sb.append("Status: ").append(result.status()).append("\n");
            if (result.output() != null) {
                sb.append("Output: ").append(result.output()).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
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

        private Duration stepTimeout = Duration.ofMinutes(5);

        protected Builder() {
        }

        protected Builder(PlanAgent agent) {
            super(agent);
        }

        /**
         * 设置步骤执行超时时间
         *
         * @param timeout 超时时间
         * @return 构建器
         */
        public Builder stepTimeout(Duration timeout) {
            this.stepTimeout = timeout;
            return this;
        }

        @Override
        public PlanAgent build() {
            return new PlanAgent(this);
        }
    }
}
