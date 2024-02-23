package io.github.ompc.dashscope4j.test.chat;

import io.github.ompc.dashscope4j.chat.ChatResponse;
import org.junit.jupiter.api.Assertions;

public class ChatAssertions {

    public static void assertChatResponse(ChatResponse response) {

        // uuid
        Assertions.assertNotNull(response.uuid());
        Assertions.assertFalse(response.uuid().isBlank());

        // usage
        Assertions.assertNotNull(response.usage());
        Assertions.assertTrue(response.usage().total() > 0);

        // choices
        Assertions.assertNotNull(response.output().choices());
        Assertions.assertFalse(response.output().choices().isEmpty());

    }

}
