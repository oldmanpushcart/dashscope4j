package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcModel;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class LoopInterceptor implements ChatInterceptor {

    private static final Message PLAN_GENERATOR_SYSTEM_MESSAGE = Message.system(PromptTemplate.newBuilder()
            .resource("/prompt/PLAN_GENERATOR.md")
            .build()
            .render());

    private static final Message PLAN_REPLANNER_SYSTEM_MESSAGE = Message.system(PromptTemplate.newBuilder()
            .resource("/prompt/PLAN_REPLANNER.md")
            .build()
            .render());

    @Override
    public CompletionStage<?> intercept(Chain chain, AigcRequest<Input, Output> request) {

        if (chain.type() != Type.ASYNC) {
            return chain.proceed(request);
        }

        return null;
    }


    /**
     * 根据对话消息列表与LLM进行交互，将返回的JSON结果解析并转化为指定的对象。
     *
     * @param client   客户端
     * @param model    对话模型
     * @param messages 对话消息列表
     * @param clazz    返回对象类型
     * @param <T>      返回对象类型
     * @return 返回对象
     */
    private <T> CompletionStage<T> completeChatAs(DashscopeClient client, AigcModel<Input, Output> model, List<Message> messages, Class<T> clazz) {
        final var request = AigcRequest.newBuilder(model)
                .input(Input.newBuilder()
                        .messages(messages)
                        .build())
                .parameters(parameters -> {
                    parameters.put("response_format", Map.of("type", "json_object"));
                    return parameters;
                })
                .build();
        return client.async(request)
                .thenApply(response -> {
                    final var json = response.output().best().message().text();
                    return JacksonJsonUtils.toObject(json, clazz);
                });
    }

    /*
     * 生成计划
     */
    private CompletionStage<Plan> processGenPlan(Chain chain, AigcRequest<Input, Output> request) {
        final var client = chain.client();
        final var model = request.model();
        final var messages = List.of(
                PLAN_GENERATOR_SYSTEM_MESSAGE,
                request.input().userInputMessage()
        );
        return completeChatAs(client, model, messages, Plan.class);
    }

    /*
     * 重新生成计划
     */
    private CompletionStage<Plan> processGenReplan(Chain chain, AigcRequest<Input, Output> request, Plan plan) {
        final var client = chain.client();
        final var model = request.model();
        final var messages = List.of(
                PLAN_REPLANNER_SYSTEM_MESSAGE,
                Message.user("""
                                ### 原诉求
                                %s
                                
                                ### 原计划
                                %s
                                """.formatted(
                                request.input().userInputMessage().text(),
                                JacksonJsonUtils.toJson(plan)
                        )
                ));
        return completeChatAs(client, model, messages, Plan.class);
    }

    private CompletionStage<Void> processTaskExecute(Chain chain, AigcRequest<Input, Output> request, Plan plan) {
        return null;
    }

}
