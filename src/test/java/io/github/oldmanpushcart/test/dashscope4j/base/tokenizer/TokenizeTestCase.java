package io.github.oldmanpushcart.test.dashscope4j.base.tokenizer;

import io.github.oldmanpushcart.dashscope4j.chat.ChatModel;
import io.github.oldmanpushcart.dashscope4j.chat.message.Message;
import io.github.oldmanpushcart.test.dashscope4j.LoadingEnv;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class TokenizeTestCase implements LoadingEnv {

    @Test
    public void test$tokenize$remote$messages() {

        final var messages = List.of(
                Message.ofUser("北京有哪些好玩地方？"),
                Message.ofAi("故宫、颐和园、天坛等都是可以去游玩的景点哦。"),
                Message.ofUser("帮我安排一些行程")
        );

        final var list = client.base().tokenize().remote(ChatModel.QWEN_PLUS).encode(messages).join();
        Assertions.assertEquals(26, list.size());
        list.forEach(v -> {
            Assertions.assertNotNull(v.getKey());
            Assertions.assertNotNull(v.getValue());
            System.out.printf("%s | %s%n", v.getKey(), v.getValue());
        });

    }

    @Test
    public void test$tokenize$remote$text() {

        final var text = """
                北京有哪些好玩地方？
                故宫、颐和园、天坛等都是可以去游玩的景点哦。
                帮我安排一些行程
                """;

        final var list = client.base().tokenize().remote(ChatModel.QWEN_PLUS).encode(text).join();
        Assertions.assertEquals(26, list.size());
        list.forEach(v -> {
            Assertions.assertNotNull(v.getKey());
            Assertions.assertNotNull(v.getValue());
            System.out.printf("%s | %s%n", v.getKey(), v.getValue());
        });

    }

    @Test
    public void test$tokenize$local$messages() {

        final var messages = List.of(
                Message.ofUser("北京有哪些好玩地方？"),
                Message.ofAi("故宫、颐和园、天坛等都是可以去游玩的景点哦。"),
                Message.ofUser("帮我安排一些行程")
        );

        final var list = client.base().tokenize().local().encode(messages).join();
        Assertions.assertEquals(26, list.size());
        list.forEach(v -> {
            Assertions.assertNotNull(v.getKey());
            Assertions.assertNotNull(v.getValue());
            System.out.printf("%s | %s%n", v.getKey(), v.getValue());
        });

    }

    @Test
    public void test$tokenize$local$text() {

        final var text = """
                北京有哪些好玩地方？
                故宫、颐和园、天坛等都是可以去游玩的景点哦。
                帮我安排一些行程
                """;

        final var list = client.base().tokenize().local().encode(text).join();
        Assertions.assertEquals(26, list.size());
        list.forEach(v -> {
            Assertions.assertNotNull(v.getKey());
            Assertions.assertNotNull(v.getValue());
            System.out.printf("%s | %s%n", v.getKey(), v.getValue());
        });

    }

}
