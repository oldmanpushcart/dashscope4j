package io.github.oldmanpushcart.dashscope4j.agent;

import io.github.oldmanpushcart.dashscope4j.agent.function.dashscope.DashscopeWebSearchFunction;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import org.junit.jupiter.api.Test;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug() {

        final var agent = ReActChatAgent.newBuilder()
                .client(client)
                .addFunction(new DashscopeWebSearchFunction())
                .build();

        final var request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_TURBO)
                .addMessage(Message.ofUser("特朗普今年多少岁了?"))
                .build();

        final var response = agent.async(request)
                .toCompletableFuture()
                .join();

        System.out.println(response.output().best().message().text());

    }

}
