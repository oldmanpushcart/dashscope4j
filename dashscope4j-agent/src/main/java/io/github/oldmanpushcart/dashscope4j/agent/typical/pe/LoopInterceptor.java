package io.github.oldmanpushcart.dashscope4j.agent.typical.pe;

import io.github.oldmanpushcart.dashscope4j.agent.util.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.interceptor.ChatInterceptor;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

public class LoopInterceptor implements ChatInterceptor {

    private static final Message PLAN_GENERATOR_MESSAGE = Message.system(PromptTemplate.newBuilder()
            .resource("/prompt/PLAN_GENERATOR.md")
            .build()
            .render());

    private static final Message PLAN_REPLANNER_MESSAGE = Message.system(PromptTemplate.newBuilder()
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
    private <T> CompletionStage<T> completeChatAs(DashscopeClient client, ChatModel model, List<Message> messages, Class<T> clazz) {
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
    private CompletionStage<Plan> processGeneratePlan(Chain chain, AigcRequest<Input, Output> request) {
        final var userInputMessage = request.input().userInputMessage();
        final var genPlanRequest = AigcRequest.newBuilder(request)
                .input(input-> Input.newBuilder(input)
                        .messages(messages-> {
                            messages.add();
                            return messages;
                        })
                        .build())
                .parameters(parameters -> {
                    parameters.put("response_format", Map.of("type", "json_object"));
                    return parameters;
                })
                .build();
        return chain.proceed(genPlanRequest)
                .thenApply(r -> {
                    //noinspection unchecked
                    return (AigcResponse<Output>) r;
                })
                .thenApply(response -> {
                    final var json = response.output().best().message().text();
                    return JacksonJsonUtils.toObject(json, Plan.class);
                });
    }

}
