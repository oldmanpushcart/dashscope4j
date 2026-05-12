package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * Plan-Execute 循环拦截器基类
 */
abstract class BaseLoopInterceptor implements ChatInterceptor {
    
    protected static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    protected final Supplier<Agent> subAgentSupplier;
    protected final int maxReplanCount;
    protected final int maxSubTasks;
    
    BaseLoopInterceptor(Supplier<Agent> subAgentSupplier, int maxReplanCount, int maxSubTasks) {
        this.subAgentSupplier = subAgentSupplier;
        this.maxReplanCount = maxReplanCount;
        this.maxSubTasks = maxSubTasks;
    }
    
    @Override
    public String toString() {
        return "dashscope4j-agent:/plan-execute";
    }
    
    /**
     * 从 JSON 解析计划
     */
    protected ExecutionPlan parsePlanFromJson(String jsonContent, UserMessage originalMessage) {
        try {
            final var planResponse = objectMapper.readValue(jsonContent, PlanResponse.class);
            
            final var tasks = new ArrayList<SubTask>();
            if (planResponse.tasks() != null) {
                var taskIndex = 0;
                for (final var item : planResponse.tasks()) {
                    final var taskId = item.taskId() != null ? item.taskId() : String.format("task-%03d", ++taskIndex);
                    final var description = item.description();
                    if (description != null && !description.trim().isEmpty()) {
                        tasks.add(new SubTask(taskId, description.trim()));
                    }
                }
            }
            
            if (tasks.isEmpty()) {
                tasks.add(new SubTask("task-001", originalMessage.text()));
            }
            
            if (tasks.size() > maxSubTasks) {
                return new ExecutionPlan(
                        planResponse.thought() != null ? planResponse.thought() : "Executing plan-based task decomposition",
                        tasks.subList(0, maxSubTasks)
                );
            }
            
            return new ExecutionPlan(
                    planResponse.thought() != null ? planResponse.thought() : "Executing plan-based task decomposition",
                    tasks
            );
            
        } catch (JsonProcessingException e) {
            return new ExecutionPlan(
                    "JSON parsing failed, executing as single task",
                    List.of(new SubTask("task-001", originalMessage.text()))
            );
        }
    }
    
    /**
     * 执行子任务
     */
    protected CompletionStage<String> executeSubTask(
            String mainSessionId,
            ExecutionPlan plan,
            SubTask task
    ) {
        final var taskIndex = plan.getCurrentTaskIndex();
        final var subSessionId = String.format("%s-%d", mainSessionId, taskIndex);
        
        task.start();
        
        try {
            final var subAgent = subAgentSupplier.get();
            final var planSnapshot = plan.createSnapshot();
            final var enhancedTaskDesc = String.format(
                    """
                    **你的角色**: 你是一个专门的子智能体，只负责执行当前任务。
                    
                    **重要边界**:
                    - 你必须专注于下方标记为"当前任务"的任务
                    - 不要尝试执行计划中的其他任务
                    - 其他任务将由不同的智能体处理
                    - 你的工作仅完成当前任务并返回结果
                    
                    %s
                    
                    === 你的当前任务 ===
                    
                    %s
                    """,
                    planSnapshot,
                    task.getDescription()
            );
            
            final var taskMessage = Message.user(enhancedTaskDesc);
            final var result = subAgent.async(subSessionId, taskMessage)
                    .toCompletableFuture()
                    .join();
            
            return CompletableFuture.completedStage(result.text());
            
        } catch (Exception e) {
            return CompletableFuture.completedStage(String.format("任务执行失败: %s", e.getMessage()));
        }
    }
    
    /**
     * 评估任务结果
     */
    protected CompletionStage<TaskEvaluationResponse> evaluateTaskResult(
            AigcRequest<Input, Output> originalRequest,
            ChatInterceptor.Chain chain,
            String taskDescription,
            String taskResult
    ) {
        if (taskResult == null || taskResult.trim().isEmpty()) {
            return CompletableFuture.completedStage(new TaskEvaluationResponse(false, "Empty result"));
        }
        
        final var userMessage = Message.user(
                String.format("任务: %s\n\n结果:\n%s\n\n请评估这个任务是否真正成功。", taskDescription, taskResult)
        );
        
        final var evaluationRequest = AigcRequest.newBuilder(originalRequest)
                .input(input -> Input.newBuilder(input)
                        .addMessage(Message.system(loadPromptFromResource("prompt/TASK_EVALUATION.md")))
                        .addMessage(userMessage)
                        .failOnToolError(false)
                        .build())
                .parameters(params -> {
                    params.put("response_format", Map.of("type", "json_object"));
                    return params;
                })
                .build();
        
        return proceedAndParse(chain, evaluationRequest)
                .thenApply(response -> parseTaskEvaluationFromJson(((AigcResponse<Output>) response).output().best().message().text()));
    }
    
    /**
     * 从 JSON 解析任务评估结果
     */
    protected TaskEvaluationResponse parseTaskEvaluationFromJson(String jsonContent) {
        try {
            return objectMapper.readValue(jsonContent, TaskEvaluationResponse.class);
        } catch (JsonProcessingException e) {
            return new TaskEvaluationResponse(false, "JSON parsing failed");
        }
    }
    
    /**
     * 从 JSON 解析重规划结果
     */
    protected ExecutionPlan parseReplanFromJson(String jsonContent, ExecutionPlan oldPlan) {
        try {
            final var replanResponse = objectMapper.readValue(jsonContent, ReplanResponse.class);
            
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
            
            if (newTasks.size() > maxSubTasks) {
                return new ExecutionPlan(
                        replanResponse.thought() != null ? replanResponse.thought() : oldPlan.getThought() + " [Revised]",
                        newTasks.subList(0, maxSubTasks)
                );
            }
            
            return new ExecutionPlan(
                    replanResponse.thought() != null ? replanResponse.thought() : oldPlan.getThought() + " [Revised]",
                    newTasks
            );
            
        } catch (JsonProcessingException e) {
            final var fallbackTasks = new ArrayList<SubTask>();
            for (final var task : oldPlan.getTasks()) {
                if (!task.isFinished()) {
                    fallbackTasks.add(new SubTask(task.getTaskId() + "-retry", task.getDescription()));
                }
            }
            return fallbackTasks.isEmpty() ? null : new ExecutionPlan(oldPlan.getThought() + " [Revised]", fallbackTasks);
        }
    }
    
    /**
     * 构建重规划请求
     */
    protected AigcRequest<Input, Output> buildReplanRequest(
            AigcRequest<Input, Output> originalRequest,
            ExecutionPlan oldPlan
    ) {
        final var planSnapshot = oldPlan.createSnapshot();
        final var userMessage = Message.user(
                String.format("当前计划需要修订。\n\n原因: 执行过程中某些任务失败。\n\n当前进度:\n%s\n\n请为剩余工作生成新计划。", planSnapshot)
        );
        
        return AigcRequest.newBuilder(originalRequest)
                .input(input -> Input.newBuilder(input)
                        .addMessage(Message.system(loadPromptFromResource("prompt/PLAN_REPLANNING.md")))
                        .addMessage(userMessage)
                        .failOnToolError(false)
                        .build())
                .parameters(params -> {
                    params.put("response_format", Map.of("type", "json_object"));
                    return params;
                })
                .build();
    }
    
    /**
     * 构建最终答案综合请求
     */
    protected AigcRequest<Input, Output> buildSynthesisRequest(
            AigcRequest<Input, Output> originalRequest,
            ExecutionPlan plan
    ) {
        final var planSnapshot = plan.createSnapshot();
        final var userMessage = Message.user(
                "基于已完成的任务，请提供对原始问题的全面最终答案:\n\n" + planSnapshot
        );
        
        return AigcRequest.newBuilder(originalRequest)
                .input(input -> Input.newBuilder(input)
                        .addMessage(Message.system(loadPromptFromResource("prompt/ANSWER_SYNTHESIS.md")))
                        .addMessage(userMessage)
                        .failOnToolError(false)
                        .build())
                .build();
    }
    
    /**
     * 从资源文件加载提示词
     */
    protected String loadPromptFromResource(String resourcePath) {
        try (var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalStateException("Prompt resource not found: " + resourcePath);
            }
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load prompt: " + resourcePath, e);
        }
    }
    
    /**
     * 抽象方法：由子类实现具体的 proceed 和解析逻辑
     */
    protected abstract CompletionStage<?> proceedAndParse(ChatInterceptor.Chain chain, AigcRequest<Input, Output> request);
}
