package io.github.oldmanpushcart.dashscope4j.client.internal.api.chat;

import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOp;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.ToolMessage;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.internal.util.flow.FlowX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import static java.util.concurrent.CompletableFuture.failedStage;

class FunctionToolCaller implements Tool.Caller {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ChatOp chatOp;
    private final ChatRequest request;
    private final AssistantMessage message;

    public FunctionToolCaller(ChatOp chatOp, ChatRequest request, AssistantMessage message) {
        this.chatOp = chatOp;
        this.request = request;
        this.message = message;
    }

    @Override
    public String toString() {
        return "dashscope4j-client://chat/function";
    }

    public CompletionStage<ChatResponse> asyncCall() {
        final var futureMap = parallelCallFunction();
        return CompletableFuture.allOf(futureMap.values().toArray(new CompletableFuture[0]))
                .thenCompose(unused -> {
                    final List<Message> history = newHistory(futureMap);
                    final ChatRequest newRequest = newHistoryRequest(history);
                    return chatOp.async(newRequest);
                });
    }

    public Flow.Publisher<ChatResponse> flowCall() {
        return FlowX
                .defer(() -> FlowX
                        .fromCompletionStage(() -> {
                            final var futureMap = parallelCallFunction();
                            return CompletableFuture.allOf(futureMap.values().toArray(new CompletableFuture[0]))
                                    .thenApply(unused -> {
                                        final var history = newHistory(futureMap);
                                        final var newRequest = newHistoryRequest(history);
                                        return chatOp.flow(newRequest);
                                    });
                        })
                        .publisher())
                .publisher();
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


    @Override
    public ChatRequest request() {
        return request;
    }

    // 并行调用函数
    private Map<String, CompletableFuture<String>> parallelCallFunction() {
        final var futureMap = new HashMap<String, CompletableFuture<String>>();
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
                .orElseThrow(() -> new IllegalArgumentException("Function tool not found: " + functionCall.stub().name()));
    }

}
