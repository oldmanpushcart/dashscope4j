package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.api.chat.function.EchoFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import io.github.oldmanpushcart.dashscope4j.api.chat.tool.function.ChatFunctionTool;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug$text() {

        final List<ChatFunctionTool> tools = Arrays.asList(
                ChatFunctionTool.of(new EchoFunction())
        );

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_MAX)
                .addMessage(Message.ofUser("今天星期几?"))
                .addTools(tools)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, false)
                .option(ChatOptions.ENABLE_WEB_SEARCH, true)
                .build();

        client.chat().flow(request)
                .thenAccept(flow-> {
                    flow.blockingSubscribe(r-> {
                        System.out.println(r.output().best().message().text());
                    });
                })
                .toCompletableFuture()
                .join();

    }

    @Test
    public void test$debug$vl() {

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_VL_MAX)
                .addMessage(Message.ofUser(""))
                .build();

        final ChatResponse response = client.chat().async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response);

    }

}
