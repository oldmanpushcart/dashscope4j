package io.github.oldmanpushcart.internal.dashscope4j.chat;

import io.github.oldmanpushcart.dashscope4j.DashScopeClient;
import io.github.oldmanpushcart.dashscope4j.DashScopeClient.OpAsyncOpFlow;
import io.github.oldmanpushcart.dashscope4j.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.util.TransformFlowProcessor;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.ToolCallMessageImpl;
import io.github.oldmanpushcart.internal.dashscope4j.chat.message.ToolMessageImpl;
import io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;

import static io.github.oldmanpushcart.dashscope4j.Constants.LOGGER_NAME;
import static io.github.oldmanpushcart.internal.dashscope4j.util.JacksonUtils.compact;

class OpToolCall {

    private final static Logger logger = LoggerFactory.getLogger(LOGGER_NAME);

    private final ChatRequestImpl request;
    private final ToolCallMessageImpl message;

    public OpToolCall(ChatRequestImpl request, ToolCallMessageImpl message) {
        this.request = request;
        this.message = message;
    }

    public CompletableFuture<OpAsyncOpFlow<ChatResponse>> op(DashScopeClient client) {

        // 检查工具调用中是否只有函数调用，当前只支持函数调用
        if (!message.calls().stream().allMatch(call -> call instanceof FunctionTool.Call)) {
            throw new IllegalArgumentException("only support function call in tool call.");
        }

        // 检查函数调用是否有多个，当前只支持单一函数调用
        if (message.calls().size() != 1) {
            throw new IllegalArgumentException("only support single function call in tool call.");
        }

        // 获取函数调用
        final var functionCall = (FunctionTool.Call) message.calls().get(0);

        // 找到函数工具
        final var functionTool = request.functionTools().stream()
                .filter(tool -> Objects.equals(tool.meta().name(), functionCall.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("not found tool by name: %s".formatted(functionCall.name())));

        if (logger.isDebugEnabled()) {
            logger.debug("{}/function <= {}", request, compact(functionCall.arguments()));
        }

        // 进行函数调用
        return callingFunction(functionTool, functionCall)
                .thenApply(resultJson -> {

                    if (logger.isDebugEnabled()) {
                        logger.debug("{}/function => {}", request, compact(resultJson));
                    }

                    final var history = new ArrayList<Message>();
                    history.add(message);
                    history.add(new ToolMessageImpl(resultJson, functionCall.name()));

                    final var messages = new ArrayList<Message>();
                    messages.addAll(request.messages());
                    messages.addAll(history);

                    final var newRequest = new ChatRequestImpl(
                            request.model(),
                            request.option(),
                            request.timeout(),
                            messages,
                            request.plugins(),
                            request.functionTools()
                    );

                    return opProxy(history, client.chat(newRequest));

                });
    }

    private CompletableFuture<String> callingFunction(FunctionTool tool, FunctionTool.Call call) {
        try {
            return tool.function().call(JacksonUtils.toObject(call.arguments(), tool.meta().parameterTs().type()))
                    .thenApply(JacksonUtils::toJson);
        } catch (Throwable cause) {
            throw new RuntimeException("function: %s call error!".formatted(call.name()), cause);
        }
    }

    private OpAsyncOpFlow<ChatResponse> opProxy(List<Message> history, OpAsyncOpFlow<ChatResponse> op) {
        return new OpAsyncOpFlow<>() {

            // 代理response
            private ChatResponse responseProxy(ChatResponse response) {
                response.output().choices().stream()
                        .filter(choice -> choice.finish() == ChatResponse.Finish.NORMAL)
                        .forEach(choice -> {
                            if (choice instanceof ChoiceImpl impl) {
                                impl.appendFirst(history);
                            }
                        });
                return response;
            }

            @Override
            public CompletableFuture<ChatResponse> async() {
                return op.async()
                        .thenApply(this::responseProxy);
            }

            @Override
            public CompletableFuture<Flow.Publisher<ChatResponse>> flow() {
                return op.flow()
                        .thenApply(source-> TransformFlowProcessor.transform(source, response -> List.of(responseProxy(response))));
            }

        };
    }

}
