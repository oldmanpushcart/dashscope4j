package io.github.oldmanpushcart.dashscope4j;

import io.github.oldmanpushcart.dashscope4j.api.chat.*;
import io.github.oldmanpushcart.dashscope4j.api.chat.function.QueryScoreFunction;
import io.github.oldmanpushcart.dashscope4j.api.chat.message.Message;
import org.junit.jupiter.api.Test;

public class DebugTestCase extends ClientSupport {

    @Test
    public void test$debug$text() {

        final ChatRequest request = ChatRequest.newBuilder()
                .model(ChatModel.QWEN_MAX)
                .addFunction(new QueryScoreFunction())
                .addMessage(Message.ofUser("语文和数学的分数是多少？"))
                .option(ChatOptions.ENABLE_INCREMENTAL_OUTPUT, true)
                .option(ChatOptions.ENABLE_PARALLEL_TOOL_CALLS, true)
                .build();
        client.chat().directFlow(request)
                .doOnNext(response-> System.out.println(response.output().best().message().text()))
                .blockingSubscribe();

    }

}
