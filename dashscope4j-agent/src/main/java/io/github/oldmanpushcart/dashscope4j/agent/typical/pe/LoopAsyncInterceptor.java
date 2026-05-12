package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * 异步模式拦截器：Plan-Execute-Observation-Replan 循环
 */
class LoopAsyncInterceptor extends BaseLoopInterceptor {
    
    LoopAsyncInterceptor(Supplier<Agent> subAgentSupplier, int maxReplanCount, int maxSubTasks) {
        super(subAgentSupplier, maxReplanCount, maxSubTasks);
    }
    
    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        if(chain.type() != Type.ASYNC) {
            return chain.proceed(request);
        }
        return processAsync(chain, request);
    }
    
    @Override
    protected CompletionStage<?> proceedAndParse(Chain chain, AigcRequest<Input, Output> request) {
        return chain.proceed(request)
                .thenApply(r -> (AigcResponse<Output>) r);
    }
    
    /**
     * 处理异步请求
     */
    private CompletionStage<AigcResponse<Output>> processAsync(Chain chain, AigcRequest<Input, Output> request) {
        return generatePlan(chain, request)
                .thenCompose(plan -> {
                    if (plan == null || plan.getTasks().isEmpty()) {
                        return chain.proceed(request)
                                .thenApply(r -> (AigcResponse<Output>) r);
                    }
                    
                    final var sessionId = (String) request.context().get("SESSION-ID");
                    return executePlan(chain, request, sessionId, plan, 0);
                });
    }
    
    /**
     * 生成执行计划
     */
    private CompletionStage<ExecutionPlan> generatePlan(Chain chain, AigcRequest<Input, Output> request) {
        final var planningRequest = AigcRequest.newBuilder(request)
                .parameters(params -> {
                    params.put("response_format", Map.of("type", "json_object"));
                    return params;
                })
                .build();
        
        return chain.proceed(planningRequest)
                .thenApply(r -> (AigcResponse<Output>) r)
                .thenApply(response -> {
                    final var jsonContent = response.output().best().message().text();
                    final var lastMessage = request.input().messages().get(request.input().messages().size() - 1);
                    return parsePlanFromJson(jsonContent, (UserMessage) lastMessage);
                });
    }
    
    /**
     * 执行计划
     */
    private CompletionStage<AigcResponse<Output>> executePlan(
            Chain chain,
            AigcRequest<Input, Output> originalRequest,
            String sessionId,
            ExecutionPlan plan,
            int replanCount
    ) {
        if (plan.isAllTasksFinished()) {
            return synthesizeFinalAnswer(chain, originalRequest, sessionId, plan);
        }
        
        if (replanCount >= maxReplanCount) {
            return synthesizeFinalAnswer(chain, originalRequest, sessionId, plan);
        }
        
        final var currentTask = plan.getNextTask();
        if (currentTask == null) {
            return synthesizeFinalAnswer(chain, originalRequest, sessionId, plan);
        }
        
        return executeSubTask(sessionId, plan, currentTask)
                .thenCompose(result -> evaluateTaskResult(originalRequest, chain, currentTask.getDescription(), result)
                        .thenCompose(evaluation -> {
                            if (evaluation.isSuccess()) {
                                currentTask.complete(result);
                                plan.advanceToNextTask();
                                return executePlan(chain, originalRequest, sessionId, plan, replanCount);
                            } else {
                                final var failureReason = String.format(
                                        "任务失败: %s\n详情: %s",
                                        currentTask.getDescription(),
                                        evaluation.reason()
                                );
                                currentTask.fail(failureReason);
                                return replan(chain, originalRequest, sessionId, plan, replanCount);
                            }
                        }));
    }
    
    /**
     * 重规划
     */
    private CompletionStage<AigcResponse<Output>> replan(
            Chain chain,
            AigcRequest<Input, Output> originalRequest,
            String sessionId,
            ExecutionPlan oldPlan,
            int replanCount
    ) {
        final var replanRequest = buildReplanRequest(originalRequest, oldPlan);
        
        return chain.proceed(replanRequest)
                .thenApply(r -> (AigcResponse<Output>) r)
                .thenCompose(response -> {
                    final var newPlan = parseReplanFromJson(response.output().best().message().text(), oldPlan);
                    if (newPlan == null || newPlan.getTasks().isEmpty()) {
                        return executePlan(chain, originalRequest, sessionId, oldPlan, replanCount + 1);
                    }
                    return executePlan(chain, originalRequest, sessionId, newPlan, replanCount + 1);
                });
    }
    
    /**
     * 综合最终答案
     */
    private CompletionStage<AigcResponse<Output>> synthesizeFinalAnswer(
            Chain chain,
            AigcRequest<Input, Output> originalRequest,
            String sessionId,
            ExecutionPlan plan
    ) {
        final var synthesisRequest = buildSynthesisRequest(originalRequest, plan);
        
        return chain.proceed(synthesisRequest)
                .thenApply(r -> (AigcResponse<Output>) r);
    }
}
