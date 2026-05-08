package io.github.oldmanpushcart.dashscope4j.client.aigc.chat.internal.interceptor.tool;


import io.github.oldmanpushcart.dashscope4j.client.DashscopeClient;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Input;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.ChatModel.Output;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.AssistantMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.message.ToolMessage;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.FunctionTool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.Tool;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolExecutionException;
import io.github.oldmanpushcart.dashscope4j.client.aigc.chat.tool.ToolResult;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.AigcResponse;
import io.github.oldmanpushcart.dashscope4j.client.util.CompletableFutureUtils;
import io.github.oldmanpushcart.dashscope4j.client.util.jackson.JacksonJsonUtils;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

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

    public Publisher<AigcResponse<Output>> flowCall() {
        return Flux.defer(() -> {
            final var futureMap = parallelCallFunction();
            return Mono.fromCompletionStage(CompletableFuture.allOf(futureMap.values().toArray(new CompletableFuture[0]))
                            .thenApply(unused -> {
                                final var history = newHistory(futureMap);
                                final var newRequest = newHistoryRequest(history);
                                return client.flow(newRequest);
                            }))
                    .flatMapMany(Flux::from);
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
                .input(input -> Input.newBuilder(input)
                        .addMessages(history)
                        .build())
                .build();
    }


    @Override
    public AigcRequest<?, ?> request() {
        return request;
    }

    @Override
    public DashscopeClient client() {
        return client;
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

        return CompletableFuture.completedStage(null)

                // 执行函数
                .thenCompose(unused -> tool.call(this, call.stub().arguments()))

                // 日志记录调用结果
                .whenComplete((result, ex) -> {

                    if (ex == null) {
                        logger.debug("{}/{} <<< {}", this, call.stub().name(), result);
                    } else {
                        logger.debug("{}/{} <<< ERROR", this, call.stub().name(), ex);
                    }

                })

                /*
                 * 对函数调用结果进行处理
                 * 将失败的信息封装为 ToolResult，并返回给LLM提供其下一步的决策。
                 */
                .handle((r, ex) -> {

                    if (null == ex) {
                        return CompletableFuture.completedStage(r);
                    }

                    if (!request.input().failOnToolError()) {
                        final var cause = CompletableFutureUtils.unwrapEx(ex);
                        final var result = ToolResult.ofError(call.stub().name(), cause);
                        final var errorJson = JacksonJsonUtils.toJson(result);
                        return CompletableFuture.completedStage(errorJson);
                    } else {
                        return CompletableFuture.<String>failedStage(ex);
                    }

                })
                .thenCompose(v -> v);
    }

    // 找到函数工具
    private Tool requireTool(FunctionTool.Call functionCall) {

        //noinspection unchecked
        final var tools = (List<Tool>) (request.parameters().get("tools"));
        if (null == tools) {
            return null;
        }

        // 找到指定的工具
        for (final var tool : tools) {

            if (!(tool instanceof FunctionTool functionTool)) {
                continue;
            }

            if (Objects.equals(functionTool.meta().name(), functionCall.stub().name())) {
                return tool;
            }

        }

        throw ToolExecutionException.notFound(functionCall.stub().name());

    }

}
