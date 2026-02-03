package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor.tool;


import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatParameterKeys;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.ToolMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
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
    private final DashscopeClient client;
    private final AigcRequest<Input, Output> request;
    private final AssistantMessage message;

    public FunctionToolCaller(DashscopeClient client, AigcRequest<Input, Output> request, AssistantMessage message) {
        this.client = client;
        this.request = request;
        this.message = message;
    }

    @Override
    public String toString() {
        return "dashscope4j-client://chat/function";
    }

    public CompletionStage<AigcResponse<Output>> asyncCall() {
        final var futureMap = parallelCallFunction();
        return CompletableFuture.allOf(futureMap.values().toArray(new CompletableFuture[0]))
                .thenCompose(unused -> {
                    final var history = newHistory(futureMap);
                    final var newRequest = newHistoryRequest(history);
                    return client.async(newRequest);
                });
    }

    public Flow.Publisher<AigcResponse<Output>> flowCall() {
        return FlowX.defer(() -> {
            final var futureMap = parallelCallFunction();
            return FlowX.fromCompletionStage(CompletableFuture
                    .allOf(futureMap.values().toArray(new CompletableFuture[0]))
                    .thenApply(unused -> {
                        final var history = newHistory(futureMap);
                        final var newRequest = newHistoryRequest(history);
                        return client.flow(newRequest);
                    }));
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
    private AigcRequest<Input, Output> newHistoryRequest(List<Message> history) {
        return AigcRequest.newBuilder(request)
                .input(Input.newBuilder(request.input())
                        .addMessages(history)
                        .build())
                .build();
    }


    @Override
    public AigcRequest<?, ?> request() {
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
                        final var tool = requireTool(call);
                        stage = callFunction(tool, call);
                    } catch (Throwable ex) {
                        stage = failedStage(ex);
                    }
                    futureMap.put(call.id(), stage.toCompletableFuture());
                });
        return futureMap;
    }

    // 函数调用
    private CompletionStage<String> callFunction(Tool tool, FunctionTool.Call call) {

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
    private Tool requireTool(FunctionTool.Call functionCall) {
        final var tools = request.parameters().get(ChatParameterKeys.TOOLS);
        if (null == tools) {
            return null;
        }
        return Arrays.stream(tools)
                .filter(FunctionTool.class::isInstance)
                .map(FunctionTool.class::cast)
                .filter(tool -> Objects.equals(tool.meta().name(), functionCall.stub().name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Function tool not found: " + functionCall.stub().name()));
    }

}
