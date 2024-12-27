package io.github.oldmanpushcart.dashscope4j.base.tokenizer;

import io.github.oldmanpushcart.dashscope4j.ClientSupport;
import io.github.oldmanpushcart.dashscope4j.api.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TokenizeTestCase extends ClientSupport {

    @Test
    public void test$tokenize$remote$messages() {

        final List<Message> messages = Arrays.asList(
                Message.ofUser("北京有哪些好玩地方？"),
                Message.ofAi("故宫、颐和园、天坛等都是可以去游玩的景点哦。"),
                Message.ofUser("帮我安排一些行程")
        );

        final List<Map.Entry<Integer, String>> list = client.base().tokenize().remote(ChatModel.QWEN_PLUS).encode(messages)
                .toCompletableFuture()
                .join();
        Assertions.assertEquals(26, list.size());
        list.forEach(v -> {
            Assertions.assertNotNull(v.getKey());
            Assertions.assertNotNull(v.getValue());
            System.out.printf("%s | %s%n", v.getKey(), v.getValue());
        });

    }

    @Test
    public void test$tokenize$remote$text() {

        final String text =
                "北京有哪些好玩地方？\n" +
                "故宫、颐和园、天坛等都是可以去游玩的景点哦。\n" +
                "帮我安排一些行程\n";

        final List<Map.Entry<Integer, String>> list = client.base().tokenize().remote(ChatModel.QWEN_PLUS).encode(text)
                .toCompletableFuture()
                .join();
        Assertions.assertEquals(26, list.size());
        list.forEach(v -> {
            Assertions.assertNotNull(v.getKey());
            Assertions.assertNotNull(v.getValue());
            System.out.printf("%s | %s%n", v.getKey(), v.getValue());
        });

    }

    @Test
    public void test$tokenize$local$messages() {

        final List<Message> messages = Arrays.asList(
                Message.ofUser("北京有哪些好玩地方？"),
                Message.ofAi("故宫、颐和园、天坛等都是可以去游玩的景点哦。"),
                Message.ofUser("帮我安排一些行程")
        );

        final List<Map.Entry<Integer, String>> list = client.base().tokenize().local().encode(messages)
                .toCompletableFuture()
                .join();
        Assertions.assertEquals(26, list.size());
        list.forEach(v -> {
            Assertions.assertNotNull(v.getKey());
            Assertions.assertNotNull(v.getValue());
            System.out.printf("%s | %s%n", v.getKey(), v.getValue());
        });

    }

    @Test
    public void test$tokenize$local$text() {

        final String text =
                "北京有哪些好玩地方？\n" +
                "故宫、颐和园、天坛等都是可以去游玩的景点哦。\n" +
                "帮我安排一些行程\n";

        final List<Map.Entry<Integer, String>> list = client.base().tokenize().local().encode(text)
                .toCompletableFuture()
                .join();
        Assertions.assertEquals(26, list.size());
        list.forEach(v -> {
            Assertions.assertNotNull(v.getKey());
            Assertions.assertNotNull(v.getValue());
            System.out.printf("%s | %s%n", v.getKey(), v.getValue());
        });

    }

}
