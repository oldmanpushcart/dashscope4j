package io.github.oldmanpushcart.dashscope4j.agent.plugin.memory;

import io.github.oldmanpushcart.dashscope4j.agent.ChatAgent;
import io.github.oldmanpushcart.dashscope4j.agent.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.agent.DashscopeAssertions;
import io.github.oldmanpushcart.dashscope4j.agent.prompt.PromptTemplate;
import io.github.oldmanpushcart.dashscope4j.agent.typical.react.ReActChatAgent;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatRequest;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.ChatResponse;
import io.github.oldmanpushcart.dashscope4j.client.api.chat.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

public class MemoryPluginTestCase extends ClientSupport {

    @Test
    public void test$memory$append() {

        final String conversationId = UUID.randomUUID().toString();
        final Memory memory = new TreeSetMemory();

        final ChatAgent agent = ReActChatAgent.newBuilder()
                .client(client)
                .addPlugin(MemoryPlugin.newBuilder()
                        .memory(memory)
                        .build())
                .build();

        final int total = 3;
        for (int index = 1; index <= total; index++) {
            final String text = PromptTemplate.newBuilder()
                    .template("${a}+${b}=?")
                    .variable("a", index)
                    .variable("b", index)
                    .build()
                    .render();
            final ChatRequest request = ChatRequest.newBuilder()
                    .model(ChatModel.QWEN_TURBO)
                    .addMessage(Message.ofUser(text))
                    .context(Memory.Context.class, new Memory.Context()
                            .conversationId(conversationId))
                    .build();

            final ChatResponse response = agent.async(request)
                    .toCompletableFuture()
                    .join();

            final int expect = index + index;
            DashscopeAssertions.assertByDashscope(client, "答案是" + expect, response.output().best().message().text());

        }

        final List<Memory.Fragment> fragments = memory.recall(conversationId, new Memory.Condition());
        Assertions.assertEquals(total, fragments.size());


        int index = total;
        for (final Memory.Fragment fragment : fragments) {
            final String text = index + "+" + index + "=?";
            final int expect = index + index;
            Assertions.assertEquals(conversationId, fragment.conversationId());
            Assertions.assertNotNull(fragment.requestMessage());
            Assertions.assertEquals(text, fragment.requestMessage().text());
            Assertions.assertNotNull(fragment.responseMessage());
            DashscopeAssertions.assertByDashscope(client, "答案是" + expect, fragment.responseMessage().text());
            index --;
        }

    }

}
