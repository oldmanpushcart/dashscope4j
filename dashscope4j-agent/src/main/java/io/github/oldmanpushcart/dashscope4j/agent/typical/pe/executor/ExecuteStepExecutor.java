package io.github.oldmanpushcart.dashscope4j.agent.typical.pe.executor;

import io.github.oldmanpushcart.dashscope4j.agent.Agent;
import io.github.oldmanpushcart.dashscope4j.agent.plugin.Plugin;
import io.github.oldmanpushcart.dashscope4j.agent.typical.pe.Plan;
import io.github.oldmanpushcart.dashscope4j.agent.typical.pe.PlanVariableResolver;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActAgent;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class ExecuteStepExecutor implements PeExecutor<ExecuteStepExecutor.Context, ExecuteStepExecutor.Result> {

    private final Agent agent;

    public ExecuteStepExecutor(DashscopeClient client, ChatModel model, List<Plugin> plugins) {
        this.agent = makeExecuteStepAgent(client, model, plugins);
    }

    @Override
    public CompletionStage<Result> async(String sessionId, Context inbound) {

        final var plan = inbound.plan();
        final var step = inbound.step();

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

        return agent.async(stepSessionId, stepInbound)
                .thenApply(Result::new);
    }

    @Override
    public void close() {

    }

    public record Context(Plan plan, Plan.Step step) {

    }

    public record Result(AssistantMessage outbound) {

    }

    private Agent makeExecuteStepAgent(DashscopeClient client, ChatModel model, List<Plugin> plugins) {
        return ReActAgent.newBuilder()
                .name("Execute Step Agent")
                .description("执行步骤")
                .client(client)
                .model(model)
                .plugins(plugins)
                .build();
    }

}
