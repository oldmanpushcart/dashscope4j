package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.ToolCallMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.ToolMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunction;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionToolNotFoundException;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.JacksonJsonUtils;
import io.reactivex.rxjava3.core.Flowable;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.CompletableFuture.failedStage;

@Slf4j
class ToolCaller implements ChatFunction.Caller {

    private final DashscopeClient client;
    private final ChatOp chatOp;
    private final ChatRequest request;
    private final ToolCallMessage message;

    public ToolCaller(DashscopeClient client, ChatOp chatOp, ChatRequest request, ToolCallMessage message) {
        preCheck(message);
        this.client = client;
        this.chatOp = chatOp;
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

    }

    /**
     * 异步调用函数工具
     *
     * @return 异步调用函数应答
     */
    public CompletionStage<ChatResponse> asyncCall() {

        final Map<String, CompletableFuture<String>> futureMap = parallelCallFunction();
        return CompletableFuture.allOf(futureMap.values().toArray(new CompletableFuture[0]))
                .thenCompose(unused -> {
                    final List<Message> history = newHistory(futureMap);
                    final ChatRequest newRequest = newHistoryRequest(history);
                    return chatOp.async(newRequest)
                            .thenApply(response -> newHistoryResponse(history, response));
                });
    }

    /**
     * 流式调用函数工具
     *
     * @return 流式调用函数应答
     */
    public CompletionStage<Flowable<ChatResponse>> flowCall() {

        final Map<String, CompletableFuture<String>> futureMap = parallelCallFunction();
        return CompletableFuture.allOf(futureMap.values().toArray(new CompletableFuture[0]))
                .thenCompose(unused -> {
                    final List<Message> history = newHistory(futureMap);
                    final ChatRequest newRequest = newHistoryRequest(history);
                    return chatOp.flow(newRequest)
                            .thenApply(flow -> flow.map(r -> newHistoryResponse(history, r)));
                });
    }

    /*
     * 多个工具调用结果合并为历史消息
     * [
     *    message,
     *    result_message1,
     *    result_message2,
     *    ...
     *    result_messageN
     * ]
     */
    private List<Message> newHistory(Map<String, CompletableFuture<String>> futureMap) {
        final List<Message> history = new ArrayList<>();
        history.add(message);
        futureMap.entrySet()
                .stream()
                .map(entry -> {
                    final String id = entry.getKey();
                    final String resultJson = entry.getValue().join();
                    return new ToolMessage(id, resultJson);
                })
                .forEach(history::add);
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

        return response.changeChoice(choice -> {

            if (choice.finish() != ChatResponse.Finish.NORMAL) {
                return choice;
            }

            return choice.changeMessages(messages -> {
                final List<Message> newMessages = new ArrayList<>();
                newMessages.addAll(history);
                newMessages.addAll(messages);
                return unmodifiableList(newMessages);
            });

        });

    }

    // 并行调用函数
    private Map<String, CompletableFuture<String>> parallelCallFunction() {
        final Map<String, CompletableFuture<String>> futureMap = new HashMap<>();
        message.calls().stream()
                .map(ChatFunctionTool.Call.class::cast)
                .forEach(call -> {
                    CompletionStage<String> future;
                    try {
                        final ChatFunctionTool tool = switchFunctionTool(call);
                        future = callFunction(tool, call);
                    } catch (Throwable ex) {
                        future = failedStage(ex);
                    }
                    futureMap.put(
                            call.id(),
                            future.toCompletableFuture()
                    );
                });
        return futureMap;
    }

    // 函数调用
    private CompletionStage<String> callFunction(ChatFunctionTool tool, ChatFunctionTool.Call call) {

        final Type parameterType = tool.meta().parameterTs().type();
        final String argumentsJson = call.stub().arguments();

        if (log.isDebugEnabled()) {
            log.debug("dashscope-client://chat/function/{} <<< {}",
                    call.stub().name(),
                    JacksonJsonUtils.compact(argumentsJson)
            );
        }

        try {
            return tool.call(this, argumentsJson)
                    .whenComplete((resultJson, ex) -> {
                        if (log.isDebugEnabled()) {
                            log.debug("dashscope-client://chat/function/{} >>> {}",
                                    call.stub().name(),
                                    JacksonJsonUtils.compact(resultJson),
                                    ex
                            );
                        }
                    });
        } catch (Throwable cause) {
            throw new RuntimeException(
                    String.format("Function call error! fn=%s;arguments[type=%s]=%s",
                            call.stub().name(),
                            parameterType.getTypeName(),
                            argumentsJson
                    ),
                    cause
            );
        }
    }

    /*
     * 转换为参数
     * 这里需要处理传递的参数直接为null的情况，null -> null
     * 不要拿null到jackson进行转换
     */
    private <T> T toArgument(String parameterJson, Type parameterType) {
        if (null == parameterJson || parameterJson.trim().isEmpty()) {
            return null;
        }
        return JacksonJsonUtils.toObject(parameterJson, parameterType);
    }

    // 找到函数工具
    private ChatFunctionTool switchFunctionTool(ChatFunctionTool.Call functionCall) {
        return request.tools().stream()
                .filter(ChatFunctionTool.class::isInstance)
                .map(ChatFunctionTool.class::cast)
                .filter(tool -> Objects.equals(tool.meta().name(), functionCall.stub().name()))
                .findFirst()
                .orElseThrow(() -> new FunctionToolNotFoundException(functionCall.stub().name()));
    }

    @Override
    public DashscopeClient client() {
        return client;
    }

    @Override
    public ChatRequest request() {
        return request;
    }

}
