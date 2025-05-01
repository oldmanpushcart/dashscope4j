package io.github.oldmanpushcart.dashscope4j.agent.util;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionToolNotFoundException;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CompletionStage;

public abstract class ChatFunctionToolHelper {

    public static CompletionStage<String> callingFunctionTool(ChatFunction.Caller caller, ChatFunctionTool functionTool, String argumentJson) {
        final Type parameterType = functionTool.meta().parameterTs().type();
        return functionTool.function()
                .call(caller, JacksonUtils.toObject(argumentJson, parameterType))
                .thenApply(JacksonUtils::toJson);
    }

    public static ChatFunctionTool requireFunctionTool(List<ChatFunctionTool> functionTools, String functionName) {
        return functionTools.stream()
                .filter(v -> v.meta().name().equals(functionName))
                .findFirst()
                .orElseThrow(() -> new FunctionToolNotFoundException(functionName));
    }

    public static ChatFunction.Caller newFunctionCaller(DashscopeClient dashscope, ChatRequest request) {
        return new ChatFunction.Caller() {

            @Override
            public DashscopeClient client() {
                return dashscope;
            }

            @Override
            public ChatRequest request() {
                return request;
            }

        };
    }

}
