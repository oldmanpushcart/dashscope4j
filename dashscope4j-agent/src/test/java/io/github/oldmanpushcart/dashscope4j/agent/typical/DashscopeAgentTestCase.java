package io.github.oldmanpushcart.dashscope4j.agent.typical;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.agent.function.SystemDateTimeFunction;
import io.github.oldmanpushcart.dashscope4j.agent.memory.TreeSetMemory;
import io.github.oldmanpushcart.dashscope4j.agent.typical.dashscope.DashscopeChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatOptions;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import org.junit.jupiter.api.Test;

public class DashscopeAgentTestCase extends ClientSupport {

    @Test
    public void test$async() {

        final ChatAgent agent = DashscopeChatAgent.newBuilder()
                .client(client)
                .build();

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .enableAutoUpload(true)
                .addMessage(Message.ofUser("杭州5月5日天气?"))
                .build();

        final ChatResponse response = agent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

    }

    @Test
    public void test$flow() {

        final ChatAgent agent = DashscopeChatAgent.newBuilder()
                .client(client)
                .memory(TreeSetMemory.newBuilder()
                        .build())
                .addFunction(new SystemDateTimeFunction())
                .build();

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.ofText("qwen3-235b-a22b"))
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .addMessage(Message.ofUser("根据杭州5月5日天气画一副中国山水画"))
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
