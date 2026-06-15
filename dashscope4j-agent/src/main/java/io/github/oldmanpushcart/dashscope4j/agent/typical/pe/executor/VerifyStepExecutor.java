package io.github.oldmanpushcart.dashscope4j.agent.typical.pe.executor;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.setting.SettingPlugin;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeAgent;
import io.github.oldmanpushcart.dashscope4j.agent.typical.pe.Plan;
import io.github.oldmanpushcart.dashscope4j.agent.typical.pe.PlanExecutor;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.util.IOUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class VerifyStepExecutor implements PeExecutor<VerifyStepExecutor.Context, VerifyStepExecutor.Result> {

    private final Agent agent;

    public VerifyStepExecutor(DashscopeClient client, ChatModel model, List<Plugin> plugins) {
        this.agent = makeVerifyStepAgent(client, model, plugins);
    }

    @Override
    public CompletionStage<Result> async(String sessionId, Context inbound) {
        final var step = inbound.step();
        final var stepOutbound = inbound.outbound();
        final var stepVerifyUserMessage = Message.user("""
                ## 任务信息
                %s
                """.formatted(
                JacksonJsonUtils.toJson(Map.of(
                        "description", step.getDescription(),
                        "outbound", stepOutbound.text()
                ))
        ));

        return agent.async(sessionId, stepVerifyUserMessage)
                .thenApply(stepVerifyOutbound -> {
                    final var stepVerifyJson = stepVerifyOutbound.text();
                    return JacksonJsonUtils.toObject(stepVerifyJson, Result.class);
                });
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(agent);
    }

    public record Context(Plan.Step step, AssistantMessage outbound) {

    }

    public record Result(Plan.Step.Status status, Map<String, Object> result, String message) {

    }

    private static Agent makeVerifyStepAgent(DashscopeClient client, ChatModel model, List<Plugin> plugins) {

        final var stepVerifySystemMessage = Message
                .system("""
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
                        """)
                .withCache();

        final var settingPlugin = SettingPlugin.newBuilder()
                .operator(request -> AigcRequest.newBuilder(request)
                        .input(input -> ChatModel.Input.newBuilder(input)
                                .messages(messages -> {
                                    messages.add(0, stepVerifySystemMessage);
                                    return messages;
                                })
                                .build())
                        .parameters(parameters -> {
                            parameters.put("response_format", Map.of("type", "json_object"));
                            return parameters;
                        })
                        .build())
                .build();

        return DashscopeAgent.newBuilder()
                .client(client)
                .model(model)
                .name("Verify Step Agent")
                .description("A verify step agent")
                .build();
    }

}
