package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.api.chat.function.QueryScoreFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import org.junit.jupiter.api.Test;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug$text() {

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWQ_PLUS)
                .addMessage(Message.ofUser("你好"))
                .build();

        client.chat().directFlow(request)
                .blockingSubscribe(response -> {
                    System.out.println(response.output().best().message().reasoningContent());
                });

    }

}
