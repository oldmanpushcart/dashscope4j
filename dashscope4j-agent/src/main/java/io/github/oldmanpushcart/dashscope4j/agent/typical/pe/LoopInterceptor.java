package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Plan-Execute 循环拦截器
 */
class LoopInterceptor implements ChatInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoopInterceptor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final Supplier<Agent> subAgentSupplier;
    private final int maxReplanCount;
    private final int maxSubTasks;

    LoopInterceptor(Supplier<Agent> subAgentSupplier, int maxReplanCount, int maxSubTasks) {
        this.subAgentSupplier = subAgentSupplier;
        this.maxReplanCount = maxReplanCount;
        this.maxSubTasks = maxSubTasks;
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
                    if (plan.getTasks().isEmpty()) {
                        return chatAsync(chain, request);
                    }

                    return executePlan(chain, request, sessionId, plan, 0);
                });
    }


    /*
     * 生成执行计划
     */
    private CompletionStage<ExecutionPlan> generatePlan(Chain chain, AigcRequest<Input, Output> request, String sessionId) {
        final var planningRequest = AigcRequest.newBuilder(request)
                .parameters(parameters -> {
                    parameters.put("response_format", Map.of(
                            "type", "json_object"
                    ));
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
                    final var plan = JacksonJsonUtils.toObject(resultJson, ExecutionPlan.class);
                    logger.debug("{}/{} generated plan. tasks={}", this, sessionId, plan.getTasks().size());
                    return plan;
                });
    }

    /*
     * 执行计划
     */
    private CompletionStage<AigcResponse<Output>> executePlan(Chain chain, AigcRequest<Input, Output> originalRequest, String sessionId, ExecutionPlan plan, int replanCount) {

        // 所有任务已经完成，则进行结果合成
        if (plan.isAllTasksFinished()) {
            logger.debug("{}/{}/plan all tasks finished, synthesizing final answer.", this, sessionId);
            return synthesizeFinalAnswer(chain, originalRequest, plan);
        }

        // 最大重规划次数已 reached，则进行结果合成
        if (replanCount >= maxReplanCount) {
            logger.debug("{}/{}/plan max replan count {} reached, synthesizing final answer.", this, sessionId, maxReplanCount);
            return synthesizeFinalAnswer(chain, originalRequest, plan);
        }

        final var task = plan.getNextTask();
        if (task == null) {
            return synthesizeFinalAnswer(chain, originalRequest, plan);
        }

        final var progress = "%s/%s".formatted(plan.getCurrentTaskIndex(), plan.getTasks().size());
        logger.debug("{}/{}/plan progress [{}]; task={};begin!", this, sessionId, progress, task.getTaskId());

        task.start();
        return executeSubTask(sessionId, plan, task)
                .thenCompose(result ->
                        evaluateTaskResult(originalRequest, chain, task.getDescription(), result)
                                .thenCompose(evaluation -> {
                                    if (evaluation.isSuccess()) {
                                        logger.debug("{}/{}/plan progress [{}]; task={};result={};", this, sessionId, progress, task.getTaskId(), "success");
                                        task.complete(result);
                                        plan.advanceToNextTask();
                                        return executePlan(chain, originalRequest, sessionId, plan, replanCount);
                                    } else {
                                        logger.debug("{}/{}/plan progress [{}]; task={};result={};reason={};", this, sessionId, progress, task.getTaskId(), "failure", evaluation.reason());
                                        final var failureReason = "任务失败: %s\n详情: %s".formatted(task.getDescription(), evaluation.reason());
                                        task.fail(failureReason);
                                        return replan(chain, originalRequest, sessionId, plan, replanCount);
                                    }
                                }));
    }

    /*
     * 执行子任务
     */
    private CompletionStage<String> executeSubTask(String mainSessionId, ExecutionPlan plan, SubTask task) {
        final var taskIndex = plan.getCurrentTaskIndex();
        final var subSessionId = String.format("%s-%d", mainSessionId, taskIndex);

        final var subAgent = subAgentSupplier.get();
        final var planSnapshot = plan.createSnapshot();
        final var enhancedTaskDesc = """
                **你的角色**: 你是一个专门的子智能体，只负责执行当前任务。
                
                **重要边界**:
                - 你必须专注于下方标记为"当前任务"的任务
                - 不要尝试执行计划中的其他任务
                - 其他任务将由不同的智能体处理
                - 你的工作仅完成当前任务并返回结果
                
                %s
                
                === 你的当前任务 ===
                
                %s
                """.formatted(
                planSnapshot,
                task.getDescription()
        );

        final var taskMessage = Message.user(enhancedTaskDesc);
        return subAgent.async(subSessionId, taskMessage)
                .thenApply(AssistantMessage::text);
    }

    private CompletionStage<TaskEvaluationResponse> evaluateTaskResult(AigcRequest<Input, Output> originalRequest, Chain chain, String taskDescription, String taskResult) {
        if (taskResult == null || taskResult.trim().isEmpty()) {
            return CompletableFuture.completedStage(new TaskEvaluationResponse(false, "Empty result"));
        }

        final var evaluationRequest = AigcRequest.newBuilder(originalRequest)
                .input(input -> Input.newBuilder(input)
                        .addMessage(Message.system(PromptTemplate.newBuilder()
                                .resource("prompt/TASK_EVALUATION.md")
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

    private CompletionStage<AigcResponse<Output>> replan(
            Chain chain,
            AigcRequest<Input, Output> originalRequest,
            String sessionId,
            ExecutionPlan oldPlan,
            int replanCount
    ) {
        logger.info("Replanning (attempt {}/{})", replanCount + 1, maxReplanCount);

        final var replanRequest = buildReplanRequest(originalRequest, oldPlan);

        return chatAsync(chain, replanRequest)
                .thenCompose(response -> {
                    final var newPlan = parseReplanFromJson(response.output().best().message().text(), oldPlan);
                    if (newPlan.getTasks().isEmpty()) {
                        logger.warn("Replan returned empty plan, continuing with old plan");
                        return executePlan(chain, originalRequest, sessionId, oldPlan, replanCount + 1);
                    }
                    logger.info("New plan generated with {} tasks", newPlan.getTasks().size());
                    return executePlan(chain, originalRequest, sessionId, newPlan, replanCount + 1);
                });
    }

    private CompletionStage<AigcResponse<Output>> synthesizeFinalAnswer(Chain chain, AigcRequest<Input, Output> originalRequest, ExecutionPlan plan) {
        final var planSnapshot = plan.createSnapshot();
        final var userMessage = Message.user(
                "基于已完成的任务，请提供对原始问题的全面最终答案:\n\n" + planSnapshot
        );

        final var synthesisRequest = AigcRequest.newBuilder(originalRequest)
                .input(input -> Input.newBuilder(input)
                        .addMessage(Message.system(PromptTemplate.newBuilder()
                                .resource("prompt/ANSWER_SYNTHESIS.md")
                                .build()
                                .render()))
                        .addMessage(userMessage)
                        .failOnToolError(false)
                        .build())
                .build();
        return chatAsync(chain, synthesisRequest);
    }

    private ExecutionPlan parseReplanFromJson(String jsonContent, ExecutionPlan oldPlan) {
        final var replanResponse = JacksonJsonUtils.toObject(jsonContent, ReplanResponse.class);
        final var newTasks = new ArrayList<SubTask>();
        if (replanResponse.newTasks() != null) {
            var taskIndex = 0;
            for (final var item : replanResponse.newTasks()) {
                final var description = item.description();
                if (description != null && !description.trim().isEmpty()) {
                    final var taskId = String.format("task-replan-%03d", ++taskIndex);
                    newTasks.add(new SubTask(taskId, description.trim()));
                }
            }
        }

        if (newTasks.isEmpty()) {
            for (final var task : oldPlan.getTasks()) {
                if (!task.isFinished()) {
                    newTasks.add(new SubTask(task.getTaskId() + "-retry", task.getDescription()));
                }
            }
        }

        return new ExecutionPlan(
                replanResponse.thought() != null ? replanResponse.thought() : oldPlan.getThought() + " [Revised]",
                newTasks
        );
    }

    private AigcRequest<Input, Output> buildReplanRequest(
            AigcRequest<Input, Output> originalRequest,
            ExecutionPlan oldPlan
    ) {
        final var planSnapshot = oldPlan.createSnapshot();
        final var userMessage = Message.user(
                String.format("当前计划需要修订。\n\n原因: 执行过程中某些任务失败。\n\n当前进度:\n%s\n\n请为剩余工作生成新计划。", planSnapshot)
        );

        return AigcRequest.newBuilder(originalRequest)
                .input(input -> Input.newBuilder(input)
                        .addMessage(Message.system(PromptTemplate.newBuilder()
                                .resource("prompt/PLAN_REPLANNING.md")
                                .build()
                                .render()))
                        .addMessage(userMessage)
                        .failOnToolError(false)
                        .build())
                .parameters(params -> {
                    params.put("response_format", Map.of("type", "json_object"));
                    return params;
                })
                .build();
    }

}
