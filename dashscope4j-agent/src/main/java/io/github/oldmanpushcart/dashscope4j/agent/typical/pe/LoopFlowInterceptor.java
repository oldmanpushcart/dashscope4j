package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * 流式模式拦截器：Plan-Execute-Observation-Replan 循环
 */
class LoopFlowInterceptor extends BaseLoopInterceptor {

    LoopFlowInterceptor(Supplier<Agent> subAgentSupplier, int maxReplanCount, int maxSubTasks) {
        super(subAgentSupplier, maxReplanCount, maxSubTasks);
    }

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {
        if (chain.type() != Type.FLOW) {
            return chain.proceed(request);
        }
        return processFlow(chain, request);
    }

    @Override
    protected CompletionStage<?> proceedAndParse(Chain chain, AigcRequest<Input, Output> request) {
        // For flow mode, collect the publisher into a single response
        return chain.proceed(request)
                .thenCompose(r -> {
                    @SuppressWarnings("unchecked") final var publisher = (Publisher<AigcResponse<Output>>) r;
                    // Collect flux into mono and convert to completion stage
                    return Mono.from(publisher).toFuture();
                })
                .exceptionally(ex -> {
                    // Handle cancellation gracefully
                    if (ex instanceof java.util.concurrent.CancellationException) {
                        return null;
                    }
                    throw new RuntimeException(ex);
                });
    }

    /**
     * 处理流式请求
     */
    private CompletionStage<Publisher<AigcResponse<Output>>> processFlow(Chain chain, AigcRequest<Input, Output> request) {
        final var sessionId = (String) request.context().get("SESSION-ID");

        return generatePlanForFlow(chain, request)
                .thenCompose(plan -> {
                    if (plan == null || plan.getTasks().isEmpty()) {
                        return chain.proceed(request)
                                .thenApply(r -> (Publisher<AigcResponse<Output>>) r);
                    }

                    return executePlanForFlow(chain, request, sessionId, plan, 0);
                });
    }

    /**
     * 为 flow 模式生成计划
     */
    private CompletionStage<ExecutionPlan> generatePlanForFlow(Chain chain, AigcRequest<Input, Output> request) {
        final var planningRequest = AigcRequest.newBuilder(request)
                .parameters(params -> {
                    params.put("response_format", Map.of("type", "json_object"));
                    return params;
                })
                .build();

        return proceedAndParse(chain, planningRequest)
                .thenApply(response -> {
                    final var aigcResponse = (AigcResponse<Output>) response;
                    final var jsonContent = aigcResponse.output().best().message().text();
                    final var lastMessage = request.input().messages().get(request.input().messages().size() - 1);
                    return parsePlanFromJson(jsonContent, (UserMessage) lastMessage);
                });
    }

    /**
     * 为 flow 模式执行计划
     */
    private CompletionStage<Publisher<AigcResponse<Output>>> executePlanForFlow(
            Chain chain,
            AigcRequest<Input, Output> originalRequest,
            String sessionId,
            ExecutionPlan plan,
            int replanCount
    ) {
        if (plan.isAllTasksFinished()) {
            return synthesizeFinalAnswerForFlow(chain, originalRequest, sessionId, plan);
        }

        if (replanCount >= maxReplanCount) {
            return synthesizeFinalAnswerForFlow(chain, originalRequest, sessionId, plan);
        }

        final var currentTask = plan.getNextTask();
        if (currentTask == null) {
            return synthesizeFinalAnswerForFlow(chain, originalRequest, sessionId, plan);
        }

        return executeSubTask(sessionId, plan, currentTask)
                .thenCompose(result -> evaluateTaskResult(originalRequest, chain, currentTask.getDescription(), result)
                        .thenCompose(evaluation -> {
                            if (evaluation.isSuccess()) {
                                currentTask.complete(result);
                                plan.advanceToNextTask();
                                return executePlanForFlow(chain, originalRequest, sessionId, plan, replanCount);
                            } else {
                                final var failureReason = String.format(
                                        "任务失败: %s\n详情: %s",
                                        currentTask.getDescription(),
                                        evaluation.reason()
                                );
                                currentTask.fail(failureReason);
                                return replanForFlow(chain, originalRequest, sessionId, plan, replanCount);
                            }
                        }));
    }

    /**
     * 为 flow 模式重规划
     */
    private CompletionStage<Publisher<AigcResponse<Output>>> replanForFlow(
            Chain chain,
            AigcRequest<Input, Output> originalRequest,
            String sessionId,
            ExecutionPlan oldPlan,
            int replanCount
    ) {
        final var replanRequest = buildReplanRequest(originalRequest, oldPlan);

        return proceedAndParse(chain, replanRequest)
                .thenCompose(response -> {
                    final var aigcResponse = (AigcResponse<Output>) response;
                    final var newPlan = parseReplanFromJson(aigcResponse.output().best().message().text(), oldPlan);
                    if (newPlan == null || newPlan.getTasks().isEmpty()) {
                        return executePlanForFlow(chain, originalRequest, sessionId, oldPlan, replanCount + 1);
                    }
                    return executePlanForFlow(chain, originalRequest, sessionId, newPlan, replanCount + 1);
                });
    }

    /**
     * 为 flow 模式综合最终答案
     */
    private CompletionStage<Publisher<AigcResponse<Output>>> synthesizeFinalAnswerForFlow(
            Chain chain,
            AigcRequest<Input, Output> originalRequest,
            String sessionId,
            ExecutionPlan plan
    ) {
        final var synthesisRequest = buildSynthesisRequest(originalRequest, plan);

        return chain.proceed(synthesisRequest)
                .thenApply(r -> {
                    @SuppressWarnings("unchecked") final var publisher = (Publisher<AigcResponse<Output>>) r;
                    return Mono.from(publisher)
                            .flatMapMany(response -> Flux.just(response));
                });
    }
}
