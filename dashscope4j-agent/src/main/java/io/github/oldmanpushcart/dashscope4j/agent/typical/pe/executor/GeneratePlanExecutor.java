package io.github.oldmanpushcart.dashscope4j.agent.typical.pe.executor;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.setting.SettingPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.pe.Plan;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.UserMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class GeneratePlanExecutor implements PeExecutor<UserMessage, Plan> {

    private final Agent agent;

    public GeneratePlanExecutor(DashscopeClient client, ChatModel model, List<Plugin> plugins) {
        this.agent = makeGeneratePlanAgent(client, model, plugins);
    }

    @Override
    public CompletionStage<Plan> async(String sessionId, UserMessage inbound) {
        return agent.async(sessionId, inbound)
                .thenApply(outbound -> JacksonJsonUtils.toObject(outbound.text(), Plan.class));
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(agent);
    }


    public static Agent makeGeneratePlanAgent(DashscopeClient client, ChatModel model, List<Plugin> plugins) {

        final var generatePlanSystemMessage = Message
                .system("""
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
                        """)
                .withCache();

        final var settingPlugin = SettingPlugin.newBuilder()
                .operator(request ->
                        AigcRequest.newBuilder(request)
                                .input(input -> ChatModel.Input.newBuilder(input)
                                        .messages(messages -> {
                                            messages.add(0, generatePlanSystemMessage);
                                            return messages;
                                        })
                                        .build())
                                .parameters(parameters -> {
                                    parameters.put("response_format", Map.of("type", "json_object"));
                                    return parameters;
                                })
                                .build())
                .build();

        final var newPlugins = new ArrayList<Plugin>();
        newPlugins.add(settingPlugin);
        newPlugins.addAll(plugins);

        return DashscopeAgent.newBuilder()
                .client(client)
                .model(model)
                .name("Generate Plan Agent")
                .description("A generate plan agent")
                .plugins(newPlugins)
                .build();
    }

}
