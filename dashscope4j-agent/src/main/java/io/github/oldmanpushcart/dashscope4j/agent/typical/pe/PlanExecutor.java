package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class PlanExecutor {

    private final DashscopeClient client;
    private final ChatModel model;
    private final Agent stepAgent;

    public PlanExecutor(DashscopeClient client, ChatModel model, Agent stepAgent) {
        this.client = client;
        this.model = model;
        this.stepAgent = stepAgent;
    }

    public CompletionStage<Plan> execute(String sessionId, UserMessage inbound) {
        return generatePlan(sessionId, inbound)
                .thenCompose(plan -> executePlan(sessionId, plan));
    }

    private CompletionStage<Plan> generatePlan(String sessionId, UserMessage inbound) {

        final var generatePlanSystemMessage = Message.system("""
                # Role
                你是一个高级 AI 任务规划专家（Planner）。你的任务是将用户的复杂需求拆解为结构化、可执行的 `Plan` 对象。
                
                # Core Concept: Data Flow Architecture
                你必须将生成的 `Plan` 视为一个全局内存数据库。每个 `Step` 都是一个处理函数，它们通过 `arguments` 和 `result` 进行数据传递。
                - **写入数据**：Step 执行后，其产出会自动存入 `step_id.result`。
                - **读取数据**：后续 Step 必须在 `arguments` 中使用 `${step_id.result}` 语法来引用前置步骤的数据。
                
                # Rules
                1. **原子化**：每个 Step 必须只做一件事。
                2. **强依赖声明**：如果 Step B 需要 Step A 的数据，必须在 `dependency_ids` 中声明 `["step_A"]`，并在 `arguments` 中使用 `${step_A.result.xxx}`。
                3. **Plan 级变量**：所有步骤都可以随时回顾总目标，使用 `${plan.goal}`。
                4. **格式约束**：只输出合法的 JSON，不要包含任何 Markdown 标记或解释性文字。
                
                # Data Flow & Variable Safety Rules (极其重要)
                1. **禁止脑补字段**：工具（Tool）的输出结构是未知的（黑盒）。在 `arguments` 中引用前置步骤结果时，**绝对不要**自行编造深层嵌套字段（如 `${step_1.result.target_date}`）。
                2. **优先整体传递**：如果不确定工具返回的具体字段名，请直接传递整个结果对象，例如 `"date_obj": "${step_1.result}"`。让执行器（Executor）自行处理。
                3. **描述中声明预期**：在 `description` 中明确写出你期望该步骤返回什么数据，以便 Executor 能够按照预期格式化输出。
                
                # Output Format (Strict JSON)
                {
                  "id": "uuid-string",
                  "goal": "用户的原始目标",
                  "steps": [
                    {
                      "id": "step_1",
                      "name": "步骤名称",
                      "description": "详细的执行指令...",
                      "status": "PENDING",
                      "dependency_ids": [],
                      "arguments": {
                        "query": "静态参数"
                      },
                      "result": null,
                      "message": null
                    },
                    {
                      "id": "step_2",
                      "name": "依赖步骤",
                      "description": "使用前置步骤的结果进行处理...",
                      "status": "PENDING",
                      "dependency_ids": ["step_1"],
                      "arguments": {
                        "input_data": "${step_1.result}",
                        "goal_review": "${plan.goal}"
                      },
                      "result": null,
                      "message": null
                    }
                  ]
                }
                """);

        final var generatePlanRequest = AigcRequest.newBuilder(model)
                .input(ChatModel.Input.newBuilder()
                        .messages(List.of(
                                generatePlanSystemMessage,
                                inbound
                        ))
                        .build())
                .parameters(parameters -> {
                    parameters.put("response_format", Map.of("type", "json_object"));
                    return parameters;
                })
                .build();
        return client.async(generatePlanRequest)
                .thenApply(generatePlanResponse -> generatePlanResponse.output().best().message())
                .thenApply(outbound -> JacksonJsonUtils.toObject(outbound.text(), Plan.class));
    }

    private CompletionStage<Plan> executePlan(String sessionId, Plan plan) {
        if (plan.isFinished()) {
            return CompletableFuture.completedStage(plan);
        }

        CompletionStage<Void> completedF = CompletableFuture.completedStage(null);
        final var stepStages = plan.getNextExecutableSteps()
                .stream()
                .map(step -> executeStep(sessionId, plan, step))
                .toList();
        completedF = completedF.thenCompose(v -> CompletableFutureUtils.allOf(stepStages));
        return completedF.thenCompose(v -> executePlan(sessionId, plan));
    }

    private CompletionStage<Plan.Step> executeStep(String sessionId, Plan plan, Plan.Step step) {

        final var stepSessionId = "%s-step-%s".formatted(sessionId, step.getId());

        final var stepJson = JacksonJsonUtils.toJson(Map.of(
                "id", step.getId(),
                "name", step.getName(),
                "description", step.getDescription(),
                "arguments", PlanVariableResolver.resolve(step.getArguments(), plan)
        ));

        final var stepInbound = Message.user("""
                你是一个任务执行代理，你将收到一个具体的任务信息，其中包含步骤及其输入参数。
                
                ## 任务信息
                %s
                
                ## 规则
                1. 仔细分析 Description 和 Arguments，选择合适的工具执行任务。
                """.formatted(stepJson));

        return stepAgent.async(stepSessionId, stepInbound)
                .thenCompose(stepOutbound -> verifyStep(sessionId, plan, step, stepOutbound))
                .exceptionally(ex -> {
                    final var cause = CompletableFutureUtils.unwrapEx(ex);
                    step.setStatus(Plan.Step.Status.FAILURE);
                    step.setMessage("execute step: %s occur error: %s".formatted(step.getId(), cause));
                    return step;
                });

    }

    private CompletionStage<Plan.Step> verifyStep(String sessionId, Plan plan, Plan.Step step, AssistantMessage stepOutbound) {
        final var stepVerifySystemMessage = Message.system("""
                你是一个任务执行结果验证器，你将收到一个具体的任务描述和任务执行结果。你需要根据任务描述认真核对任务执行结果是否符合任务描述的要求。
                
                ## 校验规则
                1. 无论成功还是失败，最终输出都必须返回一个 JSON 对象。
                2. 如果执行成功，将关键产出物放入 `result` 字段，并在 `message` 中简述执行情况。
                3. 如果执行失败，将 `status` 设为 "FAILURE"，并在 `message` 中详细说明失败原因。
                5. 最终输出结果是否符合任务描述(Description)的要求，如果不满足则将`status`设为`FAILURE`
                
                ## 输出格式
                {
                  "status": "SUCCESS 或 FAILURE",
                  "result": {...},
                  "message": "执行过程中的简要说明或错误原因"
                }
                """);

        final var stepVerifyUserMessage = Message.user("""
                ## 任务信息
                %s
                """.formatted(
                JacksonJsonUtils.toJson(Map.of(
                        "description", step.getDescription(),
                        "outbound", stepOutbound.text()
                ))
        ));

        final var stepVerifyRequest = AigcRequest.newBuilder(model)
                .input(ChatModel.Input.newBuilder()
                        .messages(List.of(
                                stepVerifySystemMessage,
                                stepVerifyUserMessage
                        ))
                        .build())
                .parameters(parameters -> {
                    parameters.put("response_format", Map.of("type", "json_object"));
                    return parameters;
                })
                .build();
        return client.async(stepVerifyRequest)
                .thenApply(stepVerifyResponse -> {
                    final var stepVerifyJson = stepVerifyResponse.output().best().message().text();
                    final var stepVerifyResult = JacksonJsonUtils.toObject(stepVerifyJson, StepVerifyResult.class);
                    step.setResult(stepVerifyResult.result());
                    step.setStatus(stepVerifyResult.status());
                    step.setMessage(stepVerifyResult.message());
                    return step;
                });
    }

    private record StepVerifyResult(
            Plan.Step.Status status,
            Map<String, Object> result,
            String message
    ) {

    }

}
