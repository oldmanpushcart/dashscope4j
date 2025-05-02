package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.agent.function.SystemDateTimeFunction;
import io.github.oldmanpushcart.dashscope4j.agent.memory.TreeSetMemory;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import org.junit.jupiter.api.Test;

public class ReActAgentTestCase extends ClientSupport {

    @Test
    public void test$async() {

        final ChatAgent agent = ReActChatAgent.newBuilder()
                .client(client)
                .addFunction(new SystemDateTimeFunction())
                .build();

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser("现在几点了?"))
                .build();

        final ChatResponse response = agent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response);

    }

    @Test
    public void test$flow() {

        final ChatAgent agent = ReActChatAgent.newBuilder()
                .client(client)
                .memory(TreeSetMemory.newBuilder()
                        .build())
                .addFunction(new SystemDateTimeFunction())
                .build();

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .addMessage(Message.ofUser("现在几点了?"))
                .build();

        agent.directFlow(request)
                .reduce(new StringBuilder(), (stringBuf, response) -> {
                    stringBuf.append(response.output().best().message().text());
                    return stringBuf;
                })
                .toCompletionStage()
                .thenAccept(System.out::println)
                .toCompletableFuture()
                .join();

    }

}
