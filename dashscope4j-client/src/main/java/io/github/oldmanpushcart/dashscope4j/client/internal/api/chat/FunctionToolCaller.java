package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.ToolCallMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.ToolMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.function.FunctionToolNotFoundException;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.MapPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.CompletableFuture.failedStage;

class FunctionToolCaller implements Tool.Caller {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ChatOp chatOp;
    private final ChatRequest request;
    private final ToolCallMessage message;

    public FunctionToolCaller(ChatOp chatOp, ChatRequest request, ToolCallMessage message) {
        this.chatOp = chatOp;
        this.request = request;
        this.message = message;
    }

    @Override
    public String toString() {
        return "dashscope4j-client://chat/function";
    }

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

    public CompletionStage<Flow.Publisher<ChatResponse>> flowCall() {
        final Map<String, CompletableFuture<String>> futureMap = parallelCallFunction();
        return CompletableFuture.allOf(futureMap.values().toArray(new CompletableFuture[0]))
                .thenApply(unused -> {
                    final var history = newHistory(futureMap);
                    final var newRequest = newHistoryRequest(history);
                    final var publisher = chatOp.flow(newRequest);
                    return new MapPublisher<>(publisher, response -> newHistoryResponse(history, response));
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

    @Override
    public ChatRequest request() {
        return request;
    }

    // 并行调用函数
    private Map<String, CompletableFuture<String>> parallelCallFunction() {
        final Map<String, CompletableFuture<String>> futureMap = new HashMap<>();
        message.calls().stream()
                .map(FunctionTool.Call.class::cast)
                .forEach(call -> {
                    CompletionStage<String> stage;
                    try {
                        final FunctionTool tool = requireFunctionTool(call);
                        stage = callFunction(tool, call);
                    } catch (Throwable ex) {
                        stage = failedStage(ex);
                    }
                    futureMap.put(call.id(), stage.toCompletableFuture());
                });
        return futureMap;
    }

    // 函数调用
    private CompletionStage<String> callFunction(FunctionTool tool, FunctionTool.Call call) {

        if (logger.isDebugEnabled()) {
            logger.debug("{}/{} >>> {}", this, call.stub().name(), call.stub().arguments());
        }

        try {
            return tool.call(this, call.stub().arguments())
                    .whenComplete((result, ex) -> {
                        if (logger.isDebugEnabled()) {
                            logger.debug("{}/{} <<< {}", this, call.stub().name(), result, ex);
                        }
                    });
        } catch (Throwable cause) {
            throw new RuntimeException(
                    "Function call error! fn=%s;argument=%s".formatted(
                            call.stub().name(),
                            call.stub().arguments()
                    ),
                    cause
            );
        }
    }

    // 找到函数工具
    private FunctionTool requireFunctionTool(FunctionTool.Call functionCall) {
        return request.tools().stream()
                .filter(FunctionTool.class::isInstance)
                .map(FunctionTool.class::cast)
                .filter(tool -> Objects.equals(tool.meta().name(), functionCall.stub().name()))
                .findFirst()
                .orElseThrow(() -> new FunctionToolNotFoundException(functionCall.stub().name()));
    }

}
