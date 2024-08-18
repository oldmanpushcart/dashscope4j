package io.github.oldmanpushcart.internal.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.OpAsyncOpFlow;
import io.github.oldmanpushcart.dashscope4j.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.chat.message.ToolCallMessage;
import io.github.oldmanpushcart.dashscope4j.chat.tool.function.ChatFunctionTool;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.ToolMessageImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import io.github.oldmanpushcart.internal.dashscope4j.util.MapFlowProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;
import static io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils.compact;

/**
 * 工具调用操作
 */
class OpToolCall {

    private final static Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private final ChatRequest request;
    private final ToolCallMessage message;

    public OpToolCall(ChatRequest request, ToolCallMessage message) {
        this.request = request;
        this.message = message;
    }

    /**
     * 操作工具调用
     *
     * @param client DashScope客户端
     * @return 异步操作
     */
    public CompletionStage<OpAsyncOpFlow<ChatResponse>> op(DashScopeClient client) {

        // 检查工具调用中是否只有函数调用，当前只支持函数调用
        if (!message.calls().stream().allMatch(call -> call instanceof ChatFunctionTool.Call)) {
            throw new UnsupportedOperationException("only support function call in tool call.");
        }

        // 检查函数调用是否有多个，当前只支持单一函数调用
        if (message.calls().size() != 1) {
            throw new UnsupportedOperationException("only support single function call in tool call.");
        }

        // 获取函数调用
        final var functionCall = (ChatFunctionTool.Call) message.calls().get(0);

        // 找到函数工具
        final var functionTool = request.tools().stream()
                .filter(ChatFunctionTool.class::isInstance)
                .map(ChatFunctionTool.class::cast)
                .filter(tool -> Objects.equals(tool.meta().name(), functionCall.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("not found tool by name: %s".formatted(functionCall.name())));

        if (logger.isDebugEnabled()) {
            logger.debug("{}/function/{} <= {}",
                    request.protocol(),
                    functionCall.name(),
                    compact(functionCall.arguments())
            );
        }

        // 进行函数调用
        return callingFunction(functionTool, functionCall)
                .thenApply(resultJson -> {

                    if (logger.isDebugEnabled()) {
                        logger.debug("{}/function/{} => {}",
                                request.protocol(),
                                functionCall.name(),
                                compact(resultJson)
                        );
                    }

                    // 工具调用的对话历史，需要在最后的response中透出
                    final var history = new ArrayList<Message>();
                    history.add(message);
                    history.add(new ToolMessageImpl(resultJson, functionCall.name()));

                    // 工具调用结果的请求
                    final var newRequest = ChatRequest.newBuilder(request)
                            .appendMessages(history)
                            .build();

                    /*
                     * 处理工具调用结果的请求
                     * 1. 这里需要代理处理，因为工具调用结果的请求中的消息和工具调用的对话历史需要在最后的response中透出
                     * 2. 通过调用client的请求来实现工具级联调用
                     */
                    return opProxy(history, client.chat(newRequest));
                });
    }

    // 函数调用
    private CompletionStage<String> callingFunction(ChatFunctionTool tool, ChatFunctionTool.Call call) {
        try {
            return tool.function().call(JacksonUtils.toObject(call.arguments(), tool.meta().parameterTs().type()))
                    .thenApply(JacksonUtils::toJson);
        } catch (Throwable cause) {
            throw new RuntimeException("function: %s call error!".formatted(call.name()), cause);
        }
    }

    // 代理操作
    private OpAsyncOpFlow<ChatResponse> opProxy(List<Message> history, OpAsyncOpFlow<ChatResponse> op) {
        return new OpAsyncOpFlow<>() {

            // 处理response
            private ChatResponse onResponse(ChatResponse response) {
                response.output().choices().stream()
                        .filter(choice -> choice.finish() == ChatResponse.Finish.NORMAL)
                        .forEach(choice -> {

                            // 添加工具调用的对话历史
                            if (choice instanceof ChoiceImpl impl) {
                                impl.appendFirst(history);
                            }

                        });
                return response;
            }

            @Override
            public CompletionStage<ChatResponse> async() {
                return op.async().thenApply(this::onResponse);
            }

            @Override
            public CompletionStage<Flow.Publisher<ChatResponse>> flow() {
                return op.flow().thenApply(source -> MapFlowProcessor.syncOneToOne(source, this::onResponse));
            }

        };
    }

}
