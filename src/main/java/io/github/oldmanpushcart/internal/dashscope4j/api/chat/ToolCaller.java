package io.github.oldmanpushcart.internal.dashscope4j.api.chat;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.OpChat;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.ToolCallMessage;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.ToolMessage;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import io.reactivex.rxjava3.core.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

import static java.util.Collections.unmodifiableList;

public class ToolCaller {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final OpChat opChat;
    private final ChatRequest request;
    private final ToolCallMessage message;

    public ToolCaller(OpChat opChat, ChatRequest request, ToolCallMessage message) {
        preCheck(message);
        this.opChat = opChat;
        this.request = request;
        this.message = message;
    }

    /*
     * 前置检查
     * 当前只支持函数工具调用
     */
    private static void preCheck(ToolCallMessage message) {

        // 检查工具调用中是否只有函数调用，当前只支持函数调用
        if (!message.calls().stream().allMatch(call -> call instanceof ChatFunctionTool.Call)) {
            throw new UnsupportedOperationException("Only support function call in tool call.");
        }

        // 检查函数调用是否有多个，当前只支持单一函数调用
        if (message.calls().size() != 1) {
            throw new UnsupportedOperationException("Only support single function call in tool call.");
        }

    }

    public CompletionStage<ChatResponse> asyncCall() {
        final ChatFunctionTool.Call call = parseFunctionCall();
        final ChatFunctionTool tool = switchFunctionTool(call);
        return callFunction(tool, call)
                .thenCompose(resultJson -> {
                    final List<Message> history = newHistory(call, resultJson);
                    final ChatRequest newRequest = newHistoryRequest(history);
                    return opChat.async(newRequest)
                            .thenApply(response -> newHistoryResponse(history, response));
                });
    }

    public CompletionStage<Flowable<ChatResponse>> flowCall() {
        return null;
    }

    private List<Message> newHistory(ChatFunctionTool.Call call, String resultJson) {
        final List<Message> history = new ArrayList<>();
        history.add(message);
        history.add(new ToolMessage(resultJson, call.stub().name()));
        return history;
    }

    // 构建新的对话请求消息，并记住本次函数调用历史
    private ChatRequest newHistoryRequest(List<Message> history) {
        return ChatRequest.newBuilder(request)
                .addMessages(history)
                .build();
    }

    // 找到返回的正常结束的选择，将历史消息添加到选择的历史消息中
    private ChatResponse newHistoryResponse(List<Message> history, ChatResponse response) {
        final List<ChatResponse.Choice> choices = new ArrayList<>();
        response.output().choices().forEach(choice -> {
            if (choice.finish() == ChatResponse.Finish.NORMAL) {
                final List<Message> historyInChoice = new ArrayList<>(choice.history());
                historyInChoice.addAll(0, history);
                choices.add(new ChatResponse.Choice(
                        choice.finish(),
                        unmodifiableList(historyInChoice)
                ));
            } else {
                choices.add(choice);
            }
        });
        final ChatResponse.Output output = new ChatResponse.Output(unmodifiableList(choices));
        return new ChatResponse(
                response.uuid(),
                response.ret(),
                response.usage(),
                output
        );
    }

    // 函数调用
    private CompletionStage<String> callFunction(ChatFunctionTool tool, ChatFunctionTool.Call call) {
        final Type parameterType = tool.meta().parameterTs().type();
        final String parameterJson = call.stub().arguments();
        try {
            return tool.function().call(JacksonUtils.toObject(parameterJson, parameterType))
                    .thenApply(JacksonUtils::toJson);
        } catch (Throwable cause) {
            throw new RuntimeException(
                    String.format("Function call error! fn=%s;parameters=%s;parameter-type=%s",
                            call.stub().name(),
                            parameterJson,
                            parameterType.getTypeName()
                    ),
                    cause
            );
        }
    }

    // 解析出函数调用存根
    private ChatFunctionTool.Call parseFunctionCall() {
        return (ChatFunctionTool.Call) message.calls().get(0);
    }

    // 找到函数工具
    private ChatFunctionTool switchFunctionTool(ChatFunctionTool.Call functionCall) {
        return request.tools().stream()
                .filter(ChatFunctionTool.class::isInstance)
                .map(ChatFunctionTool.class::cast)
                .filter(tool -> Objects.equals(tool.meta().name(), functionCall.stub().name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("Function Not found! fn=%s",
                        functionCall.stub().name()
                )));
    }

}
