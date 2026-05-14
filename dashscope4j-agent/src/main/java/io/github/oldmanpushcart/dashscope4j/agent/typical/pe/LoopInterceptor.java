package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Plan-Execute 循环拦截器
 */
class LoopInterceptor implements ChatInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoopInterceptor.class);

    private final Supplier<Agent> subAgentSupplier;
    private final int maxReplanCount;

    LoopInterceptor(Supplier<Agent> subAgentSupplier, int maxReplanCount) {
        this.subAgentSupplier = subAgentSupplier;
        this.maxReplanCount = maxReplanCount;
    }

    @Override
    public String toString() {
        return "dashscope4j-agent:/plan-execute";
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        if (chain.type() != Type.ASYNC) {
            return chain.proceed(request);
        }
        return processAsync(chain, request);
    }

    private CompletionStage<AigcResponse<Output>> chatAsync(Chain chain, AigcRequest<Input, Output> request) {
        return chain.proceed(request)
                .thenApply(r -> {
                    //noinspection unchecked
                    return (AigcResponse<Output>) r;
                });
    }

    private CompletionStage<AigcResponse<Output>> processAsync(Chain chain, AigcRequest<Input, Output> request) {
        final var sessionId = (String) request.context().get("SESSION-ID");
        return generatePlan(chain, request, sessionId)
                .thenCompose(plan -> {

                    // 如果执行计划中没有任何任务，则直接进行对话
                    if (plan.isEmpty()) {
                        return chatAsync(chain, request);
                    }

                    return executePlan(chain, request, sessionId, plan, 0);
                });
    }


    /*
     * 生成执行计划
     */
    private CompletionStage<Plan> generatePlan(Chain chain, AigcRequest<Input, Output> request, String sessionId) {
        final var planningRequest = AigcRequest.newBuilder(request)
                .parameters(parameters -> {
                    parameters.put("response_format", Map.of("type", "json_object"));
                    return parameters;
                })
                .build();

        return chain.proceed(planningRequest)
                .thenApply(r -> {
                    //noinspection unchecked
                    return (AigcResponse<Output>) r;
                })
                .thenApply(response -> {
                    final var resultJson = response.output().best().message().text();
                    final var plan = JacksonJsonUtils.toObject(resultJson, Plan.class);
                    logger.debug("{}/{} generated plan. tasks={}", this, sessionId, plan.tasks().size());
                    return plan;
                });
    }

    /*
     * 执行计划
     */
    private CompletionStage<AigcResponse<Output>> executePlan(Chain chain, AigcRequest<Input, Output> originalRequest, String sessionId, Plan plan, int replanCount) {

        // 所有任务已经完成，则进行结果合成
        if (plan.isFinished()) {
            logger.debug("{}/{} plan finished, synthesizing final answer.", this, sessionId);
            return synthesizeFinalAnswer(chain, originalRequest, plan);
        }

        // 最大重规划次数已 reached，则进行结果合成
        if (replanCount >= maxReplanCount) {
            logger.debug("{}/{} plan max replan count {} reached, synthesizing final answer.", this, sessionId, maxReplanCount);
            return synthesizeFinalAnswer(chain, originalRequest, plan);
        }

        final var task = plan.current();
        if (task == null) {
            return synthesizeFinalAnswer(chain, originalRequest, plan);
        }

        final var progress = "%s/%s".formatted(plan.index(), plan.tasks().size());
        logger.debug("{}/{}/plan progress [{}] begin! task={};{}", this, sessionId, progress, task.taskId(), task.description());

        task.start();
        return executeTask(sessionId, plan, task)
                .thenCompose(result ->
                        evaluateTaskResult(originalRequest, chain, task.description(), result)
                                .thenCompose(evaluation -> {
                                    if (evaluation.isSuccess()) {
                                        logger.debug("{}/{}/plan progress [{}]; task={};result={};", this, sessionId, progress, task.taskId(), "success");
                                        task.complete(result);
                                        plan.advance();
                                        return executePlan(chain, originalRequest, sessionId, plan, replanCount);
                                    } else {
                                        logger.debug("{}/{}/plan progress [{}]; task={};result={};reason={};", this, sessionId, progress, task.taskId(), "failure", evaluation.reason());
                                        final var failureReason = "任务失败: %s\n详情: %s".formatted(task.description(), evaluation.reason());
                                        task.fail(failureReason);
                                        return replan(chain, originalRequest, sessionId, plan, replanCount);
                                    }
                                }));
    }

    /*
     * 执行子任务
     */
    private CompletionStage<String> executeTask(String mainSessionId, Plan plan, Task task) {
        final var taskIndex = plan.index();
        final var subSessionId = String.format("%s-%d", mainSessionId, taskIndex);

        final var subAgent = subAgentSupplier.get();
        final var planJson = JacksonJsonUtils.toJson(plan);
        final var enhancedTaskDesc = """
                **你的角色**: 你是一个专门的子智能体，只负责执行当前任务。
                
                **重要边界**:
                - 你只需要关注 taskId=%s的任务
                - 不要尝试执行计划中的其他任务
                - 其他任务将由不同的智能体处理
                - 你的工作仅完成当前任务并返回结果
                
                当前计划 (JSON格式):
                %s
                
                === 你的当前任务 ===
                
                %s
                """.formatted(
                task.taskId(),
                planJson,
                task.description()
        );

        final var taskMessage = Message.user(enhancedTaskDesc);
        return subAgent.async(subSessionId, taskMessage)
                .thenApply(AssistantMessage::text);
    }

    /*
     * 验证任务执行结果
     */
    private CompletionStage<TaskEvaluationResponse> evaluateTaskResult(AigcRequest<Input, Output> originalRequest, Chain chain, String taskDescription, String taskResult) {
        final var evaluationRequest = AigcRequest.newBuilder(originalRequest)
                .input(input -> Input.newBuilder(input)
                        .addMessage(Message.system(PromptTemplate.newBuilder()
                                .resource("/prompt/TASK_EVALUATION.md")
                                .build()
                                .render()))
                        .addMessage(Message.user("""
                                任务: %s
                                
                                结果:
                                %s
                                
                                请评估这个任务是否真正成功。""".formatted(
                                taskDescription,
                                taskResult
                        )))
                        .build())
                .parameters(params -> {
                    params.put("response_format", Map.of("type", "json_object"));
                    return params;
                })
                .build();

        return chatAsync(chain, evaluationRequest)
                .thenApply(response -> {
                    final var resultJson = response.output().best().message().text();
                    return JacksonJsonUtils.toObject(resultJson, TaskEvaluationResponse.class);
                });
    }

    /*
     * 重新生成计划
     */
    private CompletionStage<AigcResponse<Output>> replan(Chain chain, AigcRequest<Input, Output> originalRequest, String sessionId, Plan plan, int replanCount) {
        logger.info("Replanning (attempt {}/{})", replanCount + 1, maxReplanCount);

        final var replanRequest = AigcRequest.newBuilder(originalRequest)
                .input(input -> Input.newBuilder(input)
                        .addMessage(Message.system(PromptTemplate.newBuilder()
                                .resource("/prompt/PLAN_REPLANNING.md")
                                .build()
                                .render()))
                        .addMessage(Message.user(
                                """
                                        当前计划需要修订。
                                        
                                        原因: 执行过程中某些任务失败。
                                        
                                        当前计划状态 (JSON格式):
                                        %s
                                        
                                        请为剩余工作生成新计划。返回格式必须与初始计划格式完全一致（包含 thought 和 tasks 字段）。""".formatted(JacksonJsonUtils.toJson(plan))
                        ))
                        .build())
                .parameters(params -> {
                    params.put("response_format", Map.of("type", "json_object"));
                    return params;
                })
                .build();

        return chatAsync(chain, replanRequest)
                .thenCompose(response -> {
                    final var jsonContent = response.output().best().message().text();
                    final var newPlan = JacksonJsonUtils.toObject(jsonContent, Plan.class);
                    logger.debug("{}/{}/replan generated new plan, thought={}, tasks={}", this, sessionId, newPlan.thought(), newPlan.tasks().size());
                    if (newPlan.tasks().isEmpty()) {
                        logger.warn("{}/{}/replan WARNING: new plan has no tasks! This will cause immediate termination.", this, sessionId);
                    }
                    return executePlan(chain, originalRequest, sessionId, newPlan, replanCount + 1);
                });
    }

    /*
     * 生成最终答案
     */
    private CompletionStage<AigcResponse<Output>> synthesizeFinalAnswer(Chain chain, AigcRequest<Input, Output> originalRequest, Plan plan) {
        final var planJson = JacksonJsonUtils.toJson(plan);
        final var userMessage = Message.user(
                "基于已完成的任务，请提供对原始问题的全面最终答案。\n\n" +
                        "当前计划状态 (JSON格式):\n" + planJson
        );

        final var synthesisRequest = AigcRequest.newBuilder(originalRequest)
                .input(input -> Input.newBuilder(input)
                        .addMessage(Message.system(PromptTemplate.newBuilder()
                                .resource("/prompt/ANSWER_SYNTHESIS.md")
                                .build()
                                .render()))
                        .addMessage(userMessage)
                        .failOnToolError(false)
                        .build())
                .build();
        return chatAsync(chain, synthesisRequest);
    }

}
